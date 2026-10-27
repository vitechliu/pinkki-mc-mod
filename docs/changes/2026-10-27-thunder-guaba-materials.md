# 2026-10-27：雷霆瓜巴二级装备材料

- 变更 ID：2026-10-27-thunder-guaba-materials
- 需求与完成行为：将现有雷霆瓜巴物品接入 Silent Gear 3.6.7、Tinkers' Construct 3.11.2.166、Tetra 6.17.0，保持三个模组可选安装。以钻石至下界合金范围定位二级材料，偏快攻速、快挖掘、轻盈，耐久低于同类高阶材料。游戏进度“二级材料”不等于宿主内部材料 tier 数字。
- 数值依据：直接核对项目缓存的真实发行 JAR。Silent Gear 钻石主材耐久 1561、挖掘速度 8、伤害 3；其下界合金为涂层而非独立主材。匠魂以高阶钴和玛玉灵作宿主基准，玛玉灵头耐久 1250、挖掘速度 6.5、伤害 3.5、下界合金采掘级。Tetra 钻石为 1561/8/primary 6，下界合金为 2031/9/primary 7.24。三套数值不是统一成品数值，不直接跨模组比较。
- Silent Gear：主材耐久 1200、采掘速度 10、伤害加成 4、攻速加成 0.4、下界合金采掘级。保留原材料支持的主部件/杆部件范围，杆部件耐久 −10%、采掘 +10%、攻速 +0.2。原结构可制作的护甲采用钻石防御档位，护甲耐久系数降至 25；轻盈和命中效果仅对工具生效。雷霆轻盈特性主手增加 5% 基础移速，max_level 1，多个材料部件不增加该特性等级。
- 匠魂：支持头、手柄、绑定结；每物品 1 材料单位，可直接部件制造，不新增熔炼/浇铸/盔甲/远程部件。头耐久 1200、采掘速度 10、伤害 3.5、下界合金采掘级；手柄耐久 −15%、攻速 +10%、挖掘 +10%。轻盈通过 flat 模块提供基础攻速 +15% 和主手基础移速 +5%，不随同材部件数量递增；手柄本身仍参与工具统计。
- Tetra：primary 7.2、secondary 2.0、tertiary 3.5、耐久 1200、效率 10、下界合金采掘级、integrityCost 2 / integrityGain 8 / magicCapacity 90。每模块额外攻速 +0.2、总移速 +5%，沿用原生属性合并；攻速主手生效，移动属性主副手均生效。使用 wood 接受族使刀刃、握柄等原有接受木材的模块可用，外观使用现有 shiny/metal/default 模型并染为冰蓝色，不改原版木材标签或替换宿主模块定义；加工要求钻石级 axe_dig。
- Tetra 实证修正：原生 collapseRound 会把没有加法基数的 MULTIPLY_BASE 属性折算为零，因此移动属性使用 MULTIPLY_TOTAL（`**generic.movement_speed`）；不以只写 JSON 就宣称生效。也不擅自覆盖宿主对副手移动属性的允许规则。
- 命中特性：复用已经存在的 `pinkkicore:thunder_guaba` MobEffect，不新建药水或修改酿造/落雷行为。沿用瓜巴之触的三条原生触发链：Silent Gear target_effect、匠魂 mob_effect 的 melee_hit、Tetra apply_hit_effects/apply_effect。固定 I / 200 tick，概率 1，重复命中刷新、多个部件不提高等级、保留更强或更长的现有效果；普通瓜巴状态可共存，刷新不推迟已有落雷倒计时。真实落雷有附近实体/地面影响，不额外绕开既有规则。
- 源码位置：1.20.1 `data/pinkkicore/{silentgear_materials,silentgear_traits,tinkering/materials,tinkering/modifiers,recipes/compat/tconstruct}`、`data/tetra/{materials/wood,item_effects}`；客户端材料外观及双语名称说明在目标 assets。生产功能仅新增资源，不引入必选依赖或跨客户端边界的 Java 引用。
- Git 参照：开始时 HEAD `1d64afb`。
- 验收场景：无宿主、各宿主单独、三个宿主一起均能启动；用雷霆瓜巴实际组装对应工具、验证原始瓜巴和普通宿主材料仍可用；头/柄/绑定结及多部件组合实际近战施加雷霆瓜巴 I，刷新、不覆盖更强效果、只加给目标、与普通状态共存。对比宿主高阶材料的实际工具攻速/采掘/耐久与主副手移速属性。客户端另查部件制作、模型、文案及工作台限制。
- 手册与 Wiki：同步 `wiki/compatibility.md`、`wiki/items.md` 和 `wiki/changelog.md`；构建从共享 Wiki 源生成离线手册。未新建配方图：这里使用宿主原有的部件制作页面，具体工具和部件消耗由各宿主展示，不伪造统一合成格。
- 验证记录：`./scripts/mod.sh 1.20.1 build` 与 `python3 scripts/verify-artifact.py 1.20.1` 通过；`python3 -m unittest discover -s scripts/tests -p test_guide.py` 的 4 项测试通过；`npm run wiki:build` 成功，仅既有 mcfunction 高亮回退提示。`git diff --check` 通过。
- 无头发行矩阵：`python3 scripts/check-guaba-compat.py --libraries .local/package-test/1.20.1/libraries --dependencies .local/guaba-compat-tests/1792981596564979000 --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar --java-home .local/jdk-17/Contents/Home` 五组全部通过。结果 `.local/guaba-compat-tests/1793070822894793000/matrix-results.json`，每组独立 `runtime/<case>/console.log`；所有服务端回环监听、--nogui，均正常停服且 exit_code 0，没有 ERROR。验证实际装备组装与 Player.attack，控制组、原瓜巴回归、新材料多部件、固定等级/时间、状态共存、更强效果保留、连续命中不延期落雷，以及实装后的速度/耐久差异和持用槽位。探针提高靶子生命，避免高伤害先杀死目标使后续刷新测试失真。
- 完整部署：`python3 scripts/deploy-local.py` 成功；`.local/hmcl/deployments/20261027-111525-649534/receipt.json` 为 installed。clean、compat 实际安装 JAR 与最终发行产物 SHA256 均为 `97e63b091b80a2cafae3f291f377e250c7928ba42648799b3c932bea6363383f`，已逐一重新计算核对。最终包相对无头矩阵测试包（SHA256 `34c0c861bf182e84b1f128b8d3685fde271c9874ab6945f4660103bb2bc9a054`）仅 `assets/pinkkicore/guide/zh_cn.json` 改变，ZIP 条目集合及其他每个条目的字节内容一致，因此材料、特性及代码均为已测试版本；差异来自补齐道具页的手册链接。
- 热同步：`python3 scripts/hot-sync.py` 成功；`.local/hmcl/hot-sync/20261027-111555-300019/receipt.json` 为 ok，两个实例资源与已有测试世界数据均更新。资源和数据可分别用 F3+T、/reload 重载；安装新 JAR 后建议完整退出重启，正式 JAR 客户端验收前可按项目流程移除受管覆盖包。
- 未验证：未启动 Minecraft 图形客户端，未验证部件制作界面、材质外观、工作台操作体验、多人显示或任意其他版本。未推送 Git、发布 Release 或部署线上 Wiki。既有雷霆瓜巴 MobEffect 的真实落雷实现未改动，本次命中特性探针验证状态和落雷时钟，不重复宣称新的落雷实机证明。
- 移植注意：仅 1.20.1。次要版本需核对宿主实际版本和数据序列化器，不能套用最新分支格式；不推进移植检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 按需核对三种宿主版本与资源格式 |
| 1.19.1 Forge | 待适配/验证 | 按精确 1.19.1 依赖与格式适配 |
