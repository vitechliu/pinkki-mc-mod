# 使用 HMCL 测试 PinkkiCore

本文面向用户当前使用的 HMCL 3.16.3。游戏由用户手动启动；代理默认只执行构建、文件准备与无头验证。

## 之前的测试流程与下载来源

初始化时分别执行了三类检查：

1. 使用官方 MDK 的 Gradle 工具链编译各目标，对 JAR 内元数据、共享类、加载器入口、Java 字节码版本、MIT 许可证及 SHA256 做校验。
2. 启动三个版本的无头开发服务端，确认 PinkkiCore 加载、世界就绪，然后发送 `stop`，确认保存完成和退出码为 0。
3. 主版本额外测试：把发行 JAR 安装到普通 Forge 独立服务端并完成上述启动/停服检查；曾启动开发客户端，由用户确认 Mods 中存在 PinkkiCore。此为历史验收，不代表以后可以自动启动客户端。

Minecraft 1.20.1 原始客户端和服务端由工具链从 Mojang 官方站点获取，Forge MDK 与安装器从 Forge 官方 Maven 获取。2026-10-22 再次核对本机缓存，结果如下：

| 文件 | 官方下载地址 | 缓存 SHA-1 与版本元数据一致 |
| --- | --- | --- |
| 原始客户端 | [Mojang client.jar](https://piston-data.mojang.com/v1/objects/0c3ec587af28e5a785c0b4a7b8a30f9a8f78f838/client.jar) | 是 |
| 原始服务端 | [Mojang server.jar](https://piston-data.mojang.com/v1/objects/84194a2f286ef7c14ed7ce0090dba59902951553/server.jar) | 是 |
| Forge MDK / 安装器 | [Forge 1.20.1 官方下载页](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) | 来源为官方 Maven；服务端安装日志包含校验成功记录 |

Gradle 开发环境随后会对原始游戏做映射、补丁和开发运行配置处理。HMCL 测试使用普通加载器和发行 mod JAR，更适合验证其他 mod 兼容性。无需把 Gradle 缓存中的映射 JAR 当成整合包导入 HMCL。

原有测试位置：开发客户端日志位于 `versions/1.20.1/run/client/logs/`；开发服务端日志位于对应版本的 `run/server/logs/`；构建及自动测试摘要位于 `.local/`；普通 Forge 服务端安装测试位于 `.local/package-test/1.20.1/`。

## 项目专用 HMCL 目录

项目专用游戏根目录（将 `<项目根目录>` 替换为本机仓库的绝对路径）：

```text
<项目根目录>/.local/hmcl/.minecraft
```

在 HMCL 中新增并选中这个游戏目录，保留原来其他游戏目录。然后按 [HMCL 官方版本隔离说明](https://docs.hmcl.net/launcher/set-item-details.html)，把项目实例设置为“各实例独立”。它会让 mods、配置、存档和游戏日志分别位于对应版本目录中。

macOS 文件选择框默认隐藏 `.local`。可以按 `Command+Shift+G`，粘贴替换后的完整路径定位，无需修改隐藏文件显示偏好。

建议创建以下两个主版本实例，名称按这里填写，便于辅助脚本定位：

| 实例名称 | 用途 | 安装内容 |
| --- | --- | --- |
| `PinkkiCore-1.20.1-clean` | 确认 PinkkiCore 自身功能 | Minecraft 1.20.1 + Forge 47.4.28 + GeckoLib 4.x（至少 4.4.9）+ PinkkiCore |
| `PinkkiCore-1.20.1-compat` | 与日常 mod 一起测试 | 同上，再加其他 mod 及所需前置 |

通过 HMCL 的新实例安装流程安装 Minecraft 1.20.1，并选择 Forge 47.4.28；也可以从已有实例复制/导入后调整实例名称和隔离设置。若当前安装界面没有相应入口，先保留目录规划，把 HMCL 版本和界面提示提供出来再确定安装路径。

游戏 Java 自定义为项目 JDK 17 的可执行文件（以下为 macOS 路径）：

```text
<项目根目录>/.local/jdk-17/Contents/Home/bin/java
```

创建后，在 HMCL 中使用“打开游戏文件夹”确认实际目录，例如：

```text
.local/hmcl/
├── .minecraft/
│   ├── assets/                         公共游戏资源
│   ├── libraries/                      公共依赖
│   └── versions/
│       ├── PinkkiCore-1.20.1-clean/
│       │   ├── PinkkiCore-1.20.1-clean.json
│       │   ├── mods/
│       │   ├── config/
│       │   ├── saves/
│       │   ├── logs/latest.log
│       │   └── crash-reports/
│       └── PinkkiCore-1.20.1-compat/     同样结构，可安装其他 mod
├── backups/                           同步 mod 前保留的旧 PinkkiCore JAR
├── deployments/                       构建日志、安装回执和文件哈希
├── runtime-cache/                     校验过的前置 mod 原始发行包
├── reports/                           按时间归档的诊断日志与 mod 清单
└── launcher-logs/                     手动保存 HMCL 启动日志或错误报告
```

实际游戏实例由 HMCL 创建，准备脚本不会伪造安装完成的版本目录。上述目录全部在 `.local/` 中，已被 Git 忽略。

账号与启动器全局配置仍由你现有的 HMCL 管理；这个方案集中保存游戏测试文件。启动器在游戏进程创建前就失败时，可能还没有 `latest.log`，需把 HMCL 的“导出游戏日志”结果手动保存到 `launcher-logs/` 或本次报告目录。仅新增游戏目录不能保证 HMCL 自身的全局日志也自动迁移。

## 添加其他 mod 与更新开发产物

第三方 mod 放进 `compat` 实例的 `mods/`，也可以通过 HMCL 的模组管理添加。确认每个 mod 同时支持 **Minecraft 1.20.1 与 Forge**，并安装所需前置；不混用 NeoForge 或 Fabric 文件。详见 [HMCL 官方模组安装文档](https://docs.hmcl.net/launcher/auto-installing.html)。

1.20.1 的 clean 与 compat 实例都需要 GeckoLib。新建独立实例可使用 [GeckoLib 4.8.4 Forge 1.20.1](https://modrinth.com/mod/geckolib/version/aC5KMoNg)；导入整合包时先保留原有满足 `[4.4.9,5)` 的版本，同一实例只留一份。Forge 运行下限为 47.1.0，不能只因构建 SDK 为 47.4.28 就升级整合包。具体包仍按 [兼容策略](geckolib-compatibility.md) 核对。

日常本地部署：先手动退出测试游戏，再从项目根目录执行（macOS/Linux，Python 3.11+）：

```bash
# 默认构建并校验 1.20.1，自动安装到 clean 和 compat
python3 scripts/deploy-local.py

# 仅在明确指定单个实例时使用
python3 scripts/deploy-local.py --profile compat
```

以后要求代理“部署”时，默认执行上述命令，把当前发行 JAR 安装进两个实例，不能只构建后就宣称部署完成。不启动 HMCL 或游戏，也不发布 GitHub Release 或部署真实服务端。脚本不安装 Minecraft/Forge；实例未安装、隔离设置不明确、Java 不是 17 或游戏未退出时会停止。

部署先检查实例的 Minecraft/Forge、实际 Java 和独立目录，再构建并校验发行 JAR。缺少 GeckoLib 时自动安装固定的 Forge 1.20.1 / 4.4.9 原始发行包；优先复用经过 SHA512 校验的原始 Gradle 缓存，否则从 Modrinth CDN 下载，固定地址与校验值见 `config/hmcl-runtime.json`。不会安装开发映射 JAR、单独的 MCLib 或可选 JEI。已有满足要求的 GeckoLib 4.x 保留；旧版、重复或无法确认兼容性时停止，交由用户处理，不擅自升级整合包。

旧 PinkkiCore 备份到 `.local/hmcl/backups/`，替换后清除同一 mod ID 的旧 JAR；第三方 mod、配置和存档保留。每次部署的构建日志与安装回执位于 `.local/hmcl/deployments/<时间>/build.log`、`receipt.json`，包括实际 Java、Forge、发行包及安装文件哈希。构建或校验失败不会进入安装；如果安装阶段发生磁盘错误，可能只完成部分实例，检查回执并修复后重试，不视作全部部署成功。部署期间请勿手动启动游戏。

`hmcl-test.py sync` 是仅校验与复制已有 JAR 的低层工具，不构建、不补前置或检查运行状态；日常部署统一使用 `deploy-local.py`。

然后手动从 HMCL 启动所选实例。发行 JAR 替换后需要重启游戏，不能像 IDEA 的 Debug 方法体 HotSwap 那样直接生效。资源包/数据包重载仍取决于具体修改内容。

只改贴图、模型、语言或已有配方数据时，可使用 `python3 scripts/hot-sync.py` 准备受管覆盖包，用户手动重载；数据包同步、移除和日志操作见 [资源热同步](hot-sync.md)。用户已确认两个项目实例的所有世界均为测试世界，默认自动同步全部已有世界的数据包，无需指定世界；不替换 mod JAR。正式 JAR 验收前应移除覆盖包，避免旧资源遮住新包。

建议先在 clean 实例确认，再在 compat 实例重现。增加其他 mod 时分批安装，每批记录名称与版本；涉及游戏内容的测试使用专用测试世界，避免把日常存档当成可反复删 mod 的测试场地。

## 留下可分析的日志

异常发生后先退出游戏，并在再次启动覆盖 `latest.log` 之前归档：

```bash
python3 scripts/hmcl-test.py report 1.20.1 compat \
  --note '复现：进入测试世界后打开背包；预期：正常显示；实际：崩溃'
```

报告目录包含当前 `latest.log`、`debug.log`（如果有）、最近三份崩溃报告、实例根目录的 JVM 崩溃日志（如果有），以及各 mod 文件名和 SHA256 清单。未启动过游戏也可以生成清单，脚本会提示缺少日志。报告是本地目录，不会自动上传。

以后可以直接告诉代理：“分析 `.local/hmcl/reports/<本次目录>`，先只读排查。”报告文件会保留，后续启动不会覆盖；当前原始日志仍留在实例的 `logs/`。

如需把 HMCL 导出的 ZIP 保存下来，可放在同一个报告目录。辅助脚本不会自动读取账号文件、复制存档或打包全部 mod；遇到特定配置问题时再读取与问题相关的配置。

## 扩展到其他 Minecraft 版本

相同目录规则适用于 `PinkkiCore-1.21.1-clean/compat` 和 `PinkkiCore-1.19.1-clean/compat`。1.21.1 安装 NeoForge、选择 JDK 21；1.19.1 安装 Forge、选择 JDK 17，并使用各自版本的第三方 mod。辅助脚本接受对应 Minecraft 版本参数，实例名称保持一致即可。
