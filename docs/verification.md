# 初始化验证记录

日期：2026-10-22（Asia/Shanghai）。环境：Apple Silicon macOS，项目本地 Temurin JDK 17.0.20.1；系统 JDK 21.0.10 用于 1.21.1。系统默认 JDK 26 未修改。

此文件记录初始化时实测结果；后续修改不自动延续这些结论。

| 检查 | 状态 |
| --- | --- |
| 共享核心用 `javac --release 17` 编译 | 通过 |
| 1.20.1 Forge `build` + JAR 校验 | 通过 |
| 1.21.1 NeoForge `build` + JAR 校验 | 通过 |
| 1.19.1 Forge `build` + JAR 校验 | 通过 |
| 三目标独立开发服务端启动/加载/正常关闭 | 全部通过：均观察到 mod 加载、Done 就绪、存档及 stop 后退出码 0 |
| 主版本发行 JAR 安装到普通 Forge 服务端 | 通过：加载 PinkkiCore、Done 就绪、stop 正常保存退出 |
| 主版本开发客户端 | 日志确认 LWJGL 初始化及 mod 加载；用户已人工确认窗口打开且 Mods 中存在 PinkkiCore 0.1.0 |
| GitHub Actions `actionlint` | 通过 |
| GitHub 实际构建与 Release 上传 | 未执行，尚未创建远端仓库 |
| IDEA Debug / HotSwap 交互 | 未执行，用户尚未安装 IDEA |

本地详细日志在忽略目录 `.local/` 中：`build-<MC>.log`、`smoke-server-<MC>.log`。JAR 与 SHA256 校验文件收集到 `dist/`，不提交 Git。

Forge 的网络预检曾等待过长；设置 Java 的连接与读取超时后重试。已将通用超时配置写入各目标的 `gradle.properties`，没有跳过 TLS 验证。本机临时缓存配置仍只留在 `.local/`。

用户已明确同意仅在本地开发测试目录接受 Minecraft EULA；`eula.txt` 与测试世界全部忽略，不随项目发布。

1.19.1 的历史 `javabridge` 库解析曾失败，已显式声明 Mojang 仓库并重试通过。旧版 ForgeGradle 5 默认 `forceExit` 在游戏结束后调用 `System.exit(0)`，会让 Gradle daemon 消失；已仅对该目标设置 `forceExit false`。参见 [ForgeGradle 5 运行配置](https://docs.minecraftforge.net/en/fg-5.x/configuration/runs/)。

主版本 IDEA 配置已生成到 `versions/1.20.1/.idea/runConfigurations/`，其中包含 Apple Silicon/macOS 需要的 `-XstartOnFirstThread`，且不提交本机绝对路径。开发客户端保留打开，所有本次自动测试服务端均已停服。

未验证范围：1.19.1 / 1.21.1 图形客户端、两个次要目标发行 JAR 在普通启动器/服务端的安装、客户端进入单人世界、真实多人认证和整合包兼容性。次要目标当前验证的是发行包结构及源码开发服务端运行。IDEA 尚未安装，因此 Debug/HotSwap 仅提供配置和步骤，未做 IDEA 交互实测。
