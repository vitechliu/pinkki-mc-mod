# 2026-10-26：雷霆瓜巴效果与酿造

- 变更 ID：2026-10-26-thunder-guaba-potions。
- 需求与完成行为：为雷霆瓜巴道具新增同名中性状态；沿用普通瓜巴的四级、每 20 刻独立随机属性、正负对称规则，同级修正幅度为普通瓜巴的 150%。I–IV 移速修正为 ±75%、±97.5%、±120%、±142.5%，护甲修正 ±12、±24、±36、±48。原版最终属性上下限仍生效，高等级强减速可令移速降至 0；不把负移速当作倒退、不添加额外易伤。
- 雷击规则：每个实体独立抽取 100–200 服务端刻（含端点）后，在当时所在位置生成一只普通原版 LightningBolt，首次也等待此间隔。落雷后重新抽取；不以状态等级改变雷击参数或频率。是真实雷击而非特效，正常影响附近实体、引火、原版雷击变种和瓜巴掉落物转换。测试目录使用关闭火蔓延的本地规则，不改变正式游戏规则。
- 生命周期：雷击时钟与属性重抽时钟分离；升级、刷新、存档重载保留原时钟，不靠刷新延后雷击。MobEffectEvent.Remove/Expired 清理实际移除或过期的雷击状态；取消移除的事件保持原行为。普通瓜巴和雷霆瓜巴使用不同修饰符 UUID、不同计时键，可同时存在，移除一个不破坏另一个。
- 酿造：粗制的药水 + 雷霆瓜巴 → 雷霆瓜巴 I（3:00）；I + 红石粉 → 延长 I（8:00）；I + 荧石粉 → II（1:30）。延长与增强不叠加，普通渠道无 III/IV。支持普通/喷溅/滞留与原版火药/龙息转换。以标准 BrewingRecipe 注册九条新配方，普通瓜巴加雷霆瓜巴合计 18 条 JEI 可枚举配方；不新增 JEI 强制依赖。
- 源码位置：`versions/1.20.1/.../forge/effect/{GuabaEffect,GuabaPotions,ThunderGuabaEffect}.java`；统一定义 `data/pinkkicore/guaba_potions.json` 中的 `thunder`，复用普通四级范围并乘倍率，不维护第二套范围。新增状态图标复用现有雷霆瓜巴 PNG；中英文本地化包含药水、喷溅、滞留与药箭。
- 玩家文档和自研手册：`wiki/guaba-potions.md#thunder-guaba`、道具用途、玩家更新；`GuabaBrewing.vue` 复用定义显示新酿造图；`scripts/generate-guide.py` 为属性和酿造块保留效果 ID，`GuideScreen.java` 按对应 ID 展示数值/当前注册配方，防止普通和雷霆两段混用或重复。手册正文随 Wiki 编译进 JAR，离线可读；未执行客户端阅读实机验收。
- Git 参照：开始沿用当前 HEAD；保留工作区已有雷击转换 JEI、瓜巴对称调整及相关文档等改动，未提交、推送、发布或部署真实服务端。
- 验收场景：普通与雷霆合计 18 条标准酿造配方候选；雷霆三个瓶型基础/延长/增强与禁止越级/叠加；四级各 400 次固定种子样本检查正负范围和 1.5 倍幅度；普通/雷霆修饰符互不删除；升级/刷新/保存重载时钟不后移；真实 I 和 IV 各落雷，间隔 100–200 刻并发生实际命中/伤害；移除和自然过期后停止生成新雷；全部普通瓜巴回归继续通过。
- 验证与部署：见下方实际执行记录。只处理 1.20.1，不更新次要目标适配检查点。
- 未验证范围：未启动 Minecraft 图形客户端；未实机验证粒子、雷声、状态图标、手册界面、JEI 点击查询、多人同步和其他整合包对雷击的拦截。无头服务端实际命中不等于上述客户端行为已验收。未推送 Wiki，未确认 Pages 线上部署。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐注册、属性、雷击事件与 JEI 平台 API |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 本轮执行记录

- Git 开始参照：`c451415`。
- 构建：`./scripts/mod.sh 1.20.1 build guabaEffectProbeJar -PguabaEffectProbe` 成功，日志 `.local/thunder-guaba-probe-build.log`。
- 发行包：`python3 scripts/verify-artifact.py 1.20.1` 通过，SHA256 `8e62db23d63df331fb7fabccaedf4c7a3c3d7a7c0a57f6a7fcfc76d164831f6b`。包含新效果类、定义/倍率/间隔、三个药水时长、状态图标和双语本地化检查；正式包不含测试探针。
- 无头实际运行：`python3 scripts/check-guaba-effect.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar` 通过。Forge 47.4.28 / GeckoLib 4.4.9 独立回环服务端，最终日志 `.local/thunder-guaba-runtime.log`；`.local/guaba-effect-tests/1793005612182078000/result.json` 为 passed/ready/probe_pass/stopped=true、exit_code=0、errors=false。包含雷霆四级各 400 次采样、幅度到达 150% 的边界附近、负/正抽样、与普通效果共存、升级/刷新/保存重载时钟、真实 I/IV 雷击间隔和伤害、移除及自然过期后不再落雷；普通瓜巴全部回归通过。
- 手册/Wiki：`python3 -m unittest discover -s scripts/tests -p test_guide.py` 四项通过，额外确认两种效果的属性/酿造块 ID 都在入包手册中，手册正文与 Wiki 一致。`npm ci --cache .local/npm-cache --offline`、`npm run wiki:build` 通过，日志 `.local/thunder-guaba-npm-ci.log`、`.local/thunder-guaba-wiki.log`。Python 语法检查及 `git diff --check` 通过。
- 修复过程记录：首轮探针发现清除效果时的属性回调仍能看到活动效果，不能据此区分升级与真正移除。改为 Forge Remove/Expired 生命周期事件清理后，最终回归通过；未沿用失败运行的结果作为成功证明。
- 完整本地部署：`python3 scripts/deploy-local.py` 成功；`.local/hmcl/deployments/20261026-170750-582432/receipt.json` 为 installed，两个实例均有回执。额外计算 clean、compat 的 mods 内 PinkkiCore SHA256，均与上述已测发行包一致；保留第三方 mod、配置、存档，未启动游戏。
- 部署结束后 `python3 scripts/hot-sync.py` 成功，同步两个实例资源与已有测试世界受管数据包；日志 `.local/thunder-guaba-hot-sync.log`。既有资源可按 F3+T、`/reload` 重载；新增 Java 效果和启动读取定义需要从新 JAR 启动生效，不能用热重载替代完整安装。正式发行 JAR 独立验收前按项目流程移除开发覆盖包。

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
