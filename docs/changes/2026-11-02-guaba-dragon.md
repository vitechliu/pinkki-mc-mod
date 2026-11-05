# 2026-11-02：朱罗纪瓜巴龙

- 变更 ID：2026-11-02-guaba-dragon。
- Git 参照：工作开始时 HEAD `c9b6ea8`；保留工作区原有跨版本、依赖和文档改动。
- 需求与行为：将确认后的红蓝头部瓜巴龙接入 1.20.1；实体 `pinkkicore:guaba_dragon`，默认常显名称“朱罗纪瓜巴龙”，支持命名牌与 NBT 保存。敌对怪物，主动追击生存/冒险玩家，受击可反击；30 生命、近战基础伤害 4、免疫火焰与熔岩。和平难度排除/移除。
- 喷火：目标在约 3–6 格且视线无遮挡、冷却结束时停下蓄力。总动作 96 tick：20 tick 前摇、60 tick 喷射、16 tick 恢复；前摇结束锁定三维方向，射程自实体位置计约 6 格（口部前伸计入），可侧移躲避。每 20 tick 一次 2 点火焰伤害，共三次；不会叠加近战或点燃目标/地形。使用原版 FLAME 粒子；服务端裁剪实体喉部至射线终点的碰撞路径，墙体阻挡粒子和伤害。喷火后冷却 160 tick。NoAI、死亡、目标变化/失效、浮水目标抢占可取消；存档不恢复中途攻击，只保留冷却。
- 生成：Monster 分类，地面生成，权重 6，单只。生物群系标签包含 `#minecraft:is_nether` 和 `#minecraft:is_badlands`；额外按真实维度限制下界或主世界。下界不要求暗处；恶地遵守怪物暗处生成，均需合法地面与非和平难度；自然生成器仍检查空间、玩家距离和怪物数量上限。没有改动原有三种 NPC/饼蛙的生成与掉落规则。
- 掉落：不创建掉落池，实体默认使用原版空掉落表，不复用野外材料掉落；基础击杀经验 5。
- 资源：`designs/guaba-dragon/` 可编辑工程与生成器；增加行走/咬击动作并将喷火姿态调整至 4.8 秒。几何、动画、贴图同步至 `versions/1.20.1/src/main/resources/assets/pinkkicore/`。粒子由服务端使用近似口部位置产生，不加载客户端骨骼；渲染器、模型类隔离在 `client` 包。
- 源码位置：1.20.1 `entity/GuabaDragonEntity.java`、`entity/ModEntities.java`、`item/ModItems.java`、`client/GuabaDragonModel.java`、`client/GuabaDragonRenderer.java`、`client/ClientEvents.java`；独立群系标签、modifier、刷怪蛋模型、中英文语言文件。共享核心和其他版本未改动。
- 玩家说明：Wiki `bosses.md` / `items.md` / `changelog.md` 同步；游戏内手册从 Wiki 生成，沿用稳定条目 `bosses#guaba-dragon`。

## 验收场景

1. 创造栏刷怪蛋或 `/summon pinkkicore:guaba_dragon` 生成，显示默认名称；改名/存档重载保留名称。
2. 生存玩家进入 24 格范围并可见时被主动锁定；近距离受到近战，约 3–6 格时会蓄力并持续喷火三秒。喷火期间不重叠近战；结束后有冷却。
3. 喷射开始后侧移躲避或离开 6 格范围；放置实心墙应截断火焰及伤害。不会生成火方块。
4. NoAI、目标失效或保存重载中断喷火；和平难度不自然生成并移除现有怪物。
5. 下界五种群系与三种恶地存在单只刷怪条目，平原/沙漠/末地没有；恶地需暗处，地面合法，下界无需暗处。
6. 击杀不掉物品；其他 NPC/饼蛙原有材料掉落仍由其自身规则处理。

## 1.20.1 验证

- `python3 designs/guaba-dragon/validate.py` 与 `check-geckolib.py`：四段动画、101 部件、24 骨骼与实际最低 GeckoLib 4.2.3 解析通过。
- `./scripts/mod.sh 1.20.1 build guabaDragonProbeJar -PguabaDragonProbe`：构建及探针重映射通过；日志 `.local/guaba-dragon-review/probe-build.log`。
- `python3 scripts/verify-artifact.py 1.20.1`：发行包、语言键、模型源一致性、四段动画、生成标签/modifier 和无掉落池检查通过。
- `python3 scripts/check-guaba-dragon.py --geckolib <本地 Forge 1.20.1 GeckoLib 4.2.3 JAR>`：实际发行包无头独立服务端通过，观察到就绪、全部探针标记和正常 stop/退出码 0。证据 `.local/guaba-dragon-tests/1793606633474441000/result.json`、`console.log` 及 `.local/guaba-dragon-review/runtime.log`。覆盖八种群系最终刷怪列表、下界/恶地生成谓词、其他群系和末地排除、刷怪蛋、默认名称与改名 NBT、真实死亡无物品掉落、无挑衅玩家索敌、近战、60 tick 喷射、三次各 2 伤害、冷却、墙体/6 格外拒绝伤害、NoAI 取消、攻击中途存档重置和和平移除。初次探针因误将原版牛生命值按 20 断言失败；已将测试靶牛明确设为 20 后完整重跑通过，未据此调整正式伤害。
- 手册共享源生成通过，`python3 -m unittest discover -s scripts/tests -p test_guide.py` 4 项通过；`npm ci` / `npm run wiki:build` 通过。生成章节包含朱罗纪瓜巴龙；日志 `.local/guaba-dragon-review/wiki-build.log`。
- `python3 scripts/deploy-local.py`：clean、compat 均完整安装成功；回执 `.local/hmcl/deployments/20261102-160548-779352/receipt.json` 为 `installed`。两个实例 JAR 与被测发行包 SHA256 均为 `f7daa0dcf74a1e70fb7b1e5692699f7936a5535363397a9931431b544b8e1528`，复核见 `.local/guaba-dragon-review/deployment-check.json`。
- `python3 scripts/hot-sync.py`：已同步两个实例资源包及各自已有测试世界的数据包，记录 `.local/hmcl/hot-sync/20261102-160627-790507`。首次与部署同时执行被互斥锁安全拒绝；部署退出后重跑成功。正式 JAR 验收前可 `python3 scripts/hot-sync.py remove` 移除受管覆盖包。

未启动 Minecraft 图形客户端；模型、名称牌、动画衔接、粒子起点与视觉范围、长期自然刷新频率待用户手动验收。无头刷怪条目/谓词验证不等于自然野外遭遇验收；粒子发送代码运行不等于客户端粒子视觉通过。Blockbench 编辑器导入仍未完成，保留设计阶段插件安装待授权边界。未提交、推送或发布；Wiki 源文件和本地构建完成，不代表 Pages 上线。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 需要实体/属性/蛋/渲染/AI/群系数据与运行验证；未推进检查点 |
| 1.19.1 Forge | 待适配/验证 | 需独立 GeckoLib 3 动画与加载器 API 适配；未推进检查点 |
