# 2026-10-26：瓜巴效果正负对称波动

- 变更 ID：2026-10-26-guaba-symmetric-fluctuation。
- 需求与完成行为：瓜巴 I–IV 的移动速度修正改为 ±50%、±65%、±80%、±95%（倍率 0.50–1.50、0.35–1.65、0.20–1.80、0.05–1.95）；护甲修正改为 ±8、±16、±24、±32。均匀抽样中心分别固定在倍率 1 和护甲 0，升级仅扩大波动，不再提高修正均值。每秒独立重抽、等级封顶、效果移除和酿造规则沿用现有行为；效果分类由有益改为中性。
- 原版边界：护甲修正允许负数，降低装备/基础护甲，保留原版最终护甲上下限；零护甲生物不会获得负护甲或额外易伤。因此对称性针对抽样修正值，不承诺不同护甲基数的最终减伤收益完全对称。不新增伤害倍率或绕过原版属性限制。移速最低倍率 5%，避免负倍率/截零破坏抽样对称性。
- 源码位置：`versions/1.20.1/src/main/resources/data/pinkkicore/guaba_potions.json`、`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/effect/GuabaEffect.java`；回归探针 `scripts/tests/guaba_effect_probe/GuabaEffectProbe.java`；产物检查 `scripts/verify-artifact.py`。
- Git 参照：开始 HEAD `c451415`。保留已有雷击 JEI、语言、兼容 Wiki 等修改，不改变其实现；本轮未提交、推送或发布。
- 验收场景：四级各 400 次固定种子抽样，正负移速/护甲均有足够样本且在对应边界；定义的上下限关于 1/0 对称，幅度逐级增大；20 点基础护甲遇负修正确实减少、移除效果恢复 20；无护甲时负修正遵守 0 下限；仍每 20 刻重抽，间隔内修正稳定；效果过期、重载、其他效果修饰符及三个瓶型酿造不回归。计时探针读取原始护甲修正，避免连续负修正在最终属性均为 0 时误判没有重抽。
- 玩家文档：`wiki/guaba-potions.md` 复用同一份定义展示新范围，说明全部等级均可能减益及原版护甲下限；`wiki/changelog.md` 添加玩家更新。现有独立手册从同份 Wiki 生成正文，属性图读取相同定义；本次构建与产物校验核对入包内容，客户端阅读仍待人工验收。
- 验证与部署：见下方本轮执行记录。仅验证/部署 1.20.1，不更新其他目标检查点。
- 未验证范围：未启动 Minecraft 图形客户端；客户端状态分类显示、HUD、真实多人同步未实机验收。本地 Wiki 构建不代表 Pages 部署，本地安装不代表客户端已启动使用。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 本轮执行记录

- 构建：`./scripts/mod.sh 1.20.1 build guabaEffectProbeJar -PguabaEffectProbe` 成功；日志 `.local/guaba-symmetric-build.log`。
- 产物校验：`python3 scripts/verify-artifact.py 1.20.1` 通过，包括四级定义对称、幅度递增与酿造定义检查；SHA256 `f3931e06e2fb3162c675b2d51ce348012a0bbda4a05e5a71f5007ca63726692a`。
- 无头发行 JAR：`python3 scripts/check-guaba-effect.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar` 通过，包含新增对称/负护甲/中性分类检查及全部原有回归。日志 `.local/guaba-symmetric-runtime.log`；`.local/guaba-effect-tests/1793003620345241000/result.json` 为 passed/ready/probe_pass/stopped=true、exit_code=0、errors=false。
- Wiki：`npm ci --cache .local/npm-cache --offline`、`npm run wiki:build` 通过；日志 `.local/guaba-symmetric-npm-ci.log`、`.local/guaba-symmetric-wiki.log`；`git diff --check` 通过。
- 完整部署：`python3 scripts/deploy-local.py` 成功；本次 `.local/hmcl/deployments/20261026-163822-295367/receipt.json` 状态 installed，两个实例均有安装回执。另行重新计算 clean、compat 的 mods 内 PinkkiCore SHA256，两者与本次测试发行 JAR 完全一致，均为上述 SHA256。其他 mod、配置与存档保留，未启动游戏。
- 热同步：首次与部署锁冲突而未执行；部署结束后重新执行 `python3 scripts/hot-sync.py` 成功，日志 `.local/guaba-symmetric-hot-sync.log`。同步两个实例与已有测试世界的受管覆盖资源/数据。已有游戏内资源可按 F3+T、`/reload` 重载；效果分类与启动读取的范围定义需从新 JAR 启动生效，热重载不能替代重新启动。正式 JAR 单独验收前移除受管覆盖包。

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
