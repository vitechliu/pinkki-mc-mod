# 2026-10-28：Tetra 瓜巴材料装备命名本地化

- 变更 ID：2026-10-28-tetra-material-localization
- 需求与完成行为：修复雷霆瓜巴长弓等 Tetra 装备显示 `Tetra.material.pinkkicore_thunder_guaba.prefix` 的问题。补齐普通瓜巴、雷霆瓜巴独立的 `.prefix` 中英文键，并补齐雷霆瓜巴材料分类名称。已有装备动态读取资源，不需要重新锻造或修改 NBT；保留用户自定义名称及材料属性、特性、配方和所有游戏逻辑。
- 根因：此前仅注册 `tetra.material.<材料 key>`。Tetra `ItemModule#getItemPrefix` 在材料派生的装备名前缀路径读取 `tetra.material.<材料 key>.prefix`；截图首字母大写来自宿主前缀格式化，不是物品 ID 或材料未注册。上游 diamond/oak 均独立提供材料名及前缀键。
- 源码位置：`versions/1.20.1/src/main/resources/assets/pinkkicore/lang/zh_cn.json`、`en_us.json`。回归校验 `scripts/tests/test_tetra_localization.py` 从实际 Tetra 材料 JSON 的 key/category 派生命名键，检查两种语言的材料名、装备前缀和分类标签完整性。
- Git 参照：开始时 HEAD `2e522e0`。工作区存在其他任务进行中的 1.19.1 适配、共享构建及文档修改；本次不回退、不适配该目标，也不构建这些改动。
- 验收场景：中文资源重载后，原有雷霆瓜巴长弓、剑等派生装备显示雷霆瓜巴前缀；普通瓜巴装备显示瓜巴前缀。英文使用 Guaba / Thunder Guaba。切换材料和分类时不出现缺失翻译键。用户自行命名的装备维持其自定义名称。
- 1.20.1 验证：先在修复前运行 `python3 -m unittest discover -s scripts/tests -p test_tetra_localization.py`，复现两种语言共 6 处缺键；补齐后同一测试通过。`npm run wiki:build` 通过，仅既有 mcfunction 高亮回退提示。`git diff --check` 对本次改动文件通过。
- 热同步：`python3 scripts/hot-sync.py` 成功，本次 `.local/hmcl/hot-sync/20261028-192036-082396/receipt.json` 为 ok；clean、compat 的 zh_cn/en_us 受管资源包文件与当前源码逐字节一致。脚本也按默认范围同步两个已有测试世界的数据包。本次只修可热更新语言资源，未重新构建或替换发行 JAR；按项目规则不执行无意义的完整部署。玩家在已启用受管覆盖包时按 F3+T 重载语言，无需 /reload 或重新锻造。
- 文档与手册：更新玩家更新记录。现有兼容章节及离线手册已使用正确中文材料名，操作和玩法说明不变；本次翻译资源修正同样供手册原生物品名展示使用，无须重复增加正文条目。未发布线上 Wiki。
- 未验证：未启动、操作或连接图形客户端；本次验证的是命名键契约与实际同步文件，截图所示 UI 的重载后显示仍待玩家确认。未重跑不受影响的战斗/材料数值矩阵。
- 移植注意：仅修 1.20.1，次要目标按需补齐各自 Tetra 对应的语言资源，不推进检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 按对应 Tetra 版本核对命名契约 |
| 1.19.1 Forge | 待适配/验证 | 不介入当前并行适配工作 |
