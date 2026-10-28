# 2026-10-23：瓜巴状态与酿造

- 变更 ID：2026-10-23-guaba-potions。
- 需求与完成行为：1.20.1 新增有益效果 `pinkkicore:guaba`。获得时立即独立随机移动速度和额外护甲，随后每 20 服务端刻重新抽取；I–IV 移速倍率范围依次为 0.50–1.75、0.65–2.50、0.80–3.25、1.00–4.00，额外护甲依次为 0–8、2–16、4–24、6–32。上下限随等级提高；指令异常高等级的实际属性按 IV 封顶。服务端使用固定 UUID 的临时修饰符，不改基础属性，不积累旧值；效果消失时移除自己的修饰符，保留其他效果和装备。
- 酿造：粗制的药水 + 瓜巴 → 瓜巴 I（3:00）；I + 红石粉 → 延长 I（8:00）；I + 荧石粉 → II（1:30）。延长与增强不可叠加，无 III/IV 药水或普通酿造路径。三个瓶型均可酿造、延长、增强；原版火药/龙息转换及滞留时间缩放保留。效果存活期间的快照不持久化属性值，重载后恢复随机修饰符。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/effect/`、`PinkkiCore.java`；定义 `data/pinkkicore/guaba_potions.json`；双语语言、`textures/mob_effect/guaba.png`（复用现有瓜巴图标）。定义文件随 JAR 打包，由 Java 和 Wiki 共用，不是可 `/reload` 的配方类型。新增探针 `scripts/tests/guaba_effect_probe/`、运行脚本 `scripts/check-guaba-effect.py`、Gradle 可选 `guabaEffectProbe` 源集；探针不打入正式 mod。
- Git 参照：开始 HEAD `6bd0ab0bbc6ece653f762079491f32fbb6c6bd9f`。保留工作区既有 HMCL/热同步/皮肤等修改；未提交、推送、发布或部署 JAR。
- 验收场景：三个瓶型的基础酿造/延长/增强；水瓶拒绝；延长/增强不能继续强化；各药水火药/龙息转换；原版迅捷酿造不回归；真实酿造台三瓶同时完成并消耗燃料/材料；I–IV 各 400 次随机边界；高等级按 IV 封顶；重复更新只有一份修饰符；存档保存/恢复无累积；移除效果保留其他修饰符；真实服务端 20 刻间隔、间隔内稳定、自然过期还原。
- 1.20.1 验证：`./scripts/mod.sh 1.20.1 build guabaEffectProbeJar -PguabaEffectProbe` 成功，日志 `.local/guaba-effect-probe-build.log`。`python3 scripts/verify-artifact.py 1.20.1` 成功，SHA256 `036232ee47f312a6db6cd2e33f86b32d2d79abb3ec27e4adc49f1dbd43765739`。发行 JAR 在 Forge 47.4.28 / GeckoLib 4.4.9 独立无头服务端运行探针成功，执行 `python3 scripts/check-guaba-effect.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar`；日志 `.local/guaba-effect-runtime.log` 与 `.local/guaba-effect-tests/*/console.log`，回执 `result.json` 的 passed/ready/probe_pass/stopped 均 true、exit_code=0、errors=false。
- Wiki 与手册：新增 `wiki/guaba-potions.md`，同步道具用途、配方图、导航与玩家更新。`GuabaBrewing.vue` 使用同一份游戏定义与原版真实图标绘制酿造台示意；数值表也读取定义。`npm ci --cache .local/npm-cache --offline`、`npm run wiki:build` 成功，日志 `.local/guaba-effect-npm-ci.log`、`.local/guaba-effect-wiki-build.log`。图标取自本地原版 1.20.1 资源，不属于 MIT 素材。自研手册基础能力尚未实现，无法接入实际游戏内阅读；后续需以稳定条目 ID `guaba_potions` 接入此定义及正文，不能将 Wiki 同步当作手册完成。
- 热同步：`python3 scripts/hot-sync.py` 成功，同步 clean/compat 资源与全部已存在测试世界的数据覆盖包；日志 `.local/guaba-effect-hot-sync.log`。可按 F3+T、执行 `/reload` 重载已有资源/数据；本功能新增 Java 注册，需要退出游戏后完整部署 JAR，热同步不会使旧 JAR 获得新效果/酿造。定义由 Java 启动时读取，修改它也需要重新构建部署。发行 JAR 探针使用独立目录，未加载 HMCL 覆盖包。
- 未验证范围：未启动图形客户端，未验证 HUD/药水本地化实际显示、真实多人同步、饮用/喷溅投掷/滞留云交互、Wiki 浏览器布局与 Pages 线上部署。探针验证瓶型酿造路径，但不将此当作客户端交互验收。后续可用 `/effect give @s pinkkicore:guaba 60 3` 检查 IV；III/IV 使用管理员指令并非普通获取。
- 移植注意：仅 1.20.1 Forge；状态生命周期、属性 UUID/API、酿造注册与药水注册应按目标版本核对。共享核心未改，不更新适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
