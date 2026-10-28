# 2026-10-26：瓜巴充电桩

- 变更 ID：2026-10-26-guaba-charging-station
- 需求与完成行为：1 格工作方块、整体高 12.8/16 格，附魔台座风格、三向铜针、雷霆瓜巴嵌块、蓝白充能电纹和原版电火花粒子。右键双槽界面，没有瓜巴燃料槽。每次一对一、20 tick、无额外燃料/FE/真实雷击需求。
- 加工：`pinkkicore:guaba_charging` 独立数据包配方，瓜巴→雷霆瓜巴、原版苦力怕刷怪蛋→闪电苦力怕刷怪蛋。支持 RecipeManager /reload 和网络同步；原工作台三槽及既有配方契约保持原样。输出堵塞/红石禁止时暂停；输入变化或失去配方重置进度；库存、进度、当前配方、红石和六面持久化。六面仅禁用/输入/输出/双向四态，无催化剂模式，默认底面输出、其余面输入。
- 合成：有序九宫格 ` L ` / `LEL` / `TTT`；L=避雷针、E=附魔台、T=雷霆瓜巴，产出 1 充电桩，获得雷霆瓜巴解锁配方书。
- 闪电苦力怕刷怪蛋：独立 Item，以原版 Creeper 的 `powered` NBT 生成带电苦力怕。右键方块、液体使用及原版发射器行为接入；不注册替代 Creeper 类型、不改原版/Forge 的 Creeper→刷怪蛋映射。对刷怪笼右键只生成，不改刷怪笼类型；该限制已写入玩家说明。不存在新生物渲染前置。
- 代码：1.20.1 `charging/ChargingContent.java`、`GuabaChargingBlock/Entity/Menu/Recipe.java`；`item/ChargedCreeperEggItem.java`；客户端 `GuabaChargingScreen.java` 与 JEI `GuabaChargingCategory.java`，客户端注册/手册配方绘制同步；资源位于同版本 blockstates/models/textures、recipes/guaba_charging、合成/解锁/战利品 JSON。
- 模型：`designs/guaba-charging-station/` 的确定性生成器、64px atlas、Java 模型、内嵌贴图 .bbmodel、实际导出驱动的离线 3D 审阅页。静态尺寸和碰撞均不高于 12.8；粒子不生成会影响世界的闪电。
- 玩家文档：Wiki 道具/配方图/更新记录同步，CraftingRecipe 扩展有序九格与空格展示；充电桩组件直接导入真实配方 JSON。手册来源编译器识别该组件，离线正文、物品图和当前 RecipeManager 的加工图均同步。JEI 新增独立类别、机器催化标记、箭头跳转和额外 UI 区域避让。
- Git 参照：实现期间 HEAD `c451415`；保留工作区其他瓜巴/雷霆药水、手册和 JEI 修改，未推进其他版本检查点。
- 验收场景：精确九格合成（两个空角必须空）、两种材料各 19/20 tick、连续转换、无其他材料消费、未知输入/堵塞、全部红石与六面模式、存读进度、shift 路由/输出限制/距离、配方网络；右键和发射器实际生成带电苦力怕；UI 居中、JEI 避让、手册/拉近模型电火花由用户人工检查。
- 构建与发行校验：`./scripts/mod.sh 1.20.1 build workbenchProbeJar -PworkbenchProbe --console=plain` 通过（`.local/guaba-charging-build.log`）；`python3 scripts/verify-artifact.py 1.20.1` 通过，SHA256 `affc4d90235e6719edccc6b02c91473881c7a01aee121c9ab4aef3652dd8fd5c`。模型静态验证、四项手册生成测试、Wiki 构建（`.local/guaba-charging-wiki.log`）与 `git diff --check` 通过。
- 无头验证：`.local/workbench-tests/1793019584661825000/result.json` 为 passed，服务端正常就绪停服、退出码 0、无 ERROR；日志 `.local/guaba-charging-probe.log`。覆盖既有工作台/剑/弓回归及充电桩两条配方、19/20 tick、连续转换、堵塞/未知输入、4×2 红石真值表、6×4 六面、模拟无副作用、槽位旁路限制、38 槽菜单、shift 输入/输出限制/权限、NBT 进度恢复、配方网络、精确有序九格/空角拒绝；实际右键和原版发射器均生成 powered Creeper，消耗正确且原版刷怪蛋映射未覆盖。没有以 JSON powered 字段代替实际生成验证；液体右键未单独无头验证。
- 可选 JEI 静态链接：`python3 scripts/check-jei-linkage.py --api .local/jei-research/jei-1.20.1-forge-15.20.0.129.jar` 通过，7 个隔离插件类、41 个字段/方法符号解析；不代表界面/JER 实机共存验证。
- 模型审阅：本地实际浏览器检查贴图、几何、多方向按钮、拖动和缩放，截图 `.local/guaba-charging-review/iso.png`。Blockbench 网页版导入首次被自动审批拒绝后，用户明确回复“授权导入”；已完成官方网页编辑器实际导入，核对 30 部件、64×64 内嵌贴图、底座变换参数与六面 UV，三维外观正常。证据 `.local/guaba-charging-review/blockbench.png`、`blockbench-uv.png`；编辑器标签保留。文件选择器遇到临时 input 被移除的问题，改用技能记录的官方 fragment 导入并重新加载后成功，未修改模型。此次仅补充编辑器验收和开发文档，不重新构建或部署。
- 默认部署：`.local/hmcl/deployments/20261026-210227-601519/receipt.json` 为 installed；clean、compat 实际 mods JAR SHA256 均与上述无头验证产物完全一致。热同步 `.local/hmcl/hot-sync/20261026-210244-334918/receipt.json` 为 ok，两个实例受管资源和全部已发现测试世界数据包已同步；未在客户端执行 F3+T 或 /reload。
- 未验证范围：未启动 Minecraft 图形客户端，UI/字体、JEI/JER 实际交互、模型在游戏内的碰撞/手持姿态、电火花观感尚待用户验收。远端 Wiki 未发布。
- 移植注意：注册、BlockEntity capability 生命周期、菜单 ContainerData、RecipeSerializer 网络格式、SpawnEgg 使用/发射器、粒子及 JEI API 均须核对目标版本；common 未加入 Minecraft 依赖。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 未构建或部署 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 工作区整理复验与完整部署

- 本次统一复验充电桩、带电刷怪蛋、普通瓜巴对称波动、雷霆瓜巴药水、JEI 雷击页面和瓜巴手册展示调整。1.20.1 `build workbenchProbeJar guabaEffectProbeJar guideProbeJar -PworkbenchProbe -PguabaEffectProbe -PguideProbe --console=plain`、发行 JAR 校验、37 项 Python 测试、Wiki 构建均通过；日志 `.local/tidy-final-20261026-{build,tests,wiki}.log`。本次发行 SHA256 为 `e0afe364d3f0c7126a1b93e41bca4eeaa71c0a47d131bcce777ae1738043da27`。
- 工作台/充电桩/带电刷怪蛋及原有武器回归通过：`.local/workbench-tests/1793023557151556000/result.json`；手册注册、2×2 合成、服务端使用、入包 Wiki、中文检索及当前配方引用通过：`.local/guide-tests/1793023565263963000/result.json`；普通/雷霆状态、18 条酿造配方、四级正负采样、共存和时钟、真实雷击间隔/伤害与移除/过期回归通过：`.local/guaba-effect-tests/1793023612438681000/result.json`。三组均正常就绪与停服、exit_code=0、errors=false；日志 `.local/tidy-final-20261026-{workbench,guide,effects}.log`。
- 模型验证通过：30 部件、16×12.8×16 尺寸、UV、工程/导出/审阅页/运行资源一致；JEI 15.20.0.129 静态链接检查通过：7 个隔离插件类、41 个字段/方法符号。日志 `.local/tidy-final-20261026-{model,jei}.log`。Python AST、JSON、11 份 PNG 的 CRC、凭证/本机路径扫描及差异空白检查通过；保留模型工程、设计源与离线审阅页，忽略规则无需修改。
- 完整部署成功，`.local/hmcl/deployments/20261026-220741-758645/receipt.json` 为 installed，clean、compat 两个实际安装 JAR 的 SHA256 经独立重算，与发行包及三组测试输入完全一致。旧 JAR 由脚本备份，其他模组、配置和存档保留。日志 `.local/tidy-final-20261026-deploy.log`。
- 热同步成功，`.local/hmcl/hot-sync/20261026-220752-306914/receipt.json` 为 ok；两个实例资源包与所有已有测试世界数据包均已处理，日志 `.local/tidy-final-20261026-hot-sync.log`。玩家通过 F3+T、`/reload` 重载资源/数据；新增注册和 Java 行为需从新 JAR 启动游戏进程。正式发行包验收前按流程移除受管覆盖包。
- 修正本轮对称波动记录中仍称手册未实现的过时说明，当前正文/配方与 Wiki 同源并随 JAR 发行。未启动 Minecraft 图形客户端，客户端界面、JEI/JER 交互、手册排版及真实整合包行为未验收；远端 CI/Pages 未验证，未发布标签、Release 或真实服务端。

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
