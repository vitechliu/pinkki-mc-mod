# 2026-11-21 · 瓜巴龙蛋与宠物瓜巴龙

- 变更 ID `2026-11-21-guaba-dragon-pets`，开发起点 `f1f878b`。
- 朱罗纪瓜巴龙固定掉落 1–2 枚普通龙蛋。普通龙蛋可用瓜巴与鸡蛋无序合成，也可用瓜巴工作台加工。粉红、云梦与冥梦各使用 8 个同种材料围绕普通蛋合成，雷霆通过充电桩加工。
- 五种蛋在足够宽敞的地面上召唤绑定使用者的宠物。宠物随主人行动，协助主人咬击和喷吐。宠物只攻击非玩家目标，喷吐只命中瞄准的生物，保护友军及同主宠物。
- 粉红拥有 4 倍最大生命。雷霆有 3 倍移速，咬击和喷吐均采用雷电类型，注册表有铁魔法的雷电伤害时优先使用。云梦有 2 倍移速，战斗遇到高差或地面无法接近时可腾空约 2 秒。冥梦在下界拥有 3 倍最大生命和攻击伤害，往返维度按最大生命截断当前值。
- 实体、道具、资源与数据位于 1.20.1 适配层。复用野生龙的喷吐计时与遮挡，宠物独立注册为 `pinkkicore:pet_guaba_dragon`，主人与变体随实体存档。Wiki 与手册共用条目和原始配方。设计工程与审阅入口见 [瓜巴龙美术](../../designs/guaba-dragon/README.md)。

## 验收场景

- 击杀野生龙并检查掉落，制作全部配方，验证梦系同类升级材料无法混用。分别在生存、创造、阻塞地面使用龙蛋，存档重载后核对主人、变体、名称和属性。
- 引导宠物跟随，让玩家打击或受到生物攻击；在目标与宠物间放置墙壁或友军，确认喷吐不会穿墙或误伤。分别检查雷霆咬击与喷吐、云梦高差腾空、冥梦往返下界的生命和攻击变化。
- 游戏内粒子、动画衔接、命中手感和配方界面保留手动客户端验收。

## 1.20.1 验证

- `./scripts/mod.sh 1.20.1 build guabaDragonProbeJar workbenchProbeJar -PguabaDragonProbe -PworkbenchProbe --offline --console=plain` 与 `python3 scripts/verify-artifact.py 1.20.1` 通过。发行包 SHA256 为 `22d853eadf9b6d01e19dcb3f8ec154a8cc245e51b1996678336e28926bb293df`。
- Forge 47.4.28 独立无头服务端在 GeckoLib 4.4.9 和 GeckoLib 4.7.1.1 + 铁魔法 3.4.0.11 两种环境中均通过，同一发行包正常就绪和停服。探针覆盖真实掉落、全部配方、两台设备 20 tick 转换、龙蛋消耗与存档、宠物跟踪两类主人战斗事件、云梦短飞中咬击、友军和墙体防护、冥梦跨维度存档加载，以及雷霆的两种伤害来源。结果分别位于 `.local/guaba-dragon-tests/1795271532703599000/result.json` 和 `.local/guaba-dragon-tests/1795271535295183000/result.json`。
- 工作台与充电桩回归通过，结果见 `.local/workbench-tests/1795271604279175000/result.json`。45 项 Python 测试通过，覆盖主版本手册中的五个图标、八条配方以及次要目标手册过滤。Wiki 使用 `npm ci --offline` 与 `npm run wiki:build` 完成本地构建。
- 设计资源校验以及 GeckoLib 4.2.3 解析通过。用离线浏览器检查导出模型的四套配色、四向镜头、拖拽和缩放、飞行动画时间轴。Blockbench 内置 Bedrock 导入器读到 101 个部件、24 根骨骼、四套 512×512 贴图及五段动画。离线编辑器页面加载不完整，Blockbench 验证范围为内置导入器的数据读取。记录见 `.local/guaba-dragon-pets-review/`。

## 本地部署

- `python3 scripts/deploy-local.py` 更新 clean 与 compat。回执 `.local/hmcl/deployments/20261121-223348-883059/receipt.json` 为 `installed`，两份已安装 JAR 的 SHA256 均与上面通过探针的发行包一致。
- `python3 scripts/hot-sync.py` 已同步两个实例的资源包及各自的测试世界数据包，回执 `.local/hmcl/hot-sync/20261121-223420-929094/receipt.json` 为 `ok`。Minecraft 客户端外观、粒子和操作留待手动验收。Wiki 已本地构建，Pages 部署待推送。

## 适配

| 目标 | 状态 | 说明 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 核对实体同步、伤害注册表、配方资源路径和 GeckoLib 动画 |
| 1.19.1 Forge | 待适配/验证 | 核对宠物与自定义伤害 API，适配 GeckoLib 3 资产 |

## 整理提交复验

- 本轮重新执行上述主版本构建、发行校验、45 项 Python 测试、Wiki 构建、设计一致性校验及最低 GeckoLib 4.2.3 资产解析，全部通过。日志 `.local/tidy-pets-20261121-build.log`、`tests.log`、`wiki.log`（后两者同前缀）。没有重新生成或重设计资产。
- 当前发行包 SHA256 仍为 `22d853eadf9b6d01e19dcb3f8ec154a8cc245e51b1996678336e28926bb293df`。GeckoLib 4.4.9 基础环境、GeckoLib 4.7.1.1 + 铁魔法 3.4.0.11 共存环境的瓜巴龙探针，以及基础环境工作台/充电桩回归全部通过；三个隔离服务端均就绪、无 ERROR、正常停服、退出 0。证据分别为 `.local/guaba-dragon-tests/1795271899959580000/result.json`、`1795271900070964000/result.json`（同目录前缀）和 `.local/workbench-tests/1795271899762672000/result.json`；摘要日志 `.local/tidy-pets-20261121-{dragon,irons,workbench}.log`。
- `deploy-local.py` 已完整更新 clean、compat；本次回执 `.local/hmcl/deployments/20261121-223943-191173/receipt.json` 为 `installed`。独立读取两个实例实际 JAR 计算哈希，与发行包及三个探针结果一致。`hot-sync.py` 已同步两个实例资源及各自现有测试世界的数据，成功回执 `.local/hmcl/hot-sync/20261121-224002-255024/receipt.json`；没有执行游戏内重载。
- 待提交 JSON、Python 语法及 22 张 PNG 的 CRC 检查通过；文本未发现本机绝对路径或常见凭证标记。已有忽略规则覆盖构建、缓存、日志与回执，无需调整；设计工程、贴图、探针源码和玩家文档保留。
- 本轮没有客户端、浏览器、Blockbench、次要目标或远端 CI/Pages 验收，也未推进适配检查点。上文浏览器及编辑器记录保留为历史证据，不作为本轮重新验收结论。
