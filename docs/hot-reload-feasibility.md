# 1.20.1 本地热更新可行性调研

调研日期：2026-10-23。范围：Minecraft 1.20.1 / Forge 47.4.28，项目 HMCL clean、compat 实例和 Java 17。仅核对项目配置、源码、依赖字节码及官方资料；没有启动游戏、安装代理、更换 Java 或启用调试端口。以下是方案与边界，不代表交互验收已经通过。

后续落地：资源/数据覆盖包同步现已通过 `scripts/hot-sync.py` 实现，命令与文件级验证见 [资源热同步](hot-sync.md)。用户随后授权这两个项目实例的所有世界仅用于测试，现默认同步全部已有世界，无需逐次指定；下文关于显式选择世界的内容为原调研方案，不再是当前默认流程。Java 热替换和增强 JVM 尚未配置，客户端热重载仍未验收；下文保留原调研方案。

## 结论与推荐顺序

支持部分热更新，优先采用两种互补方式：

1. 用户继续在 HMCL 测试，使用项目专用的外置资源包/世界数据包覆盖 PinkkiCore 资源，再手动重载。这条路径不要求打开 IDEA。
2. 需要频繁修改 Java 逻辑时，用 IDEA Debug 运行 Forge 开发客户端，编译后执行 HotSwap。其他测试 mod 可以作为经过 `fg.deobf` 处理的开发依赖加入；开发环境与普通 HMCL 实例仍应分别验证。

不把运行中替换完整发行 JAR 作为热更新。现有 `deploy-local.py` 继续用于停游戏后的完整部署；后续若实现资源同步，应使用独立命令，只管理其自有目录并记录清单，不能借热更新命令覆盖运行中的 mod JAR。

## 哪些修改可以生效

| 修改 | 推荐操作 | 边界 |
| --- | --- | --- |
| 物品/方块贴图、语言、已有模型的资源内容 | 同步到启用的外置资源包，用户按 F3+T | 首次需启用包；客户端重载可能短暂停顿，不承诺固定耗时 |
| 饼蛙 GeckoLib 模型、动画 JSON、贴图 | 同上 | 当前 4.4.9 的 GeckoLibCache 有资源重载监听，并重新读取动画/模型；活动动画、骨骼修改等显示效果需人工验收，必要时重新生成测试实体 |
| 已注册配方类型下的配方、标签、战利品表 | 同步到测试世界数据包，用户执行 /reload | 需要权限；多人应更新服务端数据包；不是所有世界生成/注册数据都能这样重载 |
| 工坊输入、产物、数量、瓜霸消耗 | 更新配方数据包并 /reload | 当前工坊每 tick 从 RecipeManager 查询，具备使用新配方的代码路径；JEI 显示刷新仍待客户端验收 |
| 已加载类的方法体，如按钮逻辑、伤害计算、工坊 tick 处理 | IDEA Debug + 编译 + Reload Changed Classes | 标准 Java 17 支持的范围；改动在后续调用生效，已有对象状态不自动重建 |
| 工坊加工时长、饼蛙基础属性/初始化逻辑 | 先区分常量、注册与运行时计算 | 当前加工时长是 Java 的 DURATION=20 常量，不属于配方 JSON；常量可能被编译内联，需重新编译并替换实际使用它的类。属性注册/初始化不会因替换方法而自动重放 |
| 新物品/方块/实体注册、网络协议、Mixin、依赖变更 | 重启游戏 | 不纳入常规热更新承诺 |
| 新增/删除字段、方法、改继承关系 | 标准 HotSwap 通常拒绝；默认重启 | 可研究增强 JVM，但并不因此获得 Forge 生命周期重放能力 |

资源包使用 `assets/pinkkicore/...`，数据包使用 `data/pinkkicore/...`，保持原资源 ID，以优先级覆盖 JAR 中的内容。1.20.1 的资源包与数据包 `pack_format` 分别为 15、15，后续实现时应从目标配置校验。外置包不会让 Java 识别尚未注册的新物品或新配方类型。

建议按实例建立自有资源包目录，数据包只放在用户指定的测试世界内；首次安装/启用之后可以重复同步文件。删除或改名资源也要同步删除旧的受管文件。退出热更新模式进行正式 JAR 验收前，停用外置覆盖包，否则旧覆盖可能遮住新 JAR 的真实内容。日志继续留在实例 logs 与项目 reports 中，并附上当时启用的覆盖包清单。

## 为什么 HMCL 连接调试器仍有额外成本

IDEA 支持附加到开启 JDWP 的普通 Java 进程，故 HMCL 启动的 Java 进程原则上也可附加。后续若采用，只监听本机地址，例如 `address=127.0.0.1:5005`；当前没有修改实例 JVM 参数。

但 ForgeGradle 开发环境使用可读映射，普通发行包使用生产 SRG 映射。当前项目以 official mappings 编译，通过 `reobfJar` 制作发行包。因此“加一个调试参数，然后直接把 IDEA 编译的 class 热替换到 HMCL”不足以成立：引用 Minecraft API 的类需要匹配生产命名，且必须考虑 ModLauncher 的类转换。纯共享 Java 核心的限制较少，但仍需逐类验证，不能推广成所有逻辑可替换。

坚持在 HMCL 内热换 Java 代码时，可研究重新混淆后的类提取和重定义工具，或专用 Minecraft 热替换工具。找到的 McRemoteHotswap 项目提供 Forge/Fabric 远程替换思路及 1.20 测试目录，但其说明不足以证明与本项目 Forge 47.4.28、当前依赖及所有类转换兼容；不作为默认依赖，不承诺直接可用。

## 增强 JVM 的价值与边界

JetBrains Runtime 支持增强类重定义，HotSwap Agent 官方给出了 JBR 17/21 配置流程，可扩大结构变化的替换范围。它不是把一个 agent 放入现有 Temurin 就自动获得同等能力；需要支持增强重定义的 JVM、匹配的参数与代理配置。

若后续采用，单独准备项目内的 JBR 17 ARM64 调试环境，保留发行构建和 HMCL 常规验收的 Temurin 17。当前未选定或验证具体二进制，不用 IDEA 自带的较新 Java 直接替代目标游戏 Java。增强重定义仍不会自动重新注册内容、重新初始化所有对象，或保证整合包兼容，因此排在基础资源重载与标准 HotSwap 之后。

## 后续落地与验收范围

建议第一阶段只实现 HMCL 的资源/数据包同步：限定 1.20.1、指定实例与测试世界，生成受管包及同步清单、校验 JSON、记录日志；游戏由用户启动和按键重载。随后配置独立 IDEA 调试运行入口，确认实际类/资源输出目录，再由用户验收方法体替换。高级 JVM 和 HMCL 发行类热替换作为后续独立实验。

配置和文件同步可以无头验证；“按 F3+T 后模型更新”“/reload 后配方与 JEI 更新”“HotSwap 后按钮或伤害行为改变”仍需用户手动验收。研究与开发工具不改变玩法，本次无需修改玩家手册、Wiki 或次要版本适配检查点。

## 资料

- [Forge 1.20.1 资源系统](https://docs.minecraftforge.net/en/1.20.1/concepts/resources/)：assets/data 与包覆盖规则。
- [Mojang 数据包与 /reload 说明](https://www.minecraft.net/it-it/article/minecraft-snapshot-17w43a)：运行时修改数据包后的重载机制；具体 1.20.1 行为还需目标版本验收。
- [IDEA HotSwap 及限制](https://www.jetbrains.com/help/idea/altering-the-program-s-execution-flow.html)：方法体替换、成员/签名限制及栈内方法的生效时间。
- [IDEA 远程调试](https://www.jetbrains.com/help/idea/tutorial-remote-debug.html)：附加 JDWP 进程。
- [ForgeGradle 6 依赖与映射](https://docs.minecraftforge.net/en/fg-6.x/dependencies/)：开发映射与发行 SRG，以及第三方 mod 的 fg.deobf。
- [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime)、[JetBrains 的 DCEVM 配置说明](https://youtrack.jetbrains.com/articles/SUPPORT-A-725)、[HotSwap Agent](https://github.com/HotswapProjects/HotswapAgent)：增强重定义所需环境与配置。
- [McRemoteHotswap 源项目](https://github.com/unimined/McRemoteHotswap)：候选研究工具，未经本项目验证。
