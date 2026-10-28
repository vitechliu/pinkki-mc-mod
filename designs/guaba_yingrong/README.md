# guaba_yingrong 人形皮肤

依据用户于 2026-10-28 提供的角色参考图制作。本目录保留最终 v2 设计源资产；2026-10-28 已接入 1.20.1 Forge 的瓜巴影融 NPC。

## 交付文件

- `guaba_yingrong.png`：64×64 RGBA 标准 Java 皮肤，Alex / Slim（3 像素手臂）。
- `guaba_yingrong.bbmodel`：Minecraft Skin 源工程，内嵌同一 PNG。Blockbench 若隐藏覆盖层，使用 Skin → Toggle Skin Layer。
- `guaba_yingrong-review.bbmodel`：标准 Alex 实际几何的独立审阅副本，12 个身体/覆盖层部件全部显示，内嵌同一 PNG。
- `create_skin.py`：确定性 UV 像素绘制，生成 PNG 并更新两个工程的内嵌纹理。
- `verify_skin.py`：检查 PNG 尺寸/格式/CRC、全部基础层面不透明、工程贴图一致、审阅几何部件数量/可见性与 UV 范围。

第三方审阅：[当前眼部 v2](https://blckbn.ch/tQUsI6)，[初稿对照](https://blckbn.ch/DEExZk)。2026-10-28 上传，选择 2 Weeks 有效期。分享为快照，修改本地资产后需要重新分享；过期可重新上传本地工程。

### 眼部 v2

按用户给出的 2×2 像素眼睛排布要求，将每只眼睛的四个像素旋转 180°，黑色眼珠位于下排靠脸部中线的一格，保留原有金色与眼白。两眼共改动 8 个像素，眼睛高度、眼距及所有其他像素保持不变。逐像素比较及 PNG/工程一致性检查通过，Blockbench 已检查正面；当前 PNG SHA256 为 `9f8134e3258d62aa5a2e1179b094184bc8f98232454f709996324d540be2fd9e`。截图为 `.local/guaba_yingrong-review/eyes-v2-front.png`。

## 造型

保持标准人形比例，采用深褐黑/橄榄色兜帽、金色帽沿、白色额发与青黑侧发、琥珀色眼睛、象牙白肩袖、黑金衣装，以及红色领结和颈部扣饰。沿用此前用户调整的适中眼距、较低眼位和无嘴像素风格。基础层保证关闭覆盖层时仍有完整身体，覆盖层用于帽沿、兜帽、领口、肩饰、束带、袖口与外套下摆。

参考图只显示上半身；深色长裤、黑金靴子、外套下摆和背面花纹为延续配色的补充设计。图中酒杯与红色能量属于独立道具/特效，不属于本次标准皮肤贴图。兜帽和衣摆使用标准皮肤覆盖层表现，不引入额外几何或模组前置。

工程复用项目已有标准 Alex UV 与自然站姿：模型单位为像素，地面 y=0，头顶 y=32，正面朝 -Z，关节/层级沿用标准人形。无专用关键帧动画；游戏中沿用粉红妹的原版 Slim 人形模型与装备层。

## 生成与验证

```bash
python3 designs/guaba_yingrong/create_skin.py
python3 designs/guaba_yingrong/verify_skin.py
```

2026-10-28 初稿：上述命令通过，重复生成 PNG 的 SHA256 一致，为 `f8bd75498f8d2d03aa4fe8a05920bb67f77407c51d34010144b2595812f58815`。在 Blockbench 网页实际打开工程，检查正面、背面、侧面和斜角，并通过视角菜单实际切换；可旋转、缩放的在线审阅使用同一模型和贴图。最终链接另开标签页验证加载。截图与检查输出位于 `.local/guaba_yingrong-review/`。

先前皮肤设计阶段未接入游戏；下节 NPC 接入阶段完成构建、无头行为验证与部署，客户端渲染和装备动作仍待人工验收。

## NPC 接入

实体 `pinkkicore:guaba_yingrong`，刷怪蛋 `pinkkicore:guaba_yingrong_spawn_egg`（深褐底色 `#302C21`、金色斑点 `#CBA45A`）。运行皮肤 `versions/1.20.1/src/main/resources/assets/pinkkicore/textures/entity/guaba_yingrong.png` 与本目录最终 PNG 逐字节一致，发行包校验会检查两者。

与粉红妹共用 `HumanoidNpcEntity` 和客户端 `HumanoidNpcRenderer`，保持 Slim 3 像素手臂、基础/覆盖层和原版装备挂点。站立、行走、观察、持物和近战摆臂由原版模型参数驱动，不另造近似几何或 GeckoLib 动画；编辑工程中的自然站姿仅供外观审阅。保留兜帽、衣摆的原皮肤覆盖层，未增加酒杯或能量特效。

中立游荡、受击近战反击、附近装备拾取与替换、六个装备槽、默认可见名字及命名牌改名、NBT 存档均沿用粉红妹规则；现已支持主世界草地稀有自然生成；没有交易、对话或任务。弓弩仅装备，没有远程攻击或主动格挡。接入与验证记录见 `docs/changes/2026-10-28-guaba-yingrong-npc.md`。
