# 张家与旮旯模型

适用于 1.20.1 Forge。白色人形以脚底为原点，正脸朝北，脸上的「张」绘在贴图上。张哥使用相同模型，运行时和审阅器都放大到两倍。待机是 2 秒的轻晃循环。行走是 0.8 秒的手脚交替循环。接触伤害由服务端碰撞判定。

石中剑的碰撞底座高 3 模型单位，选中范围延伸到剑柄。旮旯游戏机由粉色屏幕、底座、键盘和鼠标组成，屏幕使用原创像素对话场景。两个方块以北为模型正面，放置时朝向玩家。

- [离线三维审阅](review.html)可切换两种体型、两个方块、四个视角及角色动作。
- `zhang.bbmodel` 是可编辑 GeckoLib 工程。另两个 `.bbmodel` 是 Java 方块工程。三个工程均内嵌纹理。
- 游戏几何、动画、贴图和两张 Wiki 方块图标由同一脚本生成。

```sh
python3 designs/zhang-galgame/generate.py
python3 designs/zhang-galgame/validate.py
```

脚本使用 Pillow。修改模型或贴图时更新生成源，再重新生成发行资产、Blockbench 工程和预览。

离线浏览器已检查四种模型、四个视角、旋转、缩放、播放和拖动时间轴。Blockbench 实际导入三个工程。张家几何与动画通过 GeckoLib 4.4.9 解析。截图与日志在 `.local/galgame-review/`，Minecraft 客户端渲染待手动验收。
