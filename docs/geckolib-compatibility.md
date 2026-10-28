# GeckoLib 与整合包兼容策略

1.20.1 选型核对日期：2026-10-22。下表保留该目标的 4.x 策略。2026-10-28 补齐 1.19.1 Forge，独立采用 GeckoLib 3.1.30（`geckolib3`），运行范围 `[3.1.30,4)`；旧控制器和单次动作同步在该版本适配层实现，不能混用 4.x 前置。详见 [1.19.1 适配记录](changes/2026-10-28-port-1.19.1.md)。1.21.1 本次未处理。

## 版本选择

| 用途 | 版本 / 范围 | 原因 |
| --- | --- | --- |
| 编译与默认开发运行 | GeckoLib 4.4.9 | 使用较低的 4.x API 基线；该版本包含 Forge 护甲渲染崩溃修正 |
| PinkkiCore 运行前置 | `[4.4.9,5)` | 复用整合包已有的兼容 4.x，不要求精确安装编译版本，不承诺 GeckoLib 5 API 兼容 |
| 新建独立实例的可选版本 | GeckoLib 4.8.4 / Forge 1.20.1 | 核对时该目标最新正式版；不是 PinkkiCore 的最低要求 |
| Forge 构建 SDK | 47.4.28 | 保留现有工具链 |
| Forge 运行最低版本 | 47.1.0，范围 `[47.1.0,48)` | 与候选 GeckoLib JAR 声明的 Forge `[47.1,)` 下限一致，避免把构建 SDK 自动作为整合包升级要求 |

4.4.9 是本项目选择的支持下限，不表示所有历史整合包都已满足它。低于该版本的整合包仍需逐包评估。允许某个版本通过依赖检查，也不等于已经验证它的全部模型、动画及第三方交互。

官方资料：

- [GeckoLib 4.4.9 Forge 1.20.1 与修复说明](https://modrinth.com/mod/geckolib/version/RBA7lJaW)
- [GeckoLib 4.8.4 Forge 1.20.1](https://modrinth.com/mod/geckolib/version/aC5KMoNg)
- [官方接入文档](https://github.com/bernie-g/geckolib/wiki/Installation-%28Geckolib4%29)

已从官方 Modrinth API 获取候选发行包并校验 SHA512；4.4.9、4.7、4.8.4 的 `META-INF/mods.toml` 均声明 Forge `[47.1,)`，且均内嵌 MCLib 20。本地证据在 `.local/geckolib-research/`，不提交 JAR、缓存、运行目录或 EULA。

## 开发约束

- Gradle 固定 `geckolib_version=4.4.9`，不使用 `+` 或浮动最新版。默认构建从基线 API 编译，避免无意引用只存在于新版本的方法。
- 发行元数据的 `geckolib_version_range` 独立于编译版本。不能仅修改运行下限来让缺少新 API 的版本通过检查。
- GeckoLib 是客户端与服务端都必需的外部 mod；不通过 shading、Jar-in-Jar 等方式打包第二份 GeckoLib。开发所需的 MCLib 单独声明，玩家使用 GeckoLib 正式包内嵌的 MCLib。
- 后续模型、动画 JSON、插值、Molang 表达式也要在基线验证。只编译 Java 无法证明新编辑器导出的资源能在旧前置中运行。
- 初次加入实际动画、模型渲染、护甲或动态方块时，重复基线与较新前置验证；涉及光影、渲染优化 mod 的表现由具体整合包验收覆盖。
- 需要提高下限时，先确认实际功能是否必需，再记录受影响整合包；不能为方便调用新 API 默认提高要求。

## 整合包边界

| 整合包 | 已确认平台 | 当前结论 |
| --- | --- | --- |
| ATM9 | 1.20.1 Forge | 属于当前目标；没有完成整个整合包的安装或客户端验收 |
| ATM10 | 1.21.1 | 不适用当前 Forge JAR；需按需接入 1.21.1 NeoForge 和对应 GeckoLib |
| 愚者 The Fool | BBSMC 作者页为 1.20.1 Forge | 下载为网盘，未获得实际 mod 清单；待用户提供具体发行包后核对，不能据名称宣布兼容 |

来源：[ATM9 官方页](https://www.curseforge.com/minecraft/modpacks/all-the-mods-9)、[ATM10 官方仓库](https://github.com/AllTheMods/ATM-10)、[愚者作者发布页](https://bbsmc.net/modpack/the-fool)。整合包名称不能替代具体发行版本的依赖清单。

后续核对一个整合包时，读取实际 Minecraft、加载器、GeckoLib 版本及相关 mod 的依赖范围。满足范围则先保留整合包原版本，同一实例只装一份 GeckoLib；版本不满足时先诊断，不擅自升级整个包。客户端与服务端建议采用相同前置版本。无公开清单的包等待用户提供实际包或日志；不以其他包的测试替代。

## 本次验证

构建、元数据校验与无头开发服务端已通过。同一个发行 JAR 的无头安装验证如下，全部使用新的项目测试世界、绑定 `127.0.0.1`；正常项均观察到 PinkkiCore 加载、Done 就绪和 stop 正常退出：

| Forge | GeckoLib | 结果 |
| --- | --- | --- |
| 47.1.0 | 4.4.9、4.8.4 | 两组均通过 |
| 47.4.28 | 4.4.9、4.7、4.8.2、4.8.4 | 四组均通过 |
| 47.4.28 | 未安装、4.4.8 | 两组均按预期明确拒绝，未进入服务器就绪状态 |

证据：`.local/geckolib-research/runtime-results.json`、各组合日志及 [变更记录](changes/2026-10-22-geckolib.md)。Forge 在依赖检查失败后也可能返回进程状态 0；反向场景以明确的依赖错误和未加载/未就绪状态判断，不能只看退出码。所有测试进程均已退出。

当前没有实际模型和动画，因此本轮最多证明依赖加载、初始化、服务端就绪与正常停服。未启动 Minecraft 客户端，未验证渲染、光影、NPC/Boss 战斗或整个第三方整合包。
