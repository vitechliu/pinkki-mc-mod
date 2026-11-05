# guaba_texas 人形皮肤

依据用户于 2026-11-02 提供的全身参考图制作。当前是独立皮肤设计，未注册 NPC/敌人、未接入发行资源。

## 交付

- `guaba_texas.png`：64×64 RGBA 标准 Java 皮肤，Alex / Slim（3 像素手臂），包含基础层与覆盖层。
- `guaba_texas.bbmodel`：可编辑 Minecraft Skin 工程，内嵌同一 PNG；若覆盖层隐藏，使用 Skin → Toggle Skin Layer。
- `guaba_texas-review.bbmodel`：基于标准 Alex 实际几何的审阅副本，12 个身体/覆盖层部件默认全部显示。
- `create_skin.py`：按标准 UV 手工编排的像素图案，确定性生成 PNG 并更新两个工程的内嵌纹理。
- `verify_skin.py`：PNG 尺寸、RGBA 格式与 CRC、基础层不透明、下排内侧眼珠、工程贴图一致性、部件数量/可见性与 UV 范围检查。

第三方审阅：[Blockbench](https://blckbn.ch/nX6P12)。2026-11-02 上传，选择 2 Weeks 有效期。在线版本是快照；本地修改后需要重新分享，过期可重新上传本地审阅工程。

## 造型规格

黑色长发与蓝色挑染、白色褶边女仆头饰、蓝色眼睛、金色耳饰、黑色领结与金色领扣、白色胸前褶边、黑色束腰和金色腰饰、白色围裙、黑色裙摆金纹、脚踝白色饰边及黑色鞋带鞋。右侧裙装加入参考图腰间纸笺的简化纸面与棕金边框；背面用发束、蓝色飘带与围裙蝴蝶结延续正面设计。

五官沿用用户已确认的低眼位和适中眼距：两眼各为 2×2 像素，黑色眼珠在下排内侧，无嘴巴。头饰/刘海覆盖层不遮住眼睛。

保持标准人形几何，单位为像素，地面 y=0、头顶 y=32、正面朝 -Z，关节和自然站姿复用标准 Alex。长马尾、飘带、蓬袖与裙摆通过贴图/标准覆盖层表现，未添加外扩或悬空几何。托盘、茶壶和茶杯属于独立持物，本次皮肤不包含其模型或动作。无专用动画，不引入 GeckoLib 运行资源。

## 生成与验证

```bash
python3 designs/guaba_texas/create_skin.py
python3 designs/guaba_texas/verify_skin.py
```

2026-11-02：生成及校验通过，重复生成 PNG 的 SHA256 一致，为 `9f2cb787fccabb2b5702404ea62e8512f5109394c5a3f1ef81fafe967e9c4a33`。两份工程内嵌纹理与 PNG 相同。在 Blockbench 网页实际检查正面、背面、两侧和斜角，并通过视角菜单实际切换。最终分享另开标签页验证加载。截图和检查输出保存在 `.local/guaba_texas-review/`。

从独立打开的第三方分享页导出皮肤后，解码比较全部 RGBA 像素，与本地 PNG 完全相同；浏览器重新编码导致 PNG 文件哈希不同，不将文件字节差异误判为贴图变化。证据在 `.local/guaba_texas-review/shared-verification.json`。

仅设计未接入游戏：未启动 Minecraft、未验证游戏客户端渲染或 NPC 行为；无需构建、部署或资源热同步。无玩家可用玩法变化，不修改 Wiki、游戏内手册或版本适配检查点。
