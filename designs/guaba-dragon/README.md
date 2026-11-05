# 朱罗纪瓜巴龙

已接入 Minecraft 1.20.1 Forge，实体 ID 为 `pinkkicore:guaba_dragon`，刷怪蛋为 `pinkkicore:guaba_dragon_spawn_egg`。运行资源位于该目标的 `assets/pinkkicore/`。玩家玩法见 [Wiki](../../wiki/bosses.md#guaba-dragon)，验证与部署见[接入记录](../../docs/changes/2026-11-02-guaba-dragon.md)。

## 造型与比例

以用户提供的插画为参考，转译为 Minecraft 长方体和原创像素贴图：无翅双足、短前爪、金色长颈与腹部、红色躯干、火焰状背鬃和分节长尾。按用户反馈，头部以红色为底，蓝色额脊、眉缘、斜向颊部肌肉带和唇缘交替覆盖，露出红色鼻翼、太阳穴与下颌；蓝色鼻梁宽度收窄至 3 单位。背鬃和尾尖火焰是几何装饰，不是发光材质。

16 单位 = 1 格，原点为两脚之间的地面，Y 向上，正面朝 -Z。静态高约 3 格、头尾长约 5 格、躯干宽约 0.9 格。游戏碰撞箱独立设为宽 1.3、高 2.9 格，不按长尾外框生成。

101 个长方体、24 根骨骼、512×512 RGBA 像素贴图，独立 per-face UV。每面按实际 UV 绘制，不使用插画平面投影。

## 骨骼与动作

- `body → neck_base → neck_tip → head → jaw`：颈根、颈尖、头和下颌独立。
- `head → breath_socket`：空挂点位于 `[0,31,-30]`；设计预留。当前服务端粒子使用与正向口部近似的局部位置（前方 1.5 格、高 1.95 格），不在服务端加载客户端骨骼。
- `body → tail_1 → tail_2 → tail_3 → tail_4 → tail_flame`：逐节摆尾。
- 前肢分上臂/手，后肢分大腿/胫/脚；`crest`、`neck_crest` 为装饰骨骼，不是独立生物。

| 动作 | 时长 | 内容 |
| --- | --- | --- |
| `idle` | 3 秒循环 | 小幅呼吸、抬颈、逐节摆尾 |
| `walk` | 1 秒循环 | 后腿交替迈动、前爪反向摆动 |
| `melee` | 0.45 秒单次 | 小幅探颈和咬合；伤害由服务端近战控制 |
| `breath_pose` | 4.8 秒单次 | 0–1 秒蓄力；1–4 秒张口喷射；4–4.8 秒复位 |

动画没有实体世界根位移。审阅器展示同一骨架和关键帧，火焰粒子在游戏服务端产生；伤害、AI、碰撞、位移和同步由版本适配层处理。

## 文件与复现

- `guaba_dragon.bbmodel`：Blockbench 4.10 GeckoLib 工程，内嵌贴图与四段动作。
- `guaba_dragon.geo.json` / `guaba_dragon.animation.json` / `textures/guaba_dragon.png`：与 1.20.1 入包文件一致。
- `review.html`：内嵌真实导出数据的离线 WebGL 审阅器，无 CDN。支持旋转、缩放、四向视角、播放/暂停、时间轴、骨骼和背鬃开关。
- `generate.py` / `review-template.html`：确定性生成源，建模和渲染基础由 `designs/bingwa/` 适配，不修改饼蛙资产。
- `validate.py`：坐标与 UV 往返、PNG 数据与 CRC、骨骼层级、关键帧和审阅数据一致性。
- `check-geckolib.py` / `GeckoAssetCheck.java`：使用本地缓存的最低 GeckoLib 4.2.3 / Forge 1.20.1 解析，不启动游戏。

```bash
python3 designs/guaba-dragon/generate.py
python3 designs/guaba-dragon/validate.py
python3 designs/guaba-dragon/check-geckolib.py
```

生成后将几何、动画和纹理同步到 1.20.1 资源目录；发行校验会拒绝源资产和入包文件不一致。

## 审阅边界

初稿和红蓝头部修订均已在浏览器审阅。日志与截图位于 `.local/guaba-dragon-review/`。四段动作已通过静态校验与 GeckoLib 4.2.3 解析。

Blockbench 编辑器导入尚未完成：先前安装官方商店 GeckoLib 插件被自动审批拒绝，需用户明确授权安装；本次不重试安装。Minecraft 客户端外观、粒子与动画衔接尚未实机验收，其他目标版本未适配/验证。
