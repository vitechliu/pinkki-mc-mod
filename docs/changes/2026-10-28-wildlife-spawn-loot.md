# 2026-10-28：三种生物稀有野外生成与材料掉落

- 变更 ID：2026-10-28-wildlife-spawn-loot
- 需求与完成行为：允许粉红妹、瓜巴影融和饼蛙在主世界野外低概率自然生成，死亡随机掉落瓜巴、雷霆瓜巴或粉红锭。替代此前“不自然生成、无专属材料掉落”的约定；中立/反击、名字、模型、装备和存档行为不变。
- 自然生成：Forge `forge:add_spawns` 群系修改器向 `#minecraft:is_overworld` 的 CREATURE 列表加入三者，每种权重 1、每组最少/最多均为 1。通过 `SpawnPlacementRegisterEvent` 注册 ON_GROUND、MOTION_BLOCKING_NO_LEAVES。自然与区块初次生成的候选还需处于主世界/主世界群系、动物可生成地面标签（原版草方块）、明亮天光、露天、原版 Mob 生成地面条件，并额外以 1/20 概率通过。此 5% 是候选位置检查的附加门槛，不是“每个区块/每分钟/全体刷怪的 5%”。实际生成仍受原版距离、碰撞、动物容量、生成调度和 `doMobSpawning` 控制，不添加后台定时刷怪或世界写入脚本。
- 数量限制：两名 NPC 原本持久化而不计入普通动物容量，因此新增本地限额：候选位置三维距离 64 格内存活的上述生物合计已有 3 只时，拒绝继续自然生成；不会删除、改名或自动消失已有实体。刷怪蛋、命令等显式生成不施加这些新增自然生成限制。饼蛙保留原有不远距离消失的行为。
- 掉落：三张实体掉落表各 1 个池、1 次抽取，无空结果；权重 7/2/1，对应 70% 瓜巴 1–3、20% 雷霆瓜巴 1–2、10% 粉红锭 1–2。数量为均匀整数，无抢夺加成函数、无玩家击杀条件；自然、刷怪蛋、指令产生的实体均使用相同死亡表。遵循 `doMobLoot`，原版 NPC 装备掉落独立保留。未安装整合包的全局掉落修改器可能另行覆盖数据，当前结论针对项目发行数据。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/entity/WildlifeSpawns.java`、`ModEntities.java` 事件注册；目标 `data/pinkkicore/forge/biome_modifier/add_wildlife.json` 和三份 `loot_tables/entities/`；`scripts/verify-artifact.py` 检查发行表、生成配置和注册类。BingwaEntity 仅更新过时注释，不改变 AI。
- Git 参照：开始时 HEAD `2e522e05749192a13d99fa2db2aef773de5ab237`；保留开始前已有 Tetra 材料本地化、语言与玩家更新记录修改。本次未提交或推送。
- 验收场景：白天在主世界草地等待稀有单只刷新，已有区块也可参与通常自然刷新；下界/末地、石地、遮顶位置不自然生成。附近已有 3 只时不继续生成，仍可使用刷怪蛋或命令。分别杀死三者，确认材料为一类、数量在范围内；关闭 doMobLoot 后不掉材料。两名 NPC 拾取的装备仍按原有规则掉落，命名和中立行为不受影响。客户端实地遇见频率/分布与整合包共存仍待人工验收，不启动游戏客户端。
- 手册与 Wiki：更新 `wiki/bosses.md#wildlife` 和原三条生物说明、玩家更新记录、设计目录的过时自然生成说明。现有自研离线手册从 Wiki 自动生成，重新构建后随 JAR 同步；校验实际生成手册与 Wiki 相等。不发布 GitHub Pages，不修改移植检查点。
- 无头测试：`scripts/tests/wildlife_probe/` 为独立 Forge 测试 mod，不进入发行 JAR；`./scripts/mod.sh 1.20.1 build wildlifeProbeJar -PwildlifeProbe --console=plain` 构建后，`python3 scripts/check-wildlife.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar` 使用已确认 EULA 的独立安装创建一次性平坦世界，回环 `127.0.0.1:25587`、`--nogui`、无实际客户端。只在测试中加入 FakePlayer 供原版自然刷怪玩家距离判断；手动调用原版 NaturalSpawner 的自然选择/判定/插入管线，原版自动调度关闭以隔离测试，不代表生产自然刷新频率。PASS/FAIL 后正常停服并检查退出。
- 测试覆盖：实际修改后的平原/森林/针叶林/沼泽生成表、权重和组大小；下界/末地列表排除；注册高度图/放置类型；每种 2000 次位置判定（固定种子正常草地各通过 99 次），石地、遮顶、本地数量限制、显式刷怪蛋放行；每种 1000 次真实 LootTable 抽样、随机材料频率和数量范围；实际无玩家参与的死亡、NPC 装备仍掉落、关闭 doMobLoot 无奖励；原版自然生成管线实际插入三种实体并保留可见默认名字。
- 调试边界：早期测试发现刚放置顶棚时光照更新可能尚未完成，增加同步高度图校验防止误判露天。测试世界显式配置平坦生成器 layers，避免默认配置缺失导致 ERROR；稀有真实生成采样固定随机种子并扩大最多 30000 次调用窗口，避免短样本漏掉某一类型。不降低游戏中的稀有生成概率。失败日志不作为成功证据。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 群系修改器、放置事件、原版 API 和掉落资源需按目标适配 |
| 1.19.1 Forge | 待适配/验证 | 群系修改器、放置事件、原版 API 和掉落资源需按目标适配 |

## 实际验证结果

- 1.20.1 构建/测试探针编译通过，日志 `.local/wildlife-build.log`；发行 JAR 校验通过，`.local/wildlife-artifact.log`。当前 SHA256 `b0211465d3f25de7191ec4b3ef206d91dc457b917c912ef82dcc64b1b3238bbd`。
- 锁定 Wiki 依赖安装、`npm run wiki:build`、现有手册 4 项单元测试通过，日志 `.local/wildlife-wiki-install.log`、`.local/wildlife-wiki.log`、`.local/wildlife-guide-tests.log`。Python 语法和 `git diff --check` 通过。
- 完整独立服务端探针通过：`.local/wildlife-tests/1793191328679582000/console.log` 与 `result.json`。三种生物均被原版自然生成管线实际插入；位置拒绝与单只生成配置通过，2000 次单种判定均为 99 次接受。三张表每张 1000 次抽样分别得到瓜巴 706、雷霆瓜巴 193、粉红锭 101，均在约定范围内；实际死亡和原装备掉落、doMobLoot 检查通过。服务端就绪、正常停服、退出码 0、无 ERROR、无外部认证错误。
- 部署构建期间工作区另有工作台配方、配方解锁和手册更新，最终包 SHA256 为 `59ee8598f1c27caa26e4b07e9025bdd7b4c166d9ef0bbc273422fd32373ea238`；逐文件比较，变化仅为手册 JSON、瓜巴工作台配方及其解锁数据，生成/掉落代码与表未改变。本次不对同期工作台改动做额外功能结论；发行校验和手册测试已对最终包重新通过。
- 最终包完整复测通过：`.local/wildlife-tests/1793191539748589000/{console.log,result.json}`；原版自然生成管线实际插入三种生物，全部生成/掉落断言通过，服务端就绪、正常停服、退出码 0、无 ERROR。SHA256 与最终部署回执一致。
- 完整部署成功：`.local/hmcl/deployments/20261028-204409-051116/receipt.json` 为 `installed`，clean、compat 两个实际安装 JAR SHA256 均与最终通过探针的发行包一致，旧包已备份。未启动游戏。
- `python3 scripts/hot-sync.py` 已同步两个实例的受管资源包和全部已发现测试世界数据包，日志 `.local/wildlife-hot-sync.log`。新增自然生成注册需重新启动游戏；F3+T / `/reload` 仅刷新资源/数据。正式纯 JAR 客户端验收前使用 `hot-sync.py remove` 移除受管覆盖包并重载。
