# 2026-11-21 · 瓜巴床与梦系材料

- 变更 ID `2026-11-21-guaba-dreams`。开始时 HEAD 为 `95b95d7`。
- 主世界按床头方块的 Y=192～196 判定云梦，至少睡 100 tick 后正常醒来结算 1 个。瓜巴床保留双格床的放置、占用和拆卸。在下界、末地及原版床受限的维度可短睡 60 tick、设置当地重生点，床边没有可用重生位置时回退到世界出生点。下界短睡产出冥梦，断线、拆床、死亡、提前起床和换维度中断奖励。
- 瓜巴床接受原版各色床和加入床标签的模组床，可无序合成或用瓜巴工作台转换。两个瓜巴版梦系材料与普通瓜巴无序合成。睡眠及装备效果实现在主版本的 `forge/dream/`，配方和材料沿用当前 Forge 与各宿主的数据目录。
- 云梦系提供一次 +8% 移速。冥梦系在下界为主手武器提供 +2 攻击，每件有效护甲提供 +2 护甲。瓜巴版增加基础耐久、主材攻击，以及具有胸甲材料属性的宿主胸甲护甲，保留瓜巴之触。匠魂护甲板仅为四种梦系材料添加专属部件制造台配方，保留原宿主其他材料的制作路径。
- 玩家条目、配方图与兼容说明共用 Wiki 来源生成离线手册。主版本限定段落按目标版本筛选，其他版本的手册生成可以继续使用同一套 Wiki。美术工程、生成方式和独立审阅见[床模型设计](../../designs/guaba-bed/README.md)。

## 验收

- 用 1.20.1 发行 JAR 在隔离 Forge 服务端测试高度两端及相邻高度、断线和换维度中断、下界与末地短睡、背包满时的归属掉落、两半分别拆除、真实死亡重生与原版下界床。验证命令为 `./scripts/mod.sh 1.20.1 build dreamProbeJar -PdreamProbe`，再用 `scripts/check-guaba-compat.py --dream-probe versions/1.20.1/build/libs/dream-probe.jar` 运行。依赖、Java 与服务端库参数沿用[兼容探针说明](../guaba-material-compatibility.md#可复现的无头检查)。`--current-tinkers` 可附加 compat 当前的匠魂与 Mantle 版本。
- 用 `npm ci` 和 `npm run wiki:build` 检查 Wiki，用 `python3 -m unittest discover -s scripts/tests -p test_guide.py` 检查手册。独立审阅器可旋转和缩放，Blockbench 可离线导入工程。贴图、模型和 UV 对照用 `python3 designs/guaba-bed/validate.py` 检查。游戏内外观及睡眠界面留待客户端手动验收。
- 手动验收可取瓜巴床分别放在主世界云层、下界与末地。确认床的四向模型、条纹和枕头位置，检查床头、床尾的交互与醒后奖励，换一套含梦系材料的装备查看效果。

## 1.20.1 验证记录

- Forge 47.4.28 与 GeckoLib 4.4.9 的六组隔离无头服务端均通过，分别为无可选宿主、寂静装备 3.6.7、匠魂 3.11.2.166、Tetra 6.17.0、三者共存，以及三者共存并使用 compat 当前的匠魂 3.12.1.231 与 Mantle 1.11.117。各组达到服务端就绪并正常停服。日志和依赖 SHA256 见 `.local/guaba-compat-tests/1795254550137959000/matrix-results.json`。测试所用发行包 SHA256 为 `efeed4e1c85a12cb4f91153028e9d985d3ce8ff462abb76399479a2be5918188`。
- 最终构建与矩阵版本逐文件对比，仅中英文语言文件有差异。Tetra 中英文分类、材料名和装备名前缀检查通过。最终包校验及对照结果记录于 `.local/dream-artifact-delivery.log`。
- 最终包 SHA256 为 `b985ab51a8ed197cddca040c37f123808045d6644e81b84527c543a4ef895725`，三宿主与 compat 当前匠魂版本的复测通过，记录在 `.local/guaba-compat-tests/1795254691106411000/matrix-results.json`。clean 和 compat 已安装并逐份核对 SHA256，见 `.local/hmcl/deployments/20261121-175150-577398/receipt.json`。两个实例的资源包和各一个测试世界的数据包已热同步，见 `.local/hmcl/hot-sync/20261121-175225-504306/receipt.json`。
- Wiki 构建、手册生成测试和五件道具美术一致性检查通过。离线浏览器审阅通过旋转、缩放和五个视角验证。离线 Blockbench 导入工程后读到 9 个部件和 64×64 贴图。浏览器与编辑器证据在 `.local/dream-review/`。

## 适配

后续按各目标核对双格床与睡眠事件、非强制跨维度重生入口、战斗属性接口、可选材料宿主支持及配方格式。完成实际适配后再开放这些版本的手册章节。

| 目标 | 状态 | 说明 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 需要适配新梦系内容 |
| 1.19.1 Forge | 待适配/验证 | 需要适配新梦系内容 |

## 整理提交复验

- 本轮重新执行 `./scripts/mod.sh 1.20.1 build dreamProbeJar -PdreamProbe --offline --console=plain`、发行校验、44 项 Python 测试及 Wiki 构建，全部通过。日志 `.local/tidy-20261121-build.log`、`tests.log`、`wiki.log`（后两者同前缀）。系统 Python 缺少 Pillow，模型校验改用项目已有 `.local/dream-art-venv/bin/python designs/guaba-bed/validate.py` 后通过，没有重新生成或修改设计。
- 对最终发行包重新执行六组梦系兼容探针：`none,silentgear,tconstruct,tetra,all,all-current` 全部通过，均就绪、无测试错误、正常停服、退出 0；后台认证公钥获取也未报失败。完整结果和依赖哈希见 `.local/guaba-compat-tests/1795257018909239000/matrix-results.json`，摘要 `.local/tidy-20261121-matrix.log`。这些是隔离服务端测试，不代表客户端或完整整合包验收。
- 本轮发行包 SHA256 仍为 `b985ab51a8ed197cddca040c37f123808045d6644e81b84527c543a4ef895725`，与上述六组被测包一致。`deploy-local.py` 完整安装两个实例，本次回执 `.local/hmcl/deployments/20261121-183234-723495/receipt.json` 为 `installed`；独立读取 clean、compat 的实际 JAR 计算哈希，与发行包和探针记录完全一致。
- `hot-sync.py` 同步两个实例资源及各自现有测试世界的数据包；成功回执 `.local/hmcl/hot-sync/20261121-183255-129237/receipt.json`。未启动或操作游戏，未执行游戏内重载。
- 三个目标的手册生成与主版本专属段落筛选检查通过，但本轮没有构建或运行次要版本，也未推进适配检查点。JSON、Python 语法及 12 张新增 PNG 的 CRC 检查通过；待提交文本未发现本机绝对路径或常见凭证标记。保留私人策划目录及 Obsidian 忽略规则，正式设计工程、测试源码和玩家文档随功能提交。
- 未重新执行浏览器或 Blockbench 验收，沿用上文明确标注的历史记录；客户端模型、睡眠界面、宿主工作台操作与远端 CI/Pages 仍无本轮验收结论。
