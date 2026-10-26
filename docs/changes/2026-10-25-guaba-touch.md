# 2026-10-25：瓜巴材料命中附加既有瓜巴状态

- 变更 ID：2026-10-25-guaba-touch
- 需求与完成行为：Silent Gear 主材/杆、匠魂工具头/手柄/绑定结、Tetra 瓜巴模块增加“瓜巴之触”。通过三个模组自己的特性/状态模块施加既有 `pinkkicore:guaba`，固定 I（amplifier 0）、200 tick（10 秒），重复命中刷新，多部件不增加输出等级/时间；不削弱已有更强、更长的状态。没有修改瓜巴药水本体或酿造，也不增加强制依赖、反射、全局攻击监听器或第二个状态实现。基础材料数值保持不变。
- 源码位置：1.20.1 资源下 `data/pinkkicore/silentgear_traits/guaba_touch.json`、`silentgear_materials/guaba.json`、`tinkering/modifiers/guaba_touch.json`、`tinkering/materials/traits/guaba.json`，`data/tetra/item_effects/pinkkicore_guaba_touch.json`、`materials/wood/pinkkicore_guaba.json`；双语语言资源。验证更新 `scripts/tests/compat_probe/GuabaCompatProbe.java`、`scripts/check-guaba-compat.py` 和 `scripts/verify-artifact.py`。
- Git 参照：开始时 HEAD `d76ae85`。期间工作区出现另一项 Pinkki 实体、渲染、注册和探针开发，保留其修改；本需求未改动这些 Java 实现。构建基于当时完整工作区，测试使用冻结的同一发行包快照。
- 原生参考：Silent Gear 3.6.7 Venom 使用 `silentgear:target_effect`（duration 单位秒）；匠魂 3.11.2.166 Spectral/Venom 使用 `tconstruct:mob_effect`（time 单位 tick、level 从 1 开始）；Tetra 6.17.0 的数据命中链与 Infested 的 `tetra:apply_effect`。匠魂新源码中的 `weapon_mob_effect` 在固定发行版不可用，曾被实际加载测试发现后修正，未采用新分支格式冒充发行兼容。
- 触发边界：寂静装备沿用原生玩家工具 `LivingAttackEvent` 特性阶段；匠魂模块只挂 `tconstruct:melee_hit`（未增加反击/投射物 hooks），固定概率 1，该发行包正常未满蓄力的近战也会触发；Tetra 沿用原生 `tetra:apply_hit_effects`，可被其他 Tetra 战斗机制调用，不额外限制或新增远程/反击链。实际无头行为覆盖普通近战；取消伤害、免疫及拓展攻击链以宿主原生机制为准。瓜巴本身是随机速度/护甲 buff，因此给敌人施加也可能强化敌人。
- 验收场景：分别使用全普通材料、单个瓜巴头/柄/杆/绑定结或多瓜巴部件装备攻击测试生物；普通材料无瓜巴状态，瓜巴装备命中只有目标得到 I/200 tick。将目标现有状态设为 I/40 tick 后再命中刷新至 I/200 tick；II/600 tick 不被覆盖。自身体上不因攻击获得状态；不安装对应模组仍能加载。客户端另行检查特性文字、装备制造和战斗显示。
- 1.20.1 构建与校验：`./scripts/mod.sh 1.20.1 build --console=plain`、`python3 scripts/verify-artifact.py 1.20.1` 通过，日志 `.local/compat-research/guaba-touch-build.log`、`guaba-touch-verify.log`。`npm run wiki:build` 通过，日志 `guaba-touch-wiki.log`；本次相关文件的 `git diff --check`、Python 语法和 JSON 解析通过。工作区其他并行开发文件的格式不纳入本需求结论。
- 无头攻击矩阵：Silent Gear 3.6.7 + Silent Lib 8.0.0、匠魂 3.11.2.166 + Mantle 1.11.104、Tetra 6.17.0 + mutil 6.3.0；Forge 47.4.28 + GeckoLib 4.4.9。缺失全部可选模组、三者分别安装、三者共存，共五组通过；使用新隔离世界和回环端口 25579。证据 `.local/guaba-compat-tests/1792920113804879000/matrix-results.json` 与每组 `runtime/<组合>/console.log`，汇总 `.local/compat-research/guaba-touch-matrix-final.log`；本轮首次全部通过的包 SHA256 `9b7d7c6f7ff88717db2a473f96e73e998501bb4f5ea50e6f7e1b3d3168b30ba6`。探针由 FakePlayer 调用正常 `Player.attack`，没有启动客户端。
- 完整部署：`python3 scripts/deploy-local.py` 成功；回执 `.local/hmcl/deployments/20261025-172400-130755/receipt.json` 的 status 为 `installed`，clean/compat 两个实例中的 PinkkiCore JAR 均与此回执和发行包 SHA256 一致：`e91588fc12fd258b602296187c1fb9115faa3e38d241813f3504128eaf4c1e5b`。部署脚本重建包含了并行 Pinkki 改动，因此对这个最终部署包另做五组复验，五组全部通过，证据 `.local/guaba-compat-tests/1792920289836497000/matrix-results.json`、各组 `runtime/<组合>/console.log` 及 `.local/compat-research/guaba-touch-deployed-matrix.log`；这五组无 ERROR，进程正常退出。部署没有启动游戏，保留其他模组、配置和存档。
- 热同步：`python3 scripts/hot-sync.py` 成功；回执 `.local/hmcl/hot-sync/20261025-172419-626312/receipt.json` 的 status 为 `ok`。两个实例资源包与各自唯一已发现测试世界的数据包共四份覆盖包均已同步。需玩家自行 F3+T、`/reload`，未自动启用或操作游戏内重载；正式 JAR 客户端验收前仍须 `hot-sync.py remove` 移除受管覆盖包。
- 未验证范围：客户端文字显示与实机战斗、多人效果同步、取消/免疫伤害和 Tetra 扩展攻击链未实测；未发布 Release、未推送或部署 Wiki。
- 测试可靠性：矩阵先冻结发行 JAR，避免并行构建改变不同组合的输入；已知 Yggdrasil 后台公钥下载失败单独记为 `auth_key_unavailable`，其他 ERROR/探针失败仍失败。离线测试不证明正版联机认证。
- 手册与 Wiki：更新兼容条目、瓜巴药水关联入口与玩家更新记录；开发实现/参数/日志置于 `docs/guaba-material-compatibility.md` 与本记录。自研游戏内手册仍未实现，明确保留 `guaba` 条目的材料特性说明接入待办；不将宿主模组自身手册或 Wiki 当作完成本项目手册。
- 移植注意：只改 1.20.1 适配资源；次要目标需核对宿主可用发行版本、trait/modifier/effect 格式和事件语义，不推进检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 按需核对三种宿主及状态接入格式 |
| 1.19.1 Forge | 待适配/验证 | 按精确 1.19.1 可用宿主核对，不以 1.19.2 替代 |

## 并行部署的最终核对

本任务完成部署与五组验证后，另一个并行 Pinkki 开发任务再次通过完整部署脚本更新了两个实例。最新回执 `.local/hmcl/deployments/20261025-172645-933861/receipt.json` 为 `installed`，两实例与发行包 SHA256 均为 `983e70471d931aebf53027163045d48d6c912581aa7fffa8dc838bfd8213d812`。逐项比较确认瓜巴之触的全部材料/特性/状态模块定义及双语文本与前述五组验证包一致；不回滚或覆盖并行任务的改动。当前包额外执行三模组共存实际攻击复验，证据 `.local/compat-research/guaba-touch-latest-all.log` 与 `.local/guaba-compat-tests/1792920456234931000/matrix-results.json`，共存攻击验证通过、无 ERROR、正常停服。最后再次执行热同步，回执 `.local/hmcl/hot-sync/20261025-172806-049054/receipt.json` 的 status 为 `ok`。
