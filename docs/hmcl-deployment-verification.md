# HMCL 本地部署检查（2026-10-23）

本次仅检查文件、实际 Java 可执行文件、既有日志和安装产物；没有启动 HMCL 或 Minecraft，也没有操作游戏或连接服务器。

## 实例检查

两个实例均位于项目 `.local/hmcl/.minecraft/versions/`：

| 项目 | clean | compat |
| --- | --- | --- |
| Minecraft / 加载器 | 1.20.1 / Forge 47.4.28 | 1.20.1 / Forge 47.4.28 |
| 实际自定义 Java | Temurin 17.0.20.1，aarch64 | Temurin 17.0.20.1，aarch64 |
| Java 来源 | 项目 `.local/jdk-17/Contents/Home/bin/java` | 同左 |
| 实例隔离 | 通过 | 通过 |
| Minecraft 原始客户端 SHA1 | `0c3ec587af28e5a785c0b4a7b8a30f9a8f78f838` | 同左 |

客户端哈希与项目缓存的 Mojang 1.20.1 元数据一致。Java 检查直接执行配置中的 `java -XshowSettings:properties -version`，不依赖系统默认 Java。

HMCL 3.16.3 的实例配置均显式覆盖 `runningDirectory`、`javaType` 和 `customJavaPath`。`runningDirectory` 缺省空字符串在覆盖模式下表示当前实例目录；已通过本地 HMCL JAR 的 `HMCLGameRepository#getRunDirectory` 字节码核对。clean 既有日志也记录了正确的项目独立目录和 Java 17 ARM64，并曾进入世界、正常保存退出；这些是安装 PinkkiCore **之前**的历史日志，不能证明本次 mod 已加载。compat 没有此前的游戏启动日志。

clean 历史日志出现账号认证 HTTP 401 和 profile key 获取失败。如果后续正版联机仍出现认证错误，在 HMCL 退出并重新登录账号；这不表示 Java 或实例隔离配置错误。没有读取账号配置或修改登录信息。内存未见显式覆盖，沿用 HMCL 默认策略；尚未验证大规模整合包的内存与性能。

## 本次安装与验证

原先 clean 的 mods 为空，compat 尚无 mods 目录。执行 `python3 scripts/deploy-local.py` 后，两者均安装：

- `pinkkicore-forge-1.20.1-0.1.0.jar`
- `geckolib-forge-1.20.1-4.4.9.jar`（必须的独立前置，未另装开发用 MCLib 或可选 JEI）

1.20.1 构建和现有 `verify-artifact.py` 校验通过；两份已安装 PinkkiCore 与发行包 SHA256 一致：`2e1e4f4dbe2a4a7246a34d7a447828ad8d8135e797c164986412d361a881c7de`。GeckoLib 安装前匹配固定发行包 SHA512，两实例文件 SHA256 均为 `5d5fa42a52f11a7389b3fb1739efcfc6957ff8236872a71382fb67cff58b8fa2`。未安装探针 JAR。

实际构建日志和回执位于 `.local/hmcl/deployments/20261023-165034-322855/`。文件级部署回归命令：

```bash
python3 -m unittest discover -s scripts/tests -p test_hmcl_deploy.py -v
```

覆盖双实例安装、原 PinkkiCore 备份、其他 mod/存档保留、兼容前置不替换、过旧或重复前置拒绝、错误版本/隔离拒绝、运行中实例拒绝、构建失败不安装、下载哈希不匹配拒绝和目录外符号链接拒绝。

本次只改变本地部署工具及协作说明，没有改变玩家玩法、游戏资源或接口，因此不产生次要版本适配待办，不需要同步游戏内手册或玩家 Wiki。未执行服务端冒烟、客户端验收、其他 mod 兼容性实测或远程发布；安装完成不等于上述项目已验证。用户可以手动启动 clean，在 Mods 确认 PinkkiCore 和 GeckoLib，再按需测试 compat；异常后使用 `hmcl-test.py report` 保存日志。
