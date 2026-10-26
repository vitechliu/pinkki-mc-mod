# PinkkiCore

面向 Minecraft Java 版的多版本 mod，MIT 许可证。1.20.1 已实装基础材料[瓜巴](wiki/items.md#guaba)及其无序配方，中立生物[饼蛙和刷怪蛋](wiki/bosses.md#bingwa)、可拾取装备的人形 NPC [粉红妹](wiki/bosses.md#pinkki)，以及支持转换加工与自动化设置的[瓜巴工作台](wiki/guaba-workbench.md)（0.1.0 开发中，尚未发布）；次要目标尚未适配。

| 优先级 | Minecraft | 加载器 | Java | 构建工具 |
| --- | --- | --- | --- | --- |
| 主要 | 1.20.1 | Forge 47.4.28 | JDK 17 | Gradle 8.8 / ForgeGradle 6.0.54 |
| 次要 | 1.21.1 | NeoForge 21.1.264 | JDK 21 | Gradle 9.2.1 / ModDevGradle 2.0.148 |
| 次要 | 1.19.1 | Forge 42.0.9 | JDK 17 | Gradle 7.5 / ForgeGradle 5.1.77 |

Minecraft、加载器、Gradle 和构建插件版本固定在仓库配置中。升级这些版本时需重新构建、启动测试。**1.19.1 与 1.19.2 是不同目标，不可混装 JAR。**

**日常只开发、构建、无头测试和准备部署 1.20.1。** 其他版本允许暂时落后，需要时调用项目 skill `$port-minecraft-version` 补齐指定目标。每次功能变更记入 `docs/changes/`；[开发与适配流程](docs/development-workflow.md) 说明如何记录和补漏。代理默认不启动客户端，下文客户端步骤供用户手动操作。

## 游戏内瓜巴手册

使用一本书与一个瓜巴无序合成手册，右键离线阅读。正文与玩家 Wiki 同源，支持分类、中文搜索、链接跳转和真实配方图，无第三方手册前置。[玩家用法](wiki/handbook.md) · [维护与验证](docs/guidebook.md)。

## 玩家 Wiki

[在线 Wiki](https://vitechliu.github.io/pinkki-mc-mod/)（首次 Pages 部署成功后可访问），源文件位于 `wiki/`。道具、合成、结构、多方块和 Boss 等玩法变更必须与 Wiki 同步提交。开发资料继续保留在 `docs/`；本地预览和发布方式见 [Wiki 维护说明](docs/wiki.md)。

## GeckoLib 前置与整合包兼容

1.20.1 Forge 已接入 GeckoLib：使用 **4.4.9 作为编译 API 基线**，运行时允许 **`[4.4.9,5)`**。客户端与服务端都需要安装匹配 **1.20.1 Forge** 的 GeckoLib；整合包已有满足范围的版本时保留原版本，不额外放入第二份，也不因本项目开发而默认升级它。独立新实例可选 4.8.4，具体整合包仍需验证。

表中 Forge 47.4.28 是固定构建 SDK，运行最低版本单独配置为 47.1.0。GeckoLib 的新功能不能绕过编译基线直接使用；修改任一最低版本都应说明原因并重新验证。详见 [依赖选型、验证范围与整合包核对流程](docs/geckolib-compatibility.md)。次要目标尚未接入本次依赖；ATM10 为 1.21.1，需后续按需适配 NeoForge 目标。

模型设计流程的首个样例是 [饼蛙造型与动作审阅](designs/bingwa/README.md)，包含可编辑 Blockbench 工程及七段动作。已在 1.20.1 接入实体与刷怪蛋；[玩家说明](wiki/bosses.md#bingwa)包含指令、获取方式与中立反击规则。

后续建模可使用项目 skill [`$design-minecraft-model`](.agents/skills/design-minecraft-model/SKILL.md)，从参考设计、模型与动作制作，到可旋转 3D 审阅和资产校验。用法见 [模型设计流程](docs/development-workflow.md#模型设计与审阅)。

## 瓜巴工作台

1.20.1 工作台已接入确认后的模型，右键打开三槽转换界面；每次 20 刻，先以泥土转泥巴作为占位配方。支持四态红石、六面输入输出配置和漏斗/Forge 物品管道。当前从创造模式“功能方块”获取，暂无生存合成配方。[玩家操作](wiki/guaba-workbench.md)与[开发、数据包及无头验证说明](docs/guaba-workbench.md)已同步；游戏内手册已接入，使用书与瓜巴合成，右键阅读，客户端仍待手工验收。

## 瓜巴材料兼容试作

1.20.1 已为 Silent Gear（寂静装备）、Tinkers' Construct（匠魂）和 Tetra 添加可选的瓜巴材料数据；不要求安装三个模组，不内嵌它们或其前置。基础属性仍为兼容性占位，现已接入“瓜巴之触”：命中目标施加瓜巴 I，持续 10 秒。详见[调研、参数与验证范围](docs/guaba-material-compatibility.md)及[玩家操作说明](wiki/compatibility.md)。游戏内手册已接入，使用书与瓜巴合成，右键阅读。

## 从零开始：本机运行

本机是 Apple Silicon macOS。已安装的系统 Java 无需切换；脚本为每个目标选择 JDK。通用脚本需要 Bash、curl、tar；产物校验需要 Python 3.11+。

在项目根目录执行：

```bash
# 新电脑首次使用：下载缺少的 JDK，放进 .local/，不改系统默认 Java
./scripts/setup-java.sh

# 主版本编译打包（第一次会下载 Minecraft、Forge 和 Gradle，耗时较长）
./scripts/mod.sh 1.20.1 build

# 直接启动开发客户端，无需把 JAR 手动复制到启动器
./scripts/mod.sh 1.20.1 runClient
```

客户端启动后：打开 **Mods**，搜索 **PinkkiCore**，确认版本为 `0.1.0`。新建一个测试世界，进入、退出并关闭游戏。日志 `versions/1.20.1/run/client/logs/latest.log` 应包含：

```text
[pinkkicore] Mod loaded successfully.
```

1.20.1 的瓜巴位于创造模式“原材料”分类，也可用 1 西瓜片和 1 原版泥巴在背包或工作台中无序合成。不可食用，堆叠上限 64；悬停说明为“一切瓜巴的起源”。可使用书与瓜巴合成瓜巴手册，右键离线阅读，正文与玩家 Wiki 同源。开发客户端采用开发身份，验证正版启动器和真实多人认证属于打包后测试。

只有需要适配其他版本、且由用户手动测试客户端时，才替换版本号：

```bash
./scripts/mod.sh 1.21.1 runClient
./scripts/mod.sh 1.19.1 runClient
```

明确要求全部目标时，才构建并收集发行 JAR 与 SHA256 到 `dist/`：

```bash
./scripts/build-all.sh
```

单版本产物位于 `versions/<版本>/build/libs/`，例如：

```text
pinkkicore-forge-1.20.1-0.1.0.jar
pinkkicore-neoforge-1.21.1-0.1.0.jar
pinkkicore-forge-1.19.1-0.1.0.jar
```

## IDEA 开发与调试

1. 安装 IntelliJ IDEA 的免费 Java 开发功能。打开 **`versions/1.20.1`**，作为 Gradle 项目导入；初次开发先专注主版本。不要把根目录当成一个 Gradle 多模块工程，它有三套独立 Wrapper。
2. 在 Settings → Build, Execution, Deployment → Build Tools → Gradle 中使用 **Gradle Wrapper**，Gradle JVM 选择 **JDK 17**；Project Structure 中的 Project SDK 也选 17。本机项目 JDK 路径为 `.local/jdk-17/Contents/Home`，相对于仓库根目录。IDEA 若未显示 `.local`，可直接粘贴完整路径。
3. 在根目录执行 `./scripts/mod.sh 1.20.1 genIntellijRuns`，然后在 IDEA 刷新 Gradle。选择生成的客户端配置，以 **Debug** 启动。
4. `common/src/main/java` 已纳入 source set，IDEA 应识别为共享源码根目录，不需要手工复制。
5. 1.19.1 同样使用 JDK 17 和 `genIntellijRuns`；1.21.1 使用 JDK 21，ModDevGradle 在 Gradle 同步时生成运行配置，不使用 Forge 的 `genIntellijRuns` 命令。

建议每个版本开独立 IDEA 窗口。IDEA 里的 Gradle User Home 可以指定仓库的 `.local/gradle`，复用命令行已下载的依赖；否则 IDEA 会使用自己的默认缓存并再次下载。不要把这些本机绝对路径提交到 Git。

### 热更新能做到什么

HMCL 已提供独立命令 `python3 scripts/hot-sync.py`，默认同步 1.20.1 两个项目实例的客户端资源包及全部已有测试世界的数据包，不必指定世界；首次由用户确认启用，之后资源同步完按 F3+T，数据同步完执行 /reload。具体用法、日志和正式验收前的移除方式见 [资源热同步](docs/hot-sync.md)。它不热替换 Java 或 mod JAR。

| 修改类型 | 操作 | 限制 |
| --- | --- | --- |
| 纹理、语言、模型等客户端资源 | 编译/复制资源后，游戏中按 `F3+T`（Mac 可能要配合 Fn） | 文件必须先到运行中的资源目录 |
| 配方、战利品表等数据包资源 | 编译/复制资源后，世界中输入 `/reload` | 需要权限，取决于对应数据是否支持重载 |
| 已加载 Java 类的方法体 | Debug 启动后，重新编译，IDEA 执行 Reload Changed Classes | 标准 HotSwap；下一次调用方法才执行新逻辑 |
| 新增字段/方法、改类结构、注册物品、Mixin、构造和初始化逻辑 | 重启开发客户端/服务端 | HotSwap 不会重放 mod 初始化和注册流程 |

HotSwap 设置位于 Settings → Build, Execution, Deployment → Debugger → HotSwap。若 IDEA 提示不能替换类，重启即可。命令行 `runClient` 的普通运行模式不会自动启用 HotSwap。资源修改后可执行 `./scripts/mod.sh 1.20.1 processResources`；使用 IDEA 原生运行配置时，还需确认修改进入该配置实际使用的输出目录（IDEA Build Project）。方法体替换不会重放初始化或自动重建已有对象状态；需在后续调用时验证变化，也可配合断点与 Debug 的 Evaluate Expression。

不要在运行中替换 `mods/` 下的 JAR；加载器不支持完整 mod 卸载重载。当前未安装额外的 HotSwap Agent 或修改 JVM。

## 测试独立服务端

```bash
./scripts/mod.sh 1.20.1 runServer
```

首次运行通常因 EULA 未接受而退出。阅读 [Minecraft EULA](https://www.minecraft.net/eula)，同意后将 `versions/1.20.1/run/server/eula.txt` 中的 `eula=false` 改为 `eula=true`，再次运行。看到 `Done (...)!` 后说明服务器已就绪。在控制台输入 `stop` 正常关闭。

每个版本的服务端目录彼此隔离，也与客户端目录隔离。一次只启动一个使用默认端口 25565 的服务端；多个同时启动需要分别配置端口。本地客户端连接 `localhost`；正版客户端联机还需匹配服务端加载器、版本与认证要求。

已同意 EULA 后，也可以执行自动冒烟检查（需 Python 3.11+）：

```bash
python3 scripts/smoke-server.py 1.20.1
```

脚本要求观察到 mod 加载日志和服务器 `Done`，随后发送 `stop` 并检查正常退出。它跳过独立服务端不需要的客户端资源下载；第一次仍可能准备游戏代码与依赖，默认超时 600 秒，可用 `--timeout 1200` 延长。仅在没有同目标测试服运行时执行。本次初始化的测试服务端已绑定 `127.0.0.1`，供本机使用；1.20.1、1.21.1、1.19.1 的本机测试端口分别为 25565、25566、25567。

后续代码涉及服务端时必须重复测试这一步，防止通用代码误引用 `net.minecraft.client.*`，导致单机正常而独立服务端崩溃。当前没有 GameTest 内容，未配置会因“无测试”失败的空 `runGameTestServer`。

## 打包后安装

1. 使用启动器创建独立游戏实例，安装表中对应 Minecraft 和 Forge/NeoForge 版本；各实例独立游戏目录。
2. 从对应 `build/libs/` 或 GitHub Release 选一个匹配目标的 JAR，放进该实例 `mods/`，同一 mod 只保留一个版本。
   1.20.1 Forge 还需 GeckoLib 4.4.9 或以上的 4.x；已有兼容前置时直接复用，不能混装 Forge、NeoForge、Fabric 文件。
3. 启动，在 Mods 列表及日志中确认加载，进入测试世界；退出后再启动一次确认存档正常。
4. 独立服务端使用相同加载器安装流程，把同目标 JAR 放进服务端 `mods/`。生产部署前先在测试服验证，备份存档，停服替换，保留旧 JAR 以便回滚。
   1.20.1 服务端也需安装匹配的 GeckoLib，建议与客户端使用同一版本。

`runClient` 验证开发环境；发行 JAR 的实际安装测试是另一项检查。构建通过不代表任意整合包、其他 mod 或生产环境均兼容。

## Git 与自动发布

使用 HMCL 安装其他 mod、测试发行 JAR 并在项目内保留日志，参见 [HMCL 本地测试流程](docs/hmcl-testing.md)。 已创建项目实例后，运行 `python3 scripts/deploy-local.py` 即可构建 1.20.1 并自动安装到 clean、compat，缺少 GeckoLib 时补齐；不会启动游戏。

详见 [GitHub 发布说明](docs/release.md)、[代码结构与版本策略](docs/architecture.md) 和 [本次验证记录](docs/verification.md)。

## 参考资料

- [Mod 源码参考库](docs/reference-mods.md)：十二个 Mod 的 1.20.1 本地源码、分支及检索方法。

- [Forge 1.20.1 开发入门](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)
- [Forge 1.19.1 官方下载](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.19.1.html)
- [NeoForge 1.21.1 官方 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)
- [NeoForge ModDevGradle 文档](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
- [IDEA HotSwap 文档](https://www.jetbrains.com/help/idea/altering-the-program-s-execution-flow.html)
