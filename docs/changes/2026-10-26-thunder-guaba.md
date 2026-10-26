# 2026-10-26：雷霆瓜巴与批量雷击转换

- 变更 ID：2026-10-26-thunder-guaba
- 需求与完成行为：新增基础材料雷霆瓜巴（Thunder Guaba，`pinkkicore:thunder_guaba`），不可食用、堆叠 64、创造模式“原材料”可见。仅将实际受雷击的瓜巴掉落物按 1:1 原位替换为雷霆瓜巴；支持整组和多个独立掉落物。保留数量、NBT/名称及原 ItemEntity 的位置、运动、年龄、归属、拾取延迟。当前不添加合成、酿造或匠魂/Silent Gear/Tetra 材料属性与特性。
- 用户确认：雷霆瓜巴天然耐火，防止同次闪电生火烧毁产物。物品 fireResistant 同时按原版规则免疫火焰、熔岩等火类伤害；雷击事件对雷霆瓜巴本身取消伤害，防止同一闪电多 tick/多次闪烁或后续新雷击销毁。未增加普通爆炸、仙人掌、虚空等其他伤害免疫。
- 源码位置：1.20.1 `forge/item/ModItems.java` 与 `GuabaLightningConversion.java`；目标 `assets/pinkkicore/{models/item,textures/item}/thunder_guaba` 及语言资源。`designs/thunder-guaba/` 保存 imagegen 透明源图、16×16 原生图标及放大预览；Wiki 使用同一 PNG。获取示意图由 `scripts/generate-thunder-guaba-diagram.py` 从发行图标生成。
- Git 参照：开始时 HEAD `deabc67`。保留工作区已有瓜巴剑、引导手册及后续出现的瓜巴弓等并行修改；只对共同注册、语言和内容生成文件做本需求增量，不提交或回退其他任务代码。
- 触发设计：使用 Forge `EntityStruckByLightningEvent` 的 LOWEST、默认不接收取消事件订阅。仅服务端、仍存活的瓜巴 ItemEntity 可转换；其他模组此前已取消的事件不消费材料。不扫描背包、不全图吸取、不替换原版闪电或改动雷击范围，不清除地面火焰，不干涉其他物品和生物。自然雷、引雷等真实闪电使用宿主原有命中区域；纯视觉 LightningBolt 不触发事件，不转换。命令生成的真实闪电也可用于管理员测试，不额外提供获取指令。
- 来源核对：[官方 Forge 1.20.x 的雷击事件说明](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.x/src/main/java/net/minecraftforge/event/entity/EntityStruckByLightningEvent.java)说明支持在实体遭雷击前取消；本地固定 1.20.1 / Forge 47.4.28 反编译核对真实 LightningBolt.tick 调用及重复闪烁逻辑。原版 `is_fire` 伤害标签不包括 lightning_bolt，因此耐火属性之外仍需单独保护产物的后续雷击。
- 验收场景：地上各丢 1、64、17、32 个瓜巴，落雷命中时得到共 114 个雷霆瓜巴；名称/NBT 保留。另一组瓜巴在范围外不转换；纯视觉闪电不转换；已取消的事件不转换。范围内普通钻石仍按原版被雷击摧毁。雷霆瓜巴连续受雷和地面火焰后保持存活、数量不变，不生成重复产物。
- 1.20.1 构建：`./scripts/mod.sh 1.20.1 build thunderGuabaProbeJar -PthunderGuabaProbe --console=plain` 通过（`.local/thunder-guaba/build.log`）。`python3 scripts/verify-artifact.py 1.20.1` 通过（`verify.log`），覆盖 16×16 PNG、设计源/运行纹理/Wiki 图标一致、模型、双语名称和转换类入包。
- 发行 JAR 无头测试：`python3 scripts/check-thunder-guaba.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar` 通过（`.local/thunder-guaba/probe.log`、`.local/thunder-guaba-tests/1792999482845571000/result.json` 与 `console.log`）。真实 LightningBolt 正常世界 tick，验证批量 114、整组数量、名称/NBT、原实体拾取延迟和年龄、火/熔岩伤害、范围/视觉/取消对照，以及再次落雷并观察 100 tick 无丢失或重复。测试服务端绑定 `127.0.0.1:25585`、使用全新项目隔离世界、正常就绪与停服、退出码 0，无 ERROR。探针不进入发行包。此轮包 SHA256 `9b52515f0b1f878cfb29db4712833e53ca3fb45d8b7bd386a0db10480c047aed`。
- 内容验证：现有四项手册生成测试通过（`.local/thunder-guaba/guide-tests.log`）；`npm run wiki:build` 通过（`wiki.log`）。`wiki/items.md`、`wiki/changelog.md` 同步玩家获取方式、耐火及 1:1 数量；`generate-guide.py` 将同一章节与实际物品图标生成进离线手册，雷击示意使用已有原生 item/text 节点，不伪造不存在的工作台配方或新增手册前置。
- 完整部署：`python3 scripts/deploy-local.py` 成功，回执 `.local/hmcl/deployments/20261026-152643-982623/receipt.json` 为 `installed`；clean、compat 两个实例实际安装 JAR SHA256 均与发行包及上述无头测试包一致（`9b52515f0b1f878cfb29db4712833e53ca3fb45d8b7bd386a0db10480c047aed`）。日志 `.local/thunder-guaba/deploy.log`。未启动或操作游戏，未发布 Release/标签或远程服务端。
- 热同步：`python3 scripts/hot-sync.py` 已执行，日志 `.local/thunder-guaba/hot-sync.log`；回执 `.local/hmcl/hot-sync/20261026-152724-045787/receipt.json` 为 `ok`，两个实例资源包及全部已发现测试世界的数据包同步成功。资源与数据分别由玩家用 F3+T、`/reload` 重载；正式 JAR 客户端验收前按流程移除受管覆盖包。
- 未验证：客户端物品/手册图标与排版、真实雷雨或引雷三叉戟的玩家实机操作、多人网络显示及任意整合包。无头测试验证真实闪电实体命中链，不代替客户端验收；也不声称远端 Wiki 已部署。
- 移植注意：只接入 1.20.1。次要目标须核对 Forge/NeoForge 雷击事件、掉落物与资源 API；共享层不引入 Minecraft 或客户端类型，不推进检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 按需核对雷击事件与掉落物 API |
| 1.19.1 Forge | 待适配/验证 | 按精确 1.19.1 API 补齐，不能以 1.19.2 替代 |
