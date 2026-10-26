# 2026-10-26：瓜巴剑与工作台装备转换

- 变更 ID：2026-10-26-guaba-sword
- 需求与行为：瓜巴工作台消耗 1 瓜巴、1 钻石剑，20 tick 后输出瓜巴剑。玩家每次攻击先随机取整数 -3..+3 加到基础攻击属性（无其他修饰的满蓄力伤害 4..10），再走原版蓄力、暴击、附魔和护甲计算。成功近战命中施加现有瓜巴 I，100 tick；重复命中刷新，不降级更强效果。剑的速度、耐久、附魔能力、钻石修理沿用钻石剑。生物持剑命中同样施加状态，但不触发玩家专属的随机攻击钩子。
- 转换契约：新增可选 `copy_input_tag`，默认 false，启用时限一对一转换，复制标准物品 tag。钻石剑配方启用，保留附魔、名称和损耗；不承诺复制模组独立 Forge capability。机器以 assemble 的实际结果进行输出检查。配方网络格式新增字段，两端需更新 JAR。旧泥土配方行为保持。
- 实现路径：1.20.1 `item/GuabaSwordItem.java`、`item/ModItems.java`、`workbench/GuabaConversionRecipe.java` / `GuabaWorkbenchEntity.java`；`data/pinkkicore/recipes/guaba_conversion/diamond_sword_to_guaba_sword.json`；图标、语言与手持模型位于同版本 assets。
- 图标：`designs/guaba-sword/generate.py` 为 16px 可编辑矩阵，参考钻石剑轮廓，使用瓜巴的红色瓜瓤、深籽点、土黄护手和柄。源图、运行时与 Wiki PNG 字节一致；原版参考权属见设计目录 README。
- 玩家内容：Wiki 道具、加工图、更新记录同步；现有独立手册编译器加入瓜巴剑图标映射，直接从同一 Wiki 来源生成正文与实际配方引用。JEI 通过已有自定义类别自动枚举新配方，没有新增强制依赖。
- Git 参照：开始时 HEAD `deabc67`。保留工作区已有手册改动，本次在其现有实现上同步条目。
- 验收：将命名/附魔/损耗钻石剑和瓜巴放入工作台；19 tick 无产物，20 tick 精确消耗并保留元数据。查询 JEI 加工图。多次普通攻击观察伤害变化和 5 秒状态，检查蓄力/暴击/附魔及更强状态保留；审阅背包与手持图标。
- 1.20.1 构建与发行校验：`./scripts/mod.sh 1.20.1 build workbenchProbeJar -PworkbenchProbe --console=plain` 通过（`.local/guaba-sword-build.log`）；`python3 scripts/verify-artifact.py 1.20.1` 通过。最终 SHA256 `3ea04981bf668be19a3cd3bc84020bcff1a2d145ff6a90ccca2636723c2292a1`。
- 无头验证：`python3 scripts/check-workbench.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar` 通过；`.local/workbench-tests/1792985687334270000/result.json` 为 passed，正常就绪、停服、退出码 0、无 ERROR。覆盖既有工作台回归、装备转换与网络/元数据、400 次随机属性取值、实际 Player.attack 的普通/半蓄力/暴击/锋利 III 共 120 次、命中效果与临时属性清理。探针中的玩家基础伤害显式使用原版装备修饰，未操作真实游戏实例。初次完整攻击测试的奶牛生命值假设被纠正（奶牛上限非 20），最终以 100 生命测试目标验证伤害差值。
- 文档验证：四项手册来源编译/断链/组件测试通过；Wiki 静态构建通过（`.local/guaba-sword-wiki.log`），`git diff --check` 通过。手册正文与加工图引用已进入 JAR。远端 Wiki 未发布。
- 完整部署：`python3 scripts/deploy-local.py` 成功；`.local/hmcl/deployments/20261026-113518-825689/receipt.json` 为 installed，clean、compat 实际 mods 中 JAR 的 SHA256 均与上述产物相同。未启动游戏。资源/数据受管覆盖包同步回执 `.local/hmcl/hot-sync/20261026-113535-170210/receipt.json` 为 ok，日志 `.local/guaba-sword-hot-sync.log`；没有在客户端执行 F3+T 或 /reload。
- 客户端边界：背包/手持图标、JEI 配方显示和实际打击手感尚未实机验收；未启动图形客户端。
- 移植注意：Forge Item 攻击钩子、瞬时属性 UUID、服务端 tick 清理、effect 注册、配方序列化和图标资源均需按目标 API 核对；common 未引入 Minecraft 依赖。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 未构建或部署 |
| 1.19.1 Forge | 待适配/验证 | 未构建或部署 |
