# Pinkki / Pastel Spark 皮肤设计

2026-10-23，依据用户提供的角色插画制作。仅设计审阅，未注册为 NPC、未接入游戏运行资源。

- `pinkki-alex.png`：64×64 RGBA 标准 Java 皮肤，使用 Alex / Slim（3 像素手臂），包含基础与覆盖层。
- `pinkki-alex.bbmodel`：Blockbench Minecraft Skin 工程，纹理内嵌。打开后如覆盖层隐藏，使用 Skin → Toggle Skin Layer。
- `pinkki-review.bbmodel`：从同一皮肤的 Blockbench 标准 Alex 几何转换的审阅副本，包含 12 个部件，覆盖层默认显示；不添加标准皮肤以外的装饰几何。
- `create_skin.py`：以原生 UV 像素图案生成 PNG，同步两个工程的内嵌纹理。运行 `python3 designs/pinkki-skin/create_skin.py`。采用确定性像素绘制，不将参考图投影或缩小当成皮肤。

第三方审阅：[新版五官 v2](https://blckbn.ch/eu88pb)，[初版对照](https://blckbn.ch/4S216y)。2026-10-23 上传，各选择 2 Weeks 有效期；过期可重新上传本地审阅工程。修改本地文件不会自动更新已上传的快照。

## 五官 v2

按用户的眼距反馈，重排正面眼睛、腮红、嘴部和刘海覆盖层。参考 [livsis 的 Pink haired anime girl](https://www.planetminecraft.com/skin/pink-haired-anime-girl-5569537/) 与 [The Skindex 的 pink hair anime girl](https://www.minecraftskins.com/skin/16722153/pink-hair-anime-girl/) 的少女脸部排布，仅参考五官布局，不复制其纹理。虹膜由脸部从左到右零起算第 2/4 列改为第 1/6 列，眼睛之间留四格肤色区域，上方刘海不再遮住眼线。仅改变面部与额前覆盖层的 28 个像素；头部其他面、服装和身体未变化。PNG CRC、工程内嵌纹理一致性、修改范围检查通过，并在 Blockbench 检查了正面和斜角。

保留粉色侧长发、黄白角饰、奶白/粉/薄荷/浅紫外套、露腰上衣、粉色裙装与高靴配色。参考图未展示背面，背面为基于正面的补充设计。长发与角饰为贴合标准人形表面的像素表现；标准皮肤不能复刻参考图的悬空长发、外扩裙摆、长尾巴、蓬袖和大鞋头几何。小字标识在 64×64 下简化为配色与图形，不承诺可读文字。

验证：PNG 尺寸/颜色格式、两份工程内嵌纹理一致性、审阅副本 12 部件及覆盖层可见性检查通过；已在 Blockbench 浏览器实际检查正面、背面、斜角，并通过新标签页独立打开最终分享链接。截图保存在 `.local/pinkki-skin-review/`。未启动 Minecraft，未验证游戏客户端效果。

本次只有设计源资产，无游戏内玩法变化，因此不修改玩家 Wiki、未实现的游戏手册或版本适配检查点。
