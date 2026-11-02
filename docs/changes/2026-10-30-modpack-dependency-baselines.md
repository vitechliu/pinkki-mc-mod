# 2026-10-30：按整合包清单调整三个目标的依赖基线

- 变更 ID：2026-10-30-modpack-dependency-baselines
- 需求与完成行为：检索 CurseForge、Modrinth、FTB 热门目标包的固定版本，保存 31 份快照（28 份版本门槛通过，3 份排除；另保留 7 项未取得有效清单的记录）。降低不必要的最低前置版本，并以较低 API 编译；各主版本上限不变。完整来源、表格和异常见[调研报告](../compatibility/modpack-dependency-research-2026-10-30.md)。
- Git 参照：开始时 HEAD `d6b904154d8c64b1241a1b1f837cd0ec1a9d224b`；保留此前未提交的移植工作。
- 源码位置：三个目标的 `gradle.properties` / `build.gradle`、NeoForge TOML 模板、`gradle/forge.gradle`、`scripts/verify-artifact.py`；部署前置配置 `config/hmcl-runtime.json` 和 `scripts/deploy-local.py` 同步，保留已有兼容前置。`scripts/check-dependency-baselines.py` 与样本清单加入构建工作流离线检查。
- 验收场景：旧版依赖通过编译和发行加载；Curios 槽位、免疫 I—IV、卸下及重复属性；饼蛙攻击延迟、视线和溺水规则；JEI 所引用字段/方法在真实旧 API 中存在；缺少可选依赖不产生版本冲突；错误游戏/加载器、重复分支清单不当作正常样本。
- 1.20.1 验证：`build accessoryProbeJar -PaccessoryProbe`、`build bingwaProbeJar -PbingwaProbe`、发行校验通过；GeckoLib 4.2.3 + Curios 5.3.5 的饰品探针通过，GeckoLib 4.2.3 饼蛙探针通过，均就绪、正常停服、退出 0。JEI 15.2.0.27 的 41 个接口链接通过。日志在 `.local/modpack-research-20261030/` 的 `build-final-1.20.1.log`、`artifact-final-1.20.1.log`、`accessories-1.20.1-floor.log`、`bingwa-floor.log`。
- 离线回归：全部 44 个脚本测试通过（含新门槛检查、低版 Curios 部署保护），28 个有效样本无数值门槛冲突；`all-script-tests.log`、`audit-final.log`。
- 本地收尾：`deploy-local.py` 已安装两个项目实例，回执 `.local/hmcl/deployments/20261030-110107-128429/receipt.json` 为 installed，两个 SHA256 均为 `c6ca4d8dd074893c0810fd6bb2ecfd3a18880d6a67c94e1d1baa1ac64b652adb`。已有 GeckoLib 4.4.9 被保留。热同步回执 `.local/hmcl/hot-sync/20261030-110135-064523/`；未操作游戏或执行重载。首次部署因旧前置配置被脚本拒绝，修正配置后重试成功；首次热同步遇部署锁，部署结束后重试成功。
- 文档：README、Wiki 兼容页和更新记录同步；离线手册随构建生成；`npm run wiki:build` 通过。没有新增玩法或配方。
- 移植注意：本次用户明确限定三个版本的依赖研究与调整，不借机补齐其他玩法积压，不推进完整适配检查点。GeckoLib 3 与 4、Forge 与 NeoForge、Minecraft 精确版本仍隔离；没有反射跨 API 或内嵌第二份前置。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 部分完成 | NeoForge 21.1.72 + GeckoLib 4.6.6 + Curios 9.2.2 编译、发行元数据和完整行为探针通过，但保存后 JVM 驻留；宿主对照无 PinkkiCore 也复现，超时清理，不能宣称正常停服通过。相同旧库加 JEI 19.21.0.247 在 NeoForge 21.1.249 上完整探针、无 ERROR、正常退出 0。`runtime-neo249-low-libs.log`，JEI 41 个符号链接通过。 |
| 1.19.1 Forge | 部分完成 | 保持 GeckoLib 3.1.30、Curios 1.19.2-5.1.6.4、JEI 11.2.0.244；仅参数化原范围。构建、真实 JAR 元数据和饰品探针通过且正常退出，JEI 41 个符号链接通过。完整发行校验仍失败于既有缺少 WildlifeSpawns 类，未补齐该玩法或推进检查点；`artifact-final-1.19.1.log`、`accessory-final-1.19.1.log`。 |

旧 Neo72 的探针与宿主对照均已超时清理，确认相关测试 JVM 退出。

本次不启动客户端；完整整合包、GUI、动画渲染与其他 mod 交互未验。CI 仅完成配置和本地相同检查，未触发 GitHub 实际执行。未提交、推送、发布或部署真实服务器。

## 2026-11-02 整理提交复验

- 本轮默认只重新构建与运行 1.20.1。`build pinkkiProbeJar accessoryProbeJar -PpinkkiProbe -PaccessoryProbe`、`bingwaProbeJar -PbingwaProbe`、发行 JAR 校验、44 项 Python 测试、依赖样本离线审计及 Wiki 构建通过。JEI 15.2.0.27 的 41 个接口链接通过。日志为 `.local/tidy-20261102-{build,tests,baselines,jei,wiki}.log`。
- 当前发行 JAR SHA256 为 `c6ca4d8dd074893c0810fd6bb2ecfd3a18880d6a67c94e1d1baa1ac64b652adb`。GeckoLib 4.2.3 + Curios 5.3.5 饰品探针、GeckoLib 4.2.3 瓜巴影融探针通过，均正常停服、退出 0；见 `.local/tidy-20261102-accessories.log`、`npc.log`（同前缀）。
- 原饼蛙探针在 GeckoLib 4.2.3 下两次未通过：已自动锁定目标，但挑衅击退后距离约 2.6 格，超出 2.5 格攻击范围；GeckoLib 4.4.9 对照通过，不能据此单独认定依赖回归。探针现在仅在挑衅那一击临时设置抗击退并立即恢复，隔离击退位移与寻路对反击断言的影响，未修改游戏实体。调整后最低版本探针通过自动反击、六 tick 延迟伤害、超距拒绝、隔墙拒绝与水下呼吸，服务端正常退出；保留 `.local/tidy-20261102-bingwa{,-repeat,-control,-fixture}.log`。本轮不据此声称验证了击退后的追击寻路。
- `deploy-local.py --with-curios` 已完整安装 clean、compat，回执 `.local/hmcl/deployments/20261102-144816-475232/receipt.json` 为 `installed`；独立读取两个实例 JAR 重新计算 SHA256，均与上述发行产物一致。保留已有 GeckoLib 4.4.9、Curios 5.14.1。热同步记录 `.local/hmcl/hot-sync/20261102-145815-588813/`，覆盖两个实例的资源及各自现有测试世界数据；未执行游戏内重载。
- 1.21.1 本轮仅检查源文件映射、现代数据目录及共享资产一致性；未重新编译或运行两个次要目标，也未推进其已失效的完整适配检查点。上表的历史阻碍仍待对应版本任务处理。
- 瓜巴龙未接入发行包；设计校验和真实 GeckoLib 4.2.3 解析通过，Blockbench 编辑器导入及动作验收仍待完成。项目发布 skill 格式校验通过，不代表触发了发布。
- 本轮修正 README 对次要目标适配完成度的表述；探针调整不改变玩家行为，无需增加手册/Wiki 玩法条目。所有构建、探针运行、回执及缓存留在忽略目录。本轮没有客户端、远端 CI、Pages 或完整整合包验收结论。
