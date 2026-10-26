# 瓜巴充电桩模型

已接入 1.20.1 Forge。模型为 16×12.8×16 模型单位（1 格宽深、4/5 格高），底部 Y=0，正面朝北（-Z）；放置后按水平朝向旋转。黑曜石色附魔台身、瓜巴土黄镶边，顶部三根铜针对应合成的三根避雷针；底部三枚雷霆瓜巴嵌块、上方蓝白充能晶体与折线电纹。

- `guaba_charging_station.bbmodel`：可编辑 Java Block 工程，内嵌 64×64 贴图，不依赖绝对路径。
- `guaba_charging_station.json` / PNG：与游戏运行时字节一致。
- `review.html`：内嵌同一导出几何和纹理的离线三维审阅；支持拖动、滚轮、立体/俯视/正面/侧面/背面。辅助网格仅属于审阅器。
- `generate.py`：确定性生成几何、贴图、工程、审阅页及 Wiki 图标，需要 Pillow。
- `validate.py`：标准库验证尺寸/UV/工程与导出/审阅/运行资产一致。
- `icons.py`：从本地原版 1.20.1 资源绘制配方材料图标及独立带电刷怪蛋图标，需要 Pillow。原版参考纹理权利属于 Mojang/Microsoft，不因仓库 MIT 改变。

```bash
python3 designs/guaba-charging-station/generate.py
python3 designs/guaba-charging-station/icons.py
python3 designs/guaba-charging-station/validate.py
```

本地浏览器已实际检查模型与贴图、多方向按钮、拖动和缩放，截图 `.local/guaba-charging-review/iso.png`。Blockbench 网页导入首次因授权边界被自动审批拒绝；用户随后明确授权，已在官方网页编辑器实际导入并核对 30 个部件、64×64 内嵌贴图、底座变换参数（0,0,0 / 16×2×16）及六面 UV 显示。截图 `.local/guaba-charging-review/blockbench.png`、`blockbench-uv.png`。本次未修改模型几何或贴图。未启动 Minecraft 图形客户端。游戏粒子由原版 ELECTRIC_SPARK 的客户端 animateTick 实现，静态模型不包含假动画或真实闪电实体，不产生火焰/伤害。
