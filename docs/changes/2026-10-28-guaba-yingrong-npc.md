# 2026-10-28：瓜巴影融 NPC

- 变更 ID：2026-10-28-guaba-yingrong-npc
- 需求与完成行为：以工作区 `designs/guaba_yingrong/` 已确认 v2 的标准 Alex/Slim 皮肤为资产，接入 `pinkkicore:guaba_yingrong`。保持 64×64 PNG 和所有五官/衣装像素不变，沿用原版人形几何和基础/覆盖层。新增深褐底色 `#302C21`、金色斑点 `#CBA45A` 的刷怪蛋 `pinkkicore:guaba_yingrong_spawn_egg`，创造刷怪蛋分类、语言和空实体掉落表齐全。
- 与粉红妹一致：20 生命、基础攻击 2、移动速度属性 0.25、碰撞箱 0.6×1.8；默认持久化、出生显示可翻译名字、支持命名牌和指令改名及 NBT 保存。中立、随机游荡、观察玩家、受伤后近战反击，不主动索敌/召集同伴；搜索附近武器、盾牌、护甲并走近拾取，按原版规则替换装备，主副手和四个护甲槽均有效，拾取装备死亡掉落，诅咒仍遵循原版。遵循 mobGriefing 和拾取延迟。弓弩/三叉戟仅装备，无远程 AI、主动格挡、交易、对话、任务、自然生成或图中道具/能量特效。
- 共用实现：将原粉红妹实体和渲染行为原样提取至 `HumanoidNpcEntity` 与客户端 `HumanoidNpcRenderer<T>`；粉红妹与瓜巴影融各自保留独立实体类型和渲染入口。Slim 的 PlayerModel、手持、护甲、头部物品和鞘翅显示层共用；没有新关键帧动画文件，行走/观察/挥手由原版模型实时计算。实体类不依赖客户端类。此次有第二个相同行为的实际需求，因此共享行为而非复制一套 AI。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/entity/{HumanoidNpcEntity,PinkkiEntity,GuabaYingrongEntity,ModEntities}.java`、`client/{HumanoidNpcRenderer,PinkkiRenderer,GuabaYingrongRenderer,ClientEvents}.java`、`item/ModItems.java`、目标资源中的皮肤/刷怪蛋/语言/掉落表；设计源文件保留在 `designs/guaba_yingrong/`。
- Git 参照：开始时 HEAD `5b5fee5693aa656d3e8f3f2422772eeb04c39fe4`，已有未跟踪的瓜巴影融资产全部保留，不提交或推送。
- 验收场景：`/summon pinkkicore:guaba_yingrong ~ ~ ~` 或 `/give @s pinkkicore:guaba_yingrong_spawn_egg`；检查名字、最终皮肤（黑瞳位于下排内侧）、兜帽与衣摆覆盖层；观察游荡，投掷剑盾和四件护甲检查走近、穿戴和换装；未受伤不主动攻击，受伤反击；装备和改名保存读取保持；粉红妹同样回归。客户端渲染、护甲贴合、持物动作和完整退出/重进存档需用户人工验收，本次不启动游戏。
- 验证工具：现有 `scripts/tests/pinkki_probe/PinkkiProbe.java` 和 `scripts/check-pinkki.py` 增加 `--npc guaba_yingrong|pinkki` 选择，实际召唤与刷怪蛋结果校验类型，分别在独立世界运行整套行为断言；命名回归覆盖当前三个生物。探针不进入发行 JAR。使用已确认 EULA 的 `.local/package-test/1.20.1`，仅回环 `127.0.0.1:25583`，`--nogui`，结束正常停服。结果中 `npc` 标识实际被测对象，兼容保留原 `PINKKI_PROBE` 成功标记。
- 外部服务边界：首次测试所有行为断言通过，但 Mojang 公钥服务 TLS 握手失败产生 ERROR，旧脚本判失败。仅将精确的 `Yggdrasil Key Fetcher/ERROR` 公钥获取失败单独记为 `external_auth_unavailable`，因为测试为无人登录的回环离线服；其他 ERROR 和任何探针失败仍判失败，完整堆栈仍保留。不代表在线认证通过；后续完整重跑见结果。
- 手册与 Wiki：同步 `wiki/bosses.md#guaba-yingrong`、刷怪蛋、玩家更新记录和 README；现有自研游戏手册通过 `generateGuide` 从 Wiki 入册页面生成正文，新增实体及刷怪蛋随 `bosses#guaba-yingrong` / `items#guaba-yingrong-spawn-egg` 自动进入离线手册。玩家页不放开发验证过程。本次不发布 Wiki。
- 移植注意：只改 1.20.1 Forge，实体注册、渲染层和原版 API 需按目标版本另行适配；不更新适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 实体、注册、AI、装备、渲染及资源需按目标 API 移植 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 实际验证

- `python3 designs/guaba_yingrong/verify_skin.py` 通过，两份可编辑模型内嵌贴图、PNG/CRC、基础层不透明、UV 和十二个部件通过。源 PNG SHA256 `9f8134e3258d62aa5a2e1179b094184bc8f98232454f709996324d540be2fd9e`。未重新生成或修改像素。
- `./scripts/mod.sh 1.20.1 build pinkkiProbeJar -PpinkkiProbe --console=plain` 构建成功，`.local/yingrong-build.log`；发行包校验 `.local/yingrong-artifact.log` 通过，发行 JAR SHA256 `7b3bfcd2c37c05998897974c83f2cdb089fd67d9d22938f07f3c97c15ed6fbb2`。
- 瓜巴影融完整无头测试通过：`.local/guaba_yingrong-tests/1793173700327336000/{console.log,result.json}`；名称/改名、指令/刷怪蛋、属性、实际寻路与拾取、六槽装备及属性、择优替换、忽略杂物、存档、mobGriefing、拾取延迟、反击、死亡掉装和自主游荡均通过。服务端就绪、正常停服、退出码 0、无 ERROR、`external_auth_unavailable=false`。
- `npm ci --cache .local/npm-cache` 与 `npm run wiki:build` 通过，日志 `.local/yingrong-wiki-install.log`、`.local/yingrong-wiki.log`。本地构建不代表 GitHub Pages 更新。
- 粉红妹同包完整回归通过：`.local/pinkki-tests/1793173777475864000/{console.log,result.json}`，与瓜巴影融测试为同一发行 JAR；正常停服、退出码 0、无 ERROR、`external_auth_unavailable=false`。
- Blockbench 实际打开最终 v2 审阅，检查初始正面/斜角、侧面和背面，实际拖动旋转并切换相机菜单；证据 `.local/yingrong-npc-review/{front,side,back}.png`。这证明源模型可审阅，不代表 Minecraft 客户端渲染已验收。
- Python AST 语法检查与 `git diff --check` 通过。未测试远程认证、多人渲染同步、客户端显示和完整退出重启的存档恢复。
- 部署构建重新生成 Wiki 对应的离线手册，因此最终发行 JAR SHA256 更新为 `d819e8c958cbd24099341ec53db9c1d6a21b3b0cbd070560fcae400b8a396f67`；与前一轮测试包逐文件比较，唯一差异是 `assets/pinkkicore/guide/zh_cn.json`，实体/渲染字节码和皮肤完全一致。后续对该最终包重新执行两种 NPC 测试。
- 最终已安装包的瓜巴影融重跑通过：`.local/guaba_yingrong-tests/1793174645285866000/{console.log,result.json}`，SHA256 与上述最终包一致；正常停服、退出码 0、无 ERROR、`external_auth_unavailable=false`。
- 离线手册现有 4 项单元测试通过：`.local/yingrong-guide-tests.log`；实际 JAR 内新增 NPC 与刷怪蛋锚点均已存在，发行校验确认手册全文与当前 Wiki 生成结果一致。
- 共用行为源码与 HEAD 中原粉红妹实现做归一化比较完全一致，仅类名/构造器可见性/内部类宿主名改变。
- 完整部署回执 `.local/hmcl/deployments/20261028-155208-889050/receipt.json` 为 `installed`，两个实例已安装 JAR 的 SHA256 均与最终发行包一致；已热同步当前受管覆盖包和所有测试世界数据包（`.local/yingrong-hot-sync.log`）。新增 Java/注册需重新启动游戏；后续资源/数据可用 F3+T / `/reload`，正式纯 JAR 验收前使用 `hot-sync.py remove` 移除受管覆盖包。
- 最终已安装包的粉红妹回归通过：`.local/pinkki-tests/1793174708100894000/{console.log,result.json}`，与瓜巴影融和两个安装实例的 SHA256 完全一致；正常停服、退出码 0、无 ERROR、`external_auth_unavailable=false`。

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
