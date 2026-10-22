# 2026-10-22：饼蛙实体与刷怪蛋

- 变更 ID：2026-10-22-bingwa-entity
- 需求与完成行为：将已确认的饼蛙模型和七段动作接入 1.20.1 Forge。实体 `pinkkicore:bingwa`，20 生命、基础攻击伤害 3，视觉保留头顶小蛙与橘子，两者为装饰。两栖寻路、水中移动与呼吸、陆地缓行和间歇跳跃、闲置鸣叫；使用 HurtByTargetGoal 仅向伤害来源反击，不主动索敌、不召集同伴。吐舌在第 6 个服务端 tick 判伤（0.3 秒前摇），命中时重新检查目标、存活、可攻击性、2.5 格距离与视线。动画不负责判伤或世界位移。未添加捕食、繁殖、自然生成、配方或专属掉落；空实体掉落表。原版刷怪蛋模板着色为奶油色 `#EFE4BD` / 橘色 `#E89328`，位于创造刷怪蛋分类，ID `pinkkicore:bingwa_spawn_egg`。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/entity/`、`client/`、`item/ModItems.java`、入口；目标资源的 `geo/`、`animations/`、`textures/entity/`、刷怪蛋模型、语言与空掉落表。保留 `designs/bingwa/` 为源工程，发行包校验逐字节比对三份模型资产。
- Git 参照：工作开始时 HEAD `458332acf496a0f9857926fe3380be63a848cfa0`。不覆盖并行 Wiki/工作流管理修改。
- 验收场景：`/summon pinkkicore:bingwa ~ ~ ~` 成功；`/give @s pinkkicore:bingwa_spawn_egg`，对刷怪蛋分类和对方块使用进行验收。未受击时不攻击附近玩家/动物；生存玩家攻击后反击，创造/旁观模式目标规则遵循原版。观察吐舌前摇，前摇中远离或移至墙后不应被击中。水中不溺水，能游动和离水；陆地行走/跳跃/待机/鸣叫自然切换。重进世界后实体和自定义名称/生命保留。普通世界没有添加饼蛙自然生成；不由装饰自动生成独立小蛙。客户端部分待人工验收，不自动启动客户端。
- 1.20.1 验证：`./scripts/mod.sh 1.20.1 build bingwaProbeJar -PbingwaProbe --console=plain` 通过（`.local/bingwa-build.log`）；`python3 scripts/verify-artifact.py 1.20.1` 通过（`.local/bingwa-artifact.log`）；源资产一致性检查 `python3 designs/bingwa/validate.py` 通过。相同发行 JAR（SHA256 `1995652d17dc51d0c33c9f1594f4a93ef702583c71b839040ca29048115f0f9b`）在 Forge 47.4.28 + GeckoLib 4.4.9 / 4.7 两组独立服务端均通过指令召唤、实际刷怪蛋使用、属性/NBT、40 tick 未受击中立、受击后 AI 反击、6 tick 延迟命中、超距与隔墙不命中及水下呼吸能力检查。两组服务器就绪后正常停服，进程退出码 0；证据分别在 `.local/bingwa-tests/1792664978538726000/` 和 `.local/bingwa-tests/1792664999686995000/` 的 `console.log`、`result.json`。Python 语法、`git diff --check` 和 `npm run wiki:build` 通过。未启动 Minecraft 客户端；水中实际寻路、跳跃/游泳观感、渲染、声音、多人动画同步和实体完整重启存档仍待人工验收；NBT 往返不等同于重启存档验收，也不代表完整整合包兼容。
- 测试实现：`scripts/tests/bingwa_probe/` 是仅用于独立测试的 Forge mod；`./scripts/mod.sh 1.20.1 build bingwaProbeJar -PbingwaProbe --console=plain` 构建，`python3 scripts/check-bingwa.py --geckolib <本地Forge1.20.1库JAR>` 使用已确认 EULA 的 `.local/package-test/1.20.1` 安装依赖，新建 `.local/bingwa-tests/<时间戳>/` 测试世界并绑定 `127.0.0.1:25580`，检测明确 PASS/FAIL 后正常停服。探针与测试类不进入发行 mod。探针检查真实指令召唤、FakePlayer 使用刷怪蛋、属性/NBT 往返、未受击中立、受伤后 AI 反击、延迟判伤和超距/隔墙拒绝；不把 FakePlayer 检查当作客户端交互验收。
- 手册与 Wiki：同步 `wiki/bosses.md#bingwa`、`wiki/items.md#bingwa-spawn-egg`、首页、指南与玩家更新记录，标明 1.20.1 / 0.1.0 开发中未发布。自研游戏内手册尚未实现，稳定条目 ID `bingwa` 后续需接入；当前没有游戏内可阅读入口，不能声称已经同步到可用手册。未发布或部署 Wiki。
- 移植注意：实体注册/属性/AI/刷怪蛋留在版本层；客户端渲染和 GeoModel 仅由 Dist.CLIENT 入口加载，公共实体不引用客户端类。GeckoLib 编译基线仍为 4.4.9，不提高依赖下限。1.21.1 / 1.19.1 的注册、AI、动画同步及资源需按各自 API 适配并实际验证；本轮不构建它们、不推进检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 实体、属性、客户端渲染、刷怪蛋、AI 与资源待移植 |
| 1.19.1 Forge | 待适配/验证 | 按该版本实体与动画 API 适配并验证 |
