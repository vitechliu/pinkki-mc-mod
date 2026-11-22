# 2026-11-22 百科目录、瓜巴手册与 JEI

- **变更 ID** `2026-11-22-wiki-guide-jei`，起始 Git 参照 `ca98d7b`。
- **行为** 百科与 1.20.1 手册分为序章、材料、工作方块、基础武器、药水效果和生物。百科序章显示合成图与前置、联动版本。手册简述粉红矿，在材料和工作方块章节按已安装的联动模组显示相应内容。目录使用「瓜巴充能台」，正文使用游戏内名称「瓜巴充电桩」。
- **实现** `scripts/generate-guide.py` 从 Wiki 同步章节、子目录、配方引用和内部链接。1.20.1 的 `GuideBook` 按已安装模组筛选内容，`GuideScreen` 提供当前可见物品槽，`GuabaJeiPlugin` 使用 JEI 的原生查询行为。沿用版本 1 的手册格式，新字段为可选字段。

## 客户端验收

1. 启动 clean 实例，合成并打开瓜巴手册。检查六章和子目录，确认可阅读真实合成图、悬停显示提示、搜索不到未安装模组的特性。
2. 在 compat 实例打开材料章，确认所安装模组的材料特性可见。在材料独立图标、合成格、加工和酿造图上分别测试左键、默认 R/U 和改绑的键位。检查空格与裁切区域，关闭 JEI 配方界面后应回到原阅读位置。

## 1.20.1 验证

- `./scripts/mod.sh 1.20.1 build guideProbeJar -PguideProbe --offline`、`python3 scripts/verify-artifact.py 1.20.1`、47 项 Python 测试及 Wiki 静态构建通过。生成页面中的 18 个带锚点侧栏链接和序章合成图已核对。
- `check-jei-linkage.py` 分别针对 15.2.0.27 API 和 compat 实例的 15.74.0.228 JAR 通过。无 JEI 的发行 JAR 探针验证了入包、2×2 合成、游戏配方引用、搜索和联动过滤，服务端正常停服。证据见 `.local/guide-tests/1795291068275111000/result.json`。
- 本地部署回执 `.local/hmcl/deployments/20261122-035834-864967/receipt.json` 为 `installed`。clean、compat 的安装 JAR、发行包和探针输入 SHA256 一致，为 `f3b66d076aa164ea61ffed40f200666d8fd9b28f7cf187a3d25570ce3c4f3e9c`。两个实例的受管覆盖包已热同步，回执位于 `.local/hmcl/hot-sync/20261122-035914-674477/receipt.json`。
- 客户端界面、JEI 实际输入和返回行为待手动验收。Wiki Pages 待推送后由工作流发布。

## 次要目标

次要版本沿用原有七章生成模式。后续移植时需对齐各目标的药水、材料和生物覆盖范围，并按该目标的 Screen、ModList 和 JEI API 验证可选联动。

| 目标 | 状态 | 说明 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 手册目录、装载联动和 JEI 原生输入 |
| 1.19.1 Forge | 待适配/验证 | 手册目录、装载联动和 JEI 原生输入 |

## 整理提交复验

- 本轮重新执行主版本 `build guideProbeJar -PguideProbe --offline --console=plain`、发行包校验、47 项 Python 测试及 Wiki 构建，全部通过。日志 `.local/tidy-guide-20261122-build.log`、`tests.log`、`wiki.log`（后两者同前缀）。待提交 Python 语法、文本凭证/本机绝对路径扫描与差异空白检查通过，忽略规则无需调整。
- JEI 15.2.0.27 的 common/Forge API 与 compat 当前 15.74.0.228 发行 JAR 分别解析当前 10 个插件类的 46 个字段/方法链接，全部通过。这是静态链接证明，不代表界面点击、改绑键位或返回位置已验收。
- 无 JEI 环境的发行 JAR 手册探针通过：注册、2×2 合成、服务端使用、六章、搜索、实际配方引用和联动筛选，服务端就绪、正常停服、退出 0，无 ERROR。证据 `.local/guide-tests/1795334851374036000/result.json`，摘要 `.local/tidy-guide-20261122-probe.log`。
- 当前发行包与探针输入 SHA256 为 `f3b66d076aa164ea61ffed40f200666d8fd9b28f7cf187a3d25570ce3c4f3e9c`。`deploy-local.py` 完整安装两个实例，本次 `.local/hmcl/deployments/20261122-160807-508743/receipt.json` 为 `installed`；独立读取 clean、compat 实际 JAR 计算哈希，与发行包及探针一致。
- `hot-sync.py` 成功同步两个实例资源及各自现有测试世界数据，回执 `.local/hmcl/hot-sync/20261122-160827-328227/receipt.json` 为 `ok`。未执行游戏内重载；入包生成的手册已通过完整 JAR 部署更新。
- 本轮仅处理 1.20.1，没有客户端、次要版本或远端 CI/Pages 验收，也未更新标签或 Release。后续推送触发的工作流结果需独立核对。
