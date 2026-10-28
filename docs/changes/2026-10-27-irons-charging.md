# 2026-10-27：瓜巴充电桩与铁魔法联动

- 变更 ID：2026-10-27-irons-charging
- 需求与行为：同时安装 `irons_spellbooks` 时，瓜巴充电桩新增玻璃瓶→雷电瓶、空白符文→雷霆符文。均为 1→1、20 tick（约 1 秒），无额外瓜巴或能量消耗。保持原充电桩的红石、六面、输出堵塞保护；不修改铁魔法原有获取途径。
- 注册 ID 核对：官方源码 `1.20.1-legacy` 快照 `b928995887aa362467fa999a9c9fb86419f00f2d` 的 `registries/ItemRegistry.java` 注册 `lightning_bottle`、`blank_rune`、`lightning_rune`，zh_cn 对应雷电瓶、空白符文、雷霆符文；实际发行 JAR 的注册表也已验证。
- 实现路径：`versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_charging/compat/irons_spellbooks/glass_bottle_to_lightning_bottle.json`、`blank_rune_to_lightning_rune.json`。使用现有 `pinkkicore:guaba_charging` 类型，两条 JSON 均有 `forge:mod_loaded` 条件；Forge 在物品解析前跳过缺少模组的配方，没有新增运行代码、编译依赖或强制前置。
- 展示：现有 JEI 类别从当前 RecipeManager 枚举配方，因此安装铁魔法后自动展示，不新增并行类别或手写另一套配方。Wiki 配方组件直接导入 JSON；道具说明、兼容页和玩家更新记录同步。现有手册生成器直接纳入同份正文/配方引用，已构建入 JAR；游戏内由已安装模组提供真实物品图标。Wiki 引用作者固定源码快照的远程图标，不复制铁魔法贴图进发行包或仓库；雷电瓶动画条采用首帧裁切显示。Wiki 图标需要联网，手册随安装的模组离线渲染。
- Git 参照：工作开始时 HEAD `1d64afb`；保留原有雷霆瓜巴材料兼容等未提交修改。
- 验收场景：仅装 PinkkiCore 时没有这两条配方、玻璃瓶不被消耗；同时安装铁魔法时，两种材料在第 19 tick 无产物，第 20 tick 精确消耗 1 个并产出 1 个目标，连续转换正确，输出满/放错产物不吞材料；JEI/手册可查询。配方随原生资源重载流程加载，不修改真实存档。

## 1.20.1 验证

- `./scripts/mod.sh 1.20.1 build workbenchProbeJar -PworkbenchProbe --console=plain` 通过，日志 `.local/irons-charging-build.log`。
- `python3 scripts/verify-artifact.py 1.20.1` 通过；校验加载条件、准确 ID/数量、无铁魔法硬依赖、无捆绑第三方代码/资产。JAR SHA256 `1ebf838ab19e332bee87a260f2354ef0885bba562d3f218d3ba3592a28bba57e`。
- 未安装铁魔法：`.local/workbench-tests/1793072801892919000/result.json` passed，日志 `.local/irons-charging-absent.log`；两条配方安全跳过，玻璃瓶不消耗，既有工作台/剑/弓/充电桩回归通过。
- 实际安装铁魔法：`.local/workbench-tests/1793072838903502000/result.json` passed，日志 `.local/irons-charging-present.log`。使用官方 Modrinth [1.20.1-3.4.0.11](https://modrinth.com/mod/irons-spells-n-spellbooks/version/K26TSnWZ)，前置 GeckoLib 4.7.1.1、Player Animator 1.0.2-rc1+1.20、Curios 5.14.1+1.20.1、Caelus 3.2.0+1.20.1；均只安装到一次性无头服务端，下载和官方 SHA512 记录见 `.local/irons-charging-research/`。验证真实注册物品、2→4 条配方、19/20 tick、1→1、连续转换及堵塞保护。两次测试均正常就绪/停服、退出码 0、无 ERROR。
- 四项手册生成测试通过；Wiki 构建通过（`.local/irons-charging-wiki.log`）。未启动 Minecraft 图形客户端；JEI/JER 实际交互、客户端手册/远程图标显示尚未实机验收。未发布远端 Wiki。
- 完整部署：`.local/hmcl/deployments/20261027-114826-850423/receipt.json` 为 installed，clean、compat 实际安装 JAR 的 SHA256 均与两次无头测试产物一致。未额外将铁魔法及其前置装入 HMCL；两个实例当前未安装铁魔法，需同时安装后才加载新配方。热同步 `.local/hmcl/hot-sync/20261027-114846-121059/receipt.json` 为 ok，两个实例受管资源与全部已有测试世界数据包已同步；未执行游戏内 F3+T 或 /reload。

## 移植注意

只有 1.20.1 Forge 已实现和验证。NeoForge 的条件配方语法、铁魔法对应版本 ID 及加载器需分别核对；不能把该 Forge 条件资源直接推定可用。主版本数据包条件不改变 mod 元数据依赖关系。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 未构建或部署 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；保留条件配方且缺少宿主时跳过，无明确 1.19.1 宿主，安装联动不适用；客户端未验 |

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。保留条件配方且缺少宿主时跳过，无明确 1.19.1 宿主，安装联动不适用。
