# 2026-11-02：生物阵营与瓜巴影融特殊敌对

- 变更 ID：2026-11-02-mob-factions。
- Git 参照：工作开始 HEAD `c9b6ea8`；保留此前瓜巴龙与其他工作区改动，不提交或推送。
- 需求与行为：建立简单的敌对、中立、特殊三类阵营，描述默认对玩家态度。朱罗纪瓜巴龙为敌对；粉红妹和饼蛙为中立；瓜巴影融为特殊，对玩家仍中立受击反击。阵营是简单分类，不增加领地、声望、友军共享或不同阵营间的普遍战争机制。
- 特殊敌对：所有实体类型命名空间为 `pinkkicore` 的 Mob，排除瓜巴影融实体本身，在服务端加入世界时自动附加优先级 0 的最近瓜巴影融索敌目标。无需挑衅，需要视线，沿用实体 FOLLOW_RANGE（粉红妹/饼蛙 16、瓜巴龙 24 格）与原版目标存活、敌我和不可攻击过滤；优先于当前优先级 1 的受击反击和瓜巴龙优先级 2 的玩家目标。失去目标后沿用原版目标记忆及停止规则。
- 行为边界：按实体类型识别，不按可见名字识别；改名仍被攻击。瓜巴影融同类之间不主动攻击，受击后反击规则保留。原版和第三方实体不挂接目标；闪电苦力怕刷怪蛋生成的是原版 Creeper，不改变其 AI。NoAI 不执行索敌。没有调整攻击伤害、生成、掉落、碰撞、头顶名称或现有生物的玩家态度。
- 实现：`common/src/main/java/io/github/vitech/pinkkicore/core/MobFaction.java` 为不依赖游戏 API 的 Java 17 枚举；1.20.1 `entity/MobFactions.java` 提供分类与 Forge `EntityJoinLevelEvent` 服务端挂接。按目标实例类型查重，重复加入世界不会累计目标；加载已有存档时重新挂接，不额外保存攻击对象。保留已有物种各自的近战/吐舌/喷火实现。
- 玩家说明：`wiki/bosses.md#factions`、`wiki/changelog.md`，手册直接从同一 Wiki 来源生成；中英文阵营名称语言键随包提供。没有额外修改名称牌外观。

## 验收场景与验证

1. 粉红妹、饼蛙、朱罗纪瓜巴龙与瓜巴影融相遇，自动锁定并分别用近战、舌头或火焰造成伤害；给目标改名仍生效。
2. 瓜巴龙已有玩家目标、粉红妹受到其他生物攻击时，可见的瓜巴影融仍被优先锁定。
3. 隔墙不会初次索敌；移除墙后自动获取并攻击。
4. 附近只有玩家和其他中立生物时，粉红妹/饼蛙不会因此主动攻击；瓜巴影融之间不主动攻击；NoAI 生物不行动。
5. 原版牛和苦力怕 AI 不变；重复世界加入不添加重复目标；保存后重载恢复特殊敌对。

1.20.1 实际验证：

- `./scripts/mod.sh 1.20.1 build factionProbeJar -PfactionProbe` 构建、独立探针重映射通过。日志 `.local/factions/build.log`。
- `python3 scripts/verify-artifact.py 1.20.1` 发行包校验通过，增加阵营类与中英语言键检查。
- `python3 scripts/check-factions.py --geckolib <本地 1.20.1 GeckoLib 4.2.3 JAR>` 安装实际发行 JAR 到一次性、仅绑定回环地址的无头 Forge 服务端，上述五类验收通过。观察到服务端就绪、`FACTION_PROBE PASS`、正常 stop 和退出码 0。证据 `.local/faction-tests/1793607186621706000/result.json`、`console.log`、`.local/factions/runtime.log`；被测发行包 SHA256 为 `f01873a4b489ae7b6f148b54048f29923ae3cfeff45b0adeb30366ee99591e32`。
- `python3 -m unittest discover -s scripts/tests -p test_guide.py`：4 项通过；构建已生成含阵营章节的离线手册。`npm ci` / `npm run wiki:build` 通过，日志 `.local/factions/wiki-build.log`。
- `python3 scripts/deploy-local.py`：clean、compat 完整安装成功。本次回执 `.local/hmcl/deployments/20261102-161442-973014/receipt.json` 状态 `installed`；两个已安装 JAR、当前发行产物、上文无头测试 JAR 的 SHA256 完全一致，复核 `.local/factions/deployment-check.json`。
- `python3 scripts/hot-sync.py`：两个实例资源包及各自全部已有测试世界数据包同步成功，记录 `.local/hmcl/hot-sync/20261102-161516-696506`；未在游戏中自动重载。正式 JAR 验收前可用 `hot-sync.py remove` 移除受管覆盖包。

未启动 Minecraft 客户端；实际界面、名称牌及遭遇体验未实机验收。Wiki 为本地源同步/构建，不表示 Pages 已上线。其他版本不因共享枚举存在而自动拥有阵营功能，不推进适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 需对应版本事件与目标挂接，并验证各物种索敌、伤害和存档重载 |
| 1.19.1 Forge | 待适配/验证 | 需对应版本事件/API 适配与行为验证 |

## 2026-11-05 整理提交复验

- 本轮仅重新构建、运行与部署 1.20.1。`build guabaDragonProbeJar factionProbeJar -PguabaDragonProbe -PfactionProbe --offline`、发行校验、44 项 Python 测试及 Wiki 构建通过，日志分别为 `.local/tidy-20261105-build.log`、`artifact.log`、`tests.log`、`wiki.log`（后三者同前缀）。
- 在 GeckoLib 4.2.3 下对当前发行包执行两套独立无头探针：阵营分类、索敌优先级、真实伤害、中立/同类/NoAI 排除、遮挡及重载通过；瓜巴龙生成、刷怪蛋、名称/NBT、喷火时序、伤害、射程/遮挡、取消及和平规则通过。两个服务端均就绪、无 ERROR、正常 stop、退出 0。证据 `.local/faction-tests/1793868005164911000/result.json`、`.local/guaba-dragon-tests/1793868008313085000/result.json`；摘要日志 `.local/tidy-20261105-factions.log`、`dragon.log`（同前缀）。
- 当前发行 JAR SHA256 为 `f01873a4b489ae7b6f148b54048f29923ae3cfeff45b0adeb30366ee99591e32`，与上述两套探针一致。`deploy-local.py` 已完整安装 clean、compat；本次回执 `.local/hmcl/deployments/20261105-164106-566925/receipt.json` 为 `installed`，独立读取两个实例实际 JAR 重新计算哈希也一致。
- `hot-sync.py` 已同步两个实例资源及各自现有测试世界数据，成功回执 `.local/hmcl/hot-sync/20261105-164132-384060/receipt.json`；未操作游戏或执行游戏内重载。
- 瓜巴龙设计一致性与最低 GeckoLib 资产解析通过；独立 `guaba_texas` 皮肤 PNG、两份 Blockbench 工程及内嵌贴图校验通过。后者仅为设计交付，未接入发行包。本轮不重新生成或重设计资产。
- Wiki、手册共享源和玩家更新记录随功能一起整理；忽略规则已覆盖缓存、运行目录、日志、回执与备份，无需变更。未启动客户端，不重新验证次要版本，也不推进适配检查点；Blockbench 瓜巴龙编辑器导入、客户端外观和远端 CI/Pages 仍无本轮验收结论。
