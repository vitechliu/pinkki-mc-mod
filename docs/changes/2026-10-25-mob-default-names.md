# 2026-10-25：粉红妹与饼蛙默认命名

- 变更 ID：2026-10-25-mob-default-names
- 需求与完成行为：1.20.1 的粉红妹与饼蛙创建时即设置实体翻译名称为 CustomName，并启用 CustomNameVisible，使用原版命名牌生物的头顶名称显示机制，不新增客户端 HUD。名称随客户端语言翻译。指令指定名称和命名牌改名继续可用，名称与可见性通过原版 NBT 保存。
- 旧存档：读取没有 CustomName 的旧实体时补上默认名字并显示；已有自定义名称保留，已有明确的 CustomNameVisible 设置保留；自定义名称但缺少可见性字段的指令召唤默认显示。不会每 tick 覆盖命名牌改名。旧未命名实体重新加载时生效。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/entity/PinkkiEntity.java`、`BingwaEntity.java`；回归验证位于 `scripts/tests/pinkki_probe/PinkkiProbe.java`。
- Git 参照：开始时 HEAD `d76ae8539fb06dc763f1ab8ae6ed1bf0d925336e`，保留当前工作区已有修改。
- 验收场景：刷怪蛋/普通 summon 生成两种生物后均有头顶默认名称，不需命名牌；分别用命名牌改名后名称变化、保存读取仍保持；自定义名称 summon 不被默认名称覆盖；旧无名实体加载后补名。显示仍受原版距离、隐身和客户端名称渲染规则控制，并非隔无限距离可见。
- 1.20.1 验证：构建、发行 JAR 校验、独立无头探针与 Wiki 构建结果在下方记录。探针覆盖两种实体默认名称/可见性、NBT 往返、旧无名实体迁移、指令自定义名称保留、显式隐藏保留、实际 NameTagItem 改名及改名存档；同时复测粉红妹实际刷怪蛋/指令、AI 与装备行为。无头测试不能证明客户端文字位置、遮挡及显示观感，未启动游戏。
- 手册与 Wiki：同步 `wiki/bosses.md` 与玩家更新记录；自研手册尚未实现，`pinkki`、`bingwa` 条目未来接入时需包含此行为，当前不存在可用手册入口。未推送或发布 Wiki。
- 移植注意：实现仅位于 1.20.1 适配层，使用原版 Entity 名称同步与存档 API。其他版本按需适配，不推进版本检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 两种实体及名称初始化/存档读取需按目标 API 适配 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 实际验证结果

- `./scripts/mod.sh 1.20.1 build pinkkiProbeJar -PpinkkiProbe --console=plain` 通过，日志 `.local/mob-names-build.log`；`python3 scripts/verify-artifact.py 1.20.1` 通过，日志 `.local/mob-names-artifact.log`。
- 独立发行 JAR 探针通过，证据 `.local/pinkki-tests/1792923716141018000/console.log`、`result.json`：两种实体默认名称、旧存档补名、名称/可见性存档、保留自定义名称及显式隐藏、实际命名牌改名及改名存档全部通过；粉红妹的既有召唤、刷怪蛋、拾取/换装、属性、反击、死亡掉装、随机游荡回归通过。服务端就绪、正常停服、退出码 0，未启动客户端。发行 JAR SHA256 `0a1eb5663d424278662b05b97e651c253173da61dceee84b7f1f4a17cabd517b`。
- `npm run wiki:build` 与 `git diff --check` 通过，Wiki 日志 `.local/mob-names-wiki.log`。已执行 `python3 scripts/hot-sync.py` 更新两个实例受管覆盖包及测试世界数据包，日志 `.local/mob-names-hot-sync.log`；默认命名属于 Java 逻辑，热同步不能使它在已运行的游戏中生效，仍需完整 JAR 部署并重新启动游戏。
- 完整部署通过：`.local/hmcl/deployments/20261025-182303-935820/receipt.json` 状态为 `installed`；clean、compat 两个实际安装 JAR 的 SHA256 均与上述测试发行包一致。未启动游戏，客户端显示仍待人工验收。

## 2026-10-26 工作区整理复验

- 重新构建 1.20.1 发行包与 `pinkkiProbeJar -PpinkkiProbe`、运行发行包校验通过；发行 SHA256 仍为 `0a1eb5663d424278662b05b97e651c253173da61dceee84b7f1f4a17cabd517b`，日志 `.local/tidy-20261026-build.log`。未构建次要版本。
- 粉红妹/饼蛙命名与粉红妹完整行为探针通过，证据 `.local/pinkki-tests/1792981586283507000/result.json`、`console.log`，汇总 `.local/tidy-20261026-pinkki.log`。默认名、旧 NBT 迁移、自定义名称/可见性、真实命名牌、召唤与刷怪蛋、寻路拾取、六槽装备与属性、保存、替换、mobGriefing、拾取延迟、反击、掉装及随机游荡通过；正常停服、退出码 0、无 ERROR。
- 瓜巴之触在无可选模组、Silent Gear、匠魂、Tetra、三者共存五组全部通过；实际攻击、对照、目标限定、固定等级/时间、刷新和更强状态保留通过。证据 `.local/guaba-compat-tests/1792981596564979000/matrix-results.json` 与各组日志，汇总 `.local/tidy-20261026-compat.log`。五组使用同一发行 SHA256，正常停服，无 ERROR 和公钥下载警告。
- `npm run wiki:build`、33 项 Python 回归、相关 Python AST/JSON 解析、建筑审阅工具 JavaScript 语法、纹理源文件/运行资源/两份模型内嵌纹理一致性、文本凭证/本机路径扫描及差异空白检查通过。日志 `.local/tidy-20261026-wiki.log`、`.local/tidy-20261026-tests.log`。建筑提取未重跑，未沿用历史扫描数据作为本次验证。
- 本次已执行完整部署，但 `.local/hmcl/deployments/20261026-102849-214534/receipt.json` 为 failed、instances=[]：目标游戏仍在运行，脚本在安装前阻止操作；本次 clean、compat 均未更新 JAR。构建完成、部署待退出游戏，未关闭游戏或绕过保护。日志 `.local/tidy-20261026-deploy.log`。
- 热同步成功，回执 `.local/hmcl/hot-sync/20261026-102853-018426/receipt.json` 为 ok，两个实例资源包及所有已有测试世界数据包均已处理，日志 `.local/tidy-20261026-hot-sync.log`。资源/数据需由玩家 F3+T、`/reload`；Java 修改仍须退出游戏后完整部署，不能凭热同步报告已生效。
- 现有忽略规则足够，未修改；缓存、日志、实例、建筑审阅产物、回执和备份均不提交。手册接入缺口保留。未启动客户端、未验证客户端画面、远端 CI/Pages 或真实服务端。

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
