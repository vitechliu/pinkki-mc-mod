# Pinkki / Pastel Spark 皮肤设计

2026-10-23，依据用户提供的角色插画制作。皮肤设计源资产；2026-10-25 已将该 v5 皮肤接入 1.20.1 Forge 的粉红妹 NPC。

- `pinkki-alex.png`：64×64 RGBA 标准 Java 皮肤，使用 Alex / Slim（3 像素手臂），包含基础与覆盖层。
- `pinkki-alex.bbmodel`：Blockbench Minecraft Skin 工程，纹理内嵌。打开后如覆盖层隐藏，使用 Skin → Toggle Skin Layer。
- `pinkki-review.bbmodel`：从同一皮肤的 Blockbench 标准 Alex 几何转换的审阅副本，包含 12 个部件，覆盖层默认显示；不添加标准皮肤以外的装饰几何。
- `create_skin.py`：以原生 UV 像素图案生成 PNG，同步两个工程的内嵌纹理。运行 `python3 designs/pinkki-skin/create_skin.py`。采用确定性像素绘制，不将参考图投影或缩小当成皮肤。

第三方审阅：[当前五官 v5](https://blckbn.ch/Le71Hq)、[五官 v4](https://blckbn.ch/gxJtdC)、[五官 v3](https://blckbn.ch/kgFRtA)（2026-10-24 上传），[过宽的 v2](https://blckbn.ch/eu88pb)、[初版对照](https://blckbn.ch/4S216y)（2026-10-23 上传）。各选择 2 Weeks 有效期；过期可重新上传本地审阅工程。修改本地文件不会自动更新已上传的快照。

## 五官 v5

按用户要求移除嘴巴，将眼睛与头发在 v4 基础上再下移一像素，保持水平眼距及角饰位置不变。底行只保留两侧腮红与中间肤色。修改仅发生在头部侧面与覆盖层侧面的 79 个像素。PNG CRC、两份工程内嵌纹理一致性、眼睛新行对应旧行且水平位置不变、嘴部恢复肤色检查通过；Blockbench 已检查正面和斜角。

## 五官 v4

按用户提供的少女皮肤图片调整眼睛与头发高度，保持 v3 的水平眼距。上下两行眼睛整体下移一像素，刘海、两侧头发及后发纹理同步下延一像素，角饰固定；腮红与嘴部随面部布局下移以保留间隔。修改仅发生在头部侧面与覆盖层侧面的 103 个像素。检查眼睛新行与旧行逐像素相等，确认水平布局未改；PNG CRC 和两份工程内嵌纹理一致性通过，已在 Blockbench 检查正面与斜角。

## 五官 v3

用户指出 v2 眼距过大。将两侧眼睛各向内收一格，虹膜位于正面从左到右第 3/6 列（零起算 2/5），眼睛区域间留两格肤色，布局左右对称。仅修改眼睛两行的 13 个像素，保持 v2 的刘海留白和其他造型。已检查 Blockbench 正面效果；PNG CRC 和两份工程内嵌纹理一致性通过。v2 为未被用户认可的历史稿，不能作为最终设计依据。

## 五官 v2

按用户的眼距反馈，重排正面眼睛、腮红、嘴部和刘海覆盖层。参考 [livsis 的 Pink haired anime girl](https://www.planetminecraft.com/skin/pink-haired-anime-girl-5569537/) 与 [The Skindex 的 pink hair anime girl](https://www.minecraftskins.com/skin/16722153/pink-hair-anime-girl/) 的少女脸部排布，仅参考五官布局，不复制其纹理。虹膜由脸部从左到右零起算第 2/4 列改为第 1/6 列，眼睛之间留四格肤色区域，上方刘海不再遮住眼线。仅改变面部与额前覆盖层的 28 个像素；头部其他面、服装和身体未变化。PNG CRC、工程内嵌纹理一致性、修改范围检查通过，并在 Blockbench 检查了正面和斜角。

保留粉色侧长发、黄白角饰、奶白/粉/薄荷/浅紫外套、露腰上衣、粉色裙装与高靴配色。参考图未展示背面，背面为基于正面的补充设计。长发与角饰为贴合标准人形表面的像素表现；标准皮肤不能复刻参考图的悬空长发、外扩裙摆、长尾巴、蓬袖和大鞋头几何。小字标识在 64×64 下简化为配色与图形，不承诺可读文字。

验证：PNG 尺寸/颜色格式、两份工程内嵌纹理一致性、审阅副本 12 部件及覆盖层可见性检查通过；已在 Blockbench 浏览器实际检查正面、背面、斜角，并通过新标签页独立打开最终分享链接。截图保存在 `.local/pinkki-skin-review/`。未启动 Minecraft，未验证游戏客户端效果。

2026-10-24 的皮肤设计阶段没有游戏内玩法变化；当时未修改玩家 Wiki、未实现的游戏手册或版本适配检查点。

## 粉红妹 NPC 接入

实体 `pinkkicore:pinkki`、刷怪蛋 `pinkkicore:pinkki_spawn_egg`。保持现有五官、Slim 3 像素手臂、基础和覆盖层；运行纹理 `versions/1.20.1/src/main/resources/assets/pinkkicore/textures/entity/pinkki.png` 与 `pinkki-alex.png` 逐字节一致。更新皮肤后需同步该运行纹理，并执行发行包校验。

本次选择原版 `PlayerModel` / Slim 图层与原版装备层，以直接支持手持物、四个护甲槽、头部物品和鞘翅显示；不另造 GeckoLib 骨骼或重复皮肤几何。站立、行走摆臂、观察与近战挥手由原版人形模型逐帧计算，没有独立的关键帧动画资源。模型 32 像素高对应原版人形视觉尺度，碰撞箱为 0.6 × 1.8 格；编辑工程中的姿态仅供外观审阅。

可在 Blockbench 打开 `pinkki-review.bbmodel` 拖动旋转、缩放和检查十二个基础/覆盖部件。旧云分享链接本次已提示失效，请使用本地源工程，不依赖旧快照。本次重新在 Blockbench 打开源工程审阅；游戏渲染、护甲贴合与持物观感仍待客户端人工验收。

行为、独立无头测试证据和手册接入缺口见 `docs/changes/2026-10-25-pinkki-npc.md`。
