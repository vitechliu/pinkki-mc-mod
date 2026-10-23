# 瓜巴工作台：交错刻痕第三稿

状态：**第三稿已确认并接入 Minecraft 1.20.1 / Forge，尚未发布**。已注册瓜巴工作台方块与物品、转换菜单、红石和六面物品端口。此目录保留设计源，运行模型与贴图复制到主版本对应资源目录，构建时校验一致性。

本稿占地 16×16 模型单位（1×1 格），高度 12.8 单位（4/5 格）；地面 Y=0，北面为 -Z。保留方正西瓜轮廓和红色瓜瓤。第三轮反馈后，移除原先规整的九宫格与矩形边框，改为疏密不一、局部断开、方向略有变化的斜向交错刻痕，形成不规则网纹。保留锤子、小刀、划痕与四粒瓜籽；工具均为平面像素纹理，不是突出表面的几何体。原版工作台仅作为第二稿工具主题的参考，不沿用其顶部合成格布局。

四周、底面和顶面外沿使用原有 `versions/1.20.1/src/main/resources/assets/pinkkicore/textures/item/guaba.png` 的土黄棕色系：主色 RGB(178,121,76)、暗纹 RGB(146,94,54)、高光 RGB(217,152,102)，顶面浅色皮层取 RGB(214,182,155)。保留瓜皮纵向条纹，已移除原绿色。原版工作台资源只在 `.local/guaba-review/reference/` 作为审阅参考，不复制进交付资产。

采用静态 Java Block/Item 格式，不需要手册或动画前置。视觉尺寸不是已实现的游戏碰撞箱。四个侧面使用相同条纹；贴图图集为 64×64，单面采用 32 像素宽度，以容纳工具细节。按新高度调整物品显示平移配置，仍需未来游戏验收。

- `guaba_workbench.bbmodel`：可编辑 Blockbench 工程，贴图内嵌，无绝对纹理路径。
- `guaba_workbench.json`：Java 静态方块模型设计资产，纹理引用为预留路径，未复制到运行资源。
- `textures/guaba_workbench.png`：自行绘制的像素贴图。
- `review.html`：离线交互审阅片段，直接嵌入同一导出模型和贴图，支持旋转、滚轮缩放及各向视图。
- `review-standalone.html`：可直接用浏览器打开的完整离线审阅页。
- `generate.py`、`review-template.html`、`validate.py`：确定性生成、预览及资产一致性校验。

在仓库根目录运行：

```bash
python3 designs/guaba-workbench/generate.py
python3 designs/guaba-workbench/validate.py
```

已实际通过静态资产校验，已在 Blockbench 网页版打开工程并核对几何和贴图；浏览器已检查切面与立体显示。第三稿审阅证据保存在 `.local/guaba-review/`（`blockbench-v3.png`、`preview-v3.png`、`top-v3.png`）。与第二稿逐像素对照，修改仅发生在顶部瓜瓤中央；模型尺寸、工具、侧面和底面保持一致。未启动 Minecraft，不以编辑器显示代替游戏内验证；物品显示变换只完成配置与一致性检查，尚未游戏验收。未验证或适配次要版本。

正式接入、实际无头功能验证和客户端待验收范围见 [变更记录](../../docs/changes/2026-10-23-guaba-workbench.md)；玩家操作、配方和更新已同步 [Wiki](../../wiki/guaba-workbench.md)。自研游戏内手册尚未实现，条目接入缺口已明确记录；次要目标不推进检查点。
