# 2026-10-25：粉红妹 NPC 与刷怪蛋

- 变更 ID：2026-10-25-pinkki-npc
- 需求与完成行为：在 1.20.1 Forge 实装人形 NPC `pinkkicore:pinkki`（粉红妹）。直接采用 `designs/pinkki-skin/` 已确认的 v5 64×64 Slim 皮肤，不重画五官、服装或附加几何。20 生命、基础攻击 2、移动速度属性 0.25、碰撞箱 0.6×1.8；默认持久化、不因远离玩家消失。只注册刷怪蛋与指令召唤，不自然生成，不添加交易、对话、任务、繁殖等应用玩法。
- AI：漂浮、避免水域的随机游荡、观察玩家与随机张望；HurtByTargetGoal 仅对伤害来源近战反击，不主动索敌、不召集同伴。附近 8 格内有视线且可替换当前装备的掉落物时主动走近，战斗优先；最后的拾取与装备替换由原版 Mob 实现。装备搜索采用 tick 截止时间，避免与 GoalSelector 调度周期的奇偶性锁死；路径目标精度为 0 格，避免默认 1 格容差让 NPC 停在拾取范围外。
- 装备：使用原版主手、副手、头、胸、腿、脚六槽和 NBT；武器增加攻击属性，护甲提供防御。拾取范围包括 ArmorItem、SwordItem、AxeItem、BowItem、CrossbowItem、TridentItem、ShieldItem 及对应子类，不捡普通杂物；不额外实现背包。遵循原版更换装备、绑定诅咒、拾取延迟与 Forge MobGriefing 拾取钩子。关闭 mobGriefing 时不搜索/拾取。拾取装备采用原版保证死亡掉落机制，消失诅咒仍由原版处理。弓弩和三叉戟仅装备，本阶段没有远程 AI、主动举盾或鞘翅飞行。
- 渲染与模型选择：使用 PlayerModel 的 Slim 几何、64×64 皮肤基础及覆盖层、原版行走/观察/攻击姿态；提供手持物、HumanoidArmorLayer、CustomHeadLayer、ElytraLayer。可通过命令使用所有装备槽，鞘翅显示不代表会飞。左右持物姿势遵循主手方向。原版模型能直接复用现有标准皮肤和装备层，因此本轮不另造 GeckoLib 骨骼/动画；保留现有可编辑 `.bbmodel`，Blockbench 内拖动旋转审阅十二个部件。不修改 GeckoLib 依赖。
- 刷怪蛋：`pinkkicore:pinkki_spawn_egg`，原版模板，浅粉 `#F4BFD0` 底色、薄荷 `#CFF4EB` 斑点，创造模式刷怪蛋分类，中英文名称齐全；空实体战利品表，不新增专属掉落。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/entity/PinkkiEntity.java`、`ModEntities.java`、`client/PinkkiRenderer.java`、`ClientEvents.java`、`item/ModItems.java`；目标资源 `textures/entity/pinkki.png`、刷怪蛋模型、中英文语言和实体掉落表。实体与 AI 不引用客户端类。发行包校验增加皮肤逐字节一致性、实体/渲染类、刷怪蛋/语言/掉落表及测试类不泄漏检查。
- Git 参照：开始时 HEAD `d76ae8539fb06dc763f1ab8ae6ed1bf0d925336e`。保留同期工作区中的瓜巴材料兼容修改，不替它们下验证结论；本次未提交或推送。
- 验收场景：执行 `/summon pinkkicore:pinkki ~ ~ ~`，或 `/give @s pinkkicore:pinkki_spawn_egg` 对方块使用。确认 Slim 外观、皮肤覆盖层和游荡步态；投掷剑、盾和四件护甲后确认走近、拾取及可见持物/穿戴；换更高等级装备并观察属性/防御；退出重进确认装备仍在。生存模式攻击后应追击反击，未受击不主动攻击；创造/旁观目标遵循原版。弓弩不应射击，盾牌不应主动格挡。关闭 mobGriefing 时不拾取；有拾取延迟时等待延迟结束。客户端步骤仅供用户自行验收，本次不启动 Minecraft 客户端。
- 无头验证：`./scripts/mod.sh 1.20.1 build pinkkiProbeJar -PpinkkiProbe --console=plain`；`python3 scripts/verify-artifact.py 1.20.1`；`python3 scripts/check-pinkki.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar`。独立探针仅进入测试 JAR，使用已确认 EULA 的本地 Forge 47.4.28 安装，新建一次性世界、绑定 `127.0.0.1:25583`、`--nogui`，明确 PASS/FAIL 后正常停服。检查实际指令与刷怪蛋、默认属性/持久化、实际寻路拾取、六槽装备、武器/护甲实际属性、忽略杂物、NBT 往返、择优替换、mobGriefing、拾取延迟、受击反击、死亡掉装和自主随机游荡。最终结果与部署回执在下文追加。
- 资产审阅：PNG 64×64、两份 Blockbench 内嵌纹理与运行纹理逐字节一致；在真实 Blockbench 网页版打开本地 `pinkki-review.bbmodel`，检查正面、侧面、背面并实际拖动旋转。证据 `.local/pinkki-npc-review/front.png`、`back.png`。旧分享链接已失效，未重新上传云快照。
- 手册与 Wiki：同步 `wiki/bosses.md#pinkki`、`wiki/items.md#pinkki-spawn-egg`、玩家更新记录。自研手册系统仍未实现，稳定条目 ID `pinkki` 与 `pinkki-spawn-egg` 留待接入，当前无可用的游戏内阅读入口；不宣称手册同步完成。Wiki 安装锁定依赖并本地构建；未推送或部署 GitHub Pages。
- 未验证边界：无头服务端不证明客户端渲染、护甲/覆盖层贴合、持物挥动、客户端交互、多人画面同步、整合包完整行为及真实退出重启后的存档恢复。NBT 往返仅证明序列化；没有启动或操作游戏。
- 移植注意：注册、属性、AI、装备 API、刷怪蛋及 PlayerModel/装备层均在版本适配层；次要版本需按各自映射和渲染 API 适配，不能复制后直接声明兼容，不推进适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 实体、AI、装备、渲染层和资源需按目标 API 移植 |
| 1.19.1 Forge | 待适配/验证 | 实体、AI、装备、渲染层和资源需按目标 API 移植 |

## 本次实际验证结果

- 构建与独立探针编译通过：`.local/pinkki-build.log`。最终发行包校验通过：`.local/pinkki-artifact.log`，SHA256 `983e70471d931aebf53027163045d48d6c912581aa7fffa8dc838bfd8213d812`。
- 最终发行 JAR 的完整无头探针通过：`.local/pinkki-tests/1792920337261804000/console.log` 与 `result.json`。全部行为断言通过，服务端就绪、正常停服、退出码 0、无 ERROR；武器伤害属性和护甲防御实际生效。此前失败测试用于定位调度、场景阻挡及终点精度问题，不作为成功证据。
- 皮肤 PNG、两份可编辑工程内嵌纹理和运行纹理一致性通过；Python AST 语法检查、`git diff --check`、`npm ci --cache .local/npm-cache` 与 `npm run wiki:build` 通过。Wiki 提示 mcfunction 语法高亮回退为纯文本，不影响页面生成。
- 完整部署：`python3 scripts/deploy-local.py` 成功，回执 `.local/hmcl/deployments/20261025-172645-933861/receipt.json` 为 `installed`，两个实例的 PinkkiCore JAR SHA256 均与上述最终测试发行 JAR 一致。构建/校验日志和旧 JAR 备份由部署脚本保留。未启动游戏。
- 已执行 `python3 scripts/hot-sync.py`，同步 clean、compat 的当前受管资源覆盖包及所有已发现测试世界数据包，避免旧覆盖资源遮住新 JAR；记录 `.local/pinkki-hot-sync.log`。新注册与 Java 代码仍需启动新游戏进程，F3+T / `/reload` 仅重载资源/数据；正式纯 JAR 客户端验收前需 `python3 scripts/hot-sync.py remove` 并重新加载。
