# 2026-10-23：瓜巴工作台转换与自动化

- 变更 ID：2026-10-23-guaba-workbench。
- 需求与完成行为：把已确认第三稿模型接入 1.20.1 工作方块，右键三槽界面；20 刻转换，首批占位配方为 1 泥土 + 1 瓜巴 → 1 泥巴；四态红石、六面物品输入/输出/双向/禁用、保存恢复和拆除掉落。默认五面输入、底面输出；通过原版漏斗和 Forge 物品接口被动搬运。
- 源码位置：主版本 `forge/workbench/` 与 `forge/client/GuabaWorkbenchScreen.java`、入口注册；资源 `blockstates`、`models`、`textures/block`、`loot_tables/blocks`、`recipes/guaba_conversion` 和双语语言文件。设计源 `designs/guaba-workbench/`；测试 `scripts/tests/workbench_probe/`、`scripts/check-workbench.py`；产物校验扩展 `scripts/verify-artifact.py`。
- Git 参照：工作开始 HEAD `e44bc47`；保留用户先前 README 修改与 `docs/reference-mods.md`，未提交、未推送、未发布。
- 验收场景：19 刻不扣料，20 刻消耗并生成泥巴；连续加工；缺瓜巴/未知原料不消耗；输出满/不兼容暂停并恢复；更换输入重新计时；四态×两种真实信号；六面×四种 IO 模式（含模拟、不允许输出塞入、输入不可抽出）；缓存端口随配置改变、失效重建；真实漏斗供料/取料及禁用面隔离；NBT 库存/进度/配置与异常编号；菜单 Shift 点击、按钮编号/距离限制；配方序列化；拆除物品各掉落一次。
- 1.20.1 验证：构建与发行 JAR 无头探针通过，使用 Forge 47.4.28 / GeckoLib 4.4.9；日志 `.local/guaba-workbench-probe-build.log`、`.local/guaba-workbench-runtime.log`，一次性测试 `.local/workbench-tests/1792739335624824000/`，其中 `result.json` 为 `passed=true`、`exit_code=0`、`ready=true`、`stopped=true`、`errors=false`。本轮发行 JAR SHA256 `912a64407c4dafadb5ce3ade213f71d08ab2d8f480aef91e815fafd4175baf3f`。
- 资源与 Wiki：`python3 designs/guaba-workbench/validate.py`、`python3 scripts/verify-artifact.py 1.20.1` 通过；JAR 与设计源的模型和贴图逐字节一致。`npm ci --cache .local/npm-cache --offline` 与 Node 24 下 `npm run wiki:build` 通过，日志 `.local/guaba-workbench-npm-ci.log`、`.local/guaba-workbench-wiki-build.log`；JSON 解析和客户端隔离检查通过；浏览器核对实际加工图与工作台图标显示正常，截图 `.local/guaba-workbench-wiki-review/recipes.png`。Wiki 已同步工作台条目、实际 JSON 驱动的图标加工图和玩家更新；自研游戏内手册未实现，`guaba_workbench` 条目与动态配方展示待手册基础能力就绪，不能视作已同步游戏内阅读。详见 [实现与验证说明](../guaba-workbench.md)。
- 未验证范围：没有启动 Minecraft 图形客户端；实际客户端外观/点击/悬停、真实多人网络同步、Mekanism / EnderIO 管道和其他整合包待人工验收。本地 Wiki 构建不代表 Pages 部署，未发布和部署真实服务端。
- 移植注意：不同版本的注册、BlockEntity NBT、菜单网络与 GUI、RecipeSerializer 编解码和 capability API 都需独立适配。本次只改 1.20.1，无共享 Minecraft 依赖，无需推进次要版本检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐，不能复用 Forge capability/API 结论 |
| 1.19.1 Forge | 待适配/验证 | 后续按需补齐 |
