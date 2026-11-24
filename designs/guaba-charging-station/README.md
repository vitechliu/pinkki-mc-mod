# 瓜巴充电桩模型

已接入 1.20.1 Forge。模型为 16×12.8×16 模型单位（1 格宽深、4/5 格高），底部 Y=0，正面朝北（-Z）；放置后按水平朝向旋转。黑曜石色附魔台身、瓜巴土黄镶边，顶部三根铜针对应合成的三根避雷针；底部三枚雷霆瓜巴嵌块、上方蓝白充能晶体与折线电纹。

- `guaba_charging_station.bbmodel`：可编辑 Java Block 工程，内嵌 64×64 贴图，不依赖绝对路径。
- `guaba_charging_station.json` / PNG：与游戏运行时字节一致。
- `review.html`：内嵌同一导出几何和纹理的离线三维审阅；支持拖动、滚轮、立体/俯视/正面/侧面/背面。辅助网格仅属于审阅器。
- `generate.py`：确定性生成几何、贴图、工程、审阅页及 Wiki 图标，需要 Pillow。Wiki 图标从导出模型逐像素判断遮挡。
- `validate.py`：标准库验证尺寸/UV/工程与导出/审阅/运行资产一致。
- `icons.py`：从本地原版 1.20.1 资源绘制配方材料图标及独立带电刷怪蛋图标，需要 Pillow。原版参考纹理权利属于 Mojang/Microsoft，不因仓库 MIT 改变。

```bash
python3 designs/guaba-charging-station/generate.py
python3 designs/guaba-charging-station/icons.py
python3 designs/guaba-charging-station/validate.py
```

底座的正面装饰分布在三个深度，台面导线与槽体和针座错开。校验会拦截同向共面且面积重叠的面。

初版审阅在浏览器中检查了模型与贴图、多方向按钮、拖动和缩放，并在 Blockbench 网页编辑器核对了 30 个部件、64×64 内嵌贴图及六面 UV。初版截图在 `.local/guaba-charging-review/`，底座与导线的后续修正见[设备图标与瓜巴手册](../../docs/changes/2026-11-24-device-icons.md)。

游戏粒子由原版 ELECTRIC_SPARK 的客户端 animateTick 实现。静态模型只包含方块几何和贴图。
