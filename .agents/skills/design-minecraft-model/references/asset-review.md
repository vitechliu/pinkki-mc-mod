# 资产与审阅技术说明

仅在处理相应格式、编辑器导入或预览实现时阅读。下面是饼蛙流程中实际遇到的问题，新的工具版本仍须核对。

## 仓库中的可复用样例

从仓库根目录查看：

- `designs/bingwa/generate.py`：确定性 UUID、长方体/骨骼、PNG atlas、工程与运行资产生成。
- `designs/bingwa/review-template.html`：无需 CDN 的 WebGL 审阅片段，骨骼动画、时间轴、相机与配件显示。
- `designs/bingwa/validate.py`：源工程坐标往返、UV、动画引用及审阅数据一致性校验。
- `designs/bingwa/GeckoAssetCheck.java`：用实际 GeckoLib 解析器无头读取模型和动画。

这些文件是具体模型的源文件。新模型需要复制并适配到自己的目录，不能覆盖饼蛙，也不能保留其固定数量断言、骨骼名、UI 文案或跳跃辅助逻辑。不要为执行样例校验而启动整个游戏。

## 工程与运行格式

饼蛙采用 `.bbmodel` 工程格式 4.10、`model_format: geckolib_model`、`geckolib_model_type: Entity` 和 `geckolib_modid: pinkkicore`；几何格式 1.12.0，动画格式 1.8.0。工程格式、GeckoLib 版本和 Minecraft 版本互相独立。

Blockbench 4.10 工程的 Bedrock/GeckoLib 坐标映射在该样例中为：

- `editor.from.x = -(geo.origin.x + geo.size.x)`，Y/Z 保持几何原点。
- pivot 的 X 取反；静态旋转的 X/Y 取反，Z 保持。
- 顶/底面的 per-face UV 端点需要按编辑器 codec 翻转，其他面按对应映射处理。
- Blockbench 5 加载旧工程时会迁移动画 position.x 和 rotation.x/y，GeckoLib 插件导出又有自身的坐标转换。不要额外翻转两次；必须检查实际编辑器 codec 并做往返校验。
- 不要只把 `format_version` 改成 5；新工程的 groups 等存储结构也不同。

优先使用实际编辑器导出并核对，程序生成时再按对应 schema 实现。低版本运行库未验证前不要依赖插件新加入的 Bezier、复杂 Molang 或导出标志。

## Blockbench 打开模型

使用 [Blockbench 网页版](https://web.blockbench.net/) 或桌面版，并通过官方插件商店安装 **GeckoLib Models & Animations**。真实打开 `.bbmodel` 后核对纹理、骨骼和全部动画，不能以 JSON 校验替代编辑器检查。

文件选择不可用时，网页版曾验证可通过 fragment 导入：

```javascript
const payload = encodeURIComponent(JSON.stringify(model));
const url = 'https://web.blockbench.net/#loadtype=json&loadname='
  + encodeURIComponent(name + '.bbmodel') + '&loaddata=' + payload;
```

- 编码前必须压缩成单行 JSON。曾使用的网页版导入实现会在换行处截断漂亮打印的 JSON。
- 使用 `#` fragment，不把整份模型作为 HTTP 查询参数；也不要把完整长 URL 或 base64 输出到聊天/日志。
- 同页只改 hash 可能不重新导入；需要重新加载时先保护当前未保存工程，必要时新开审阅标签。
- URL 过长时退回本地文件打开，不自动上传到云端分享站点。

## 交互审阅实现

审阅器必须能离线读取本次交付资产，并与导出文件对照。使用饼蛙 WebGL 模板时，保留纹理最近邻采样、骨骼层级与关键帧求值，按新主体调整相机、地面、动画和配件。

若交付内联可视化，遵循当时可用的 `visualize` skill 对片段结构、资源与展示路径的要求；不要硬编码个人机器的插件或输出路径。独立浏览器运行时，宿主专用 API 必须有存在性判断。

检查视图切换、拖动/缩放、时间轴、播放、配件父子关系与 UI 主题；单张截图不能证明这些交互。预览器支持的插值与动画表达式必须匹配导出资产，未实现的特性不能静默显示为近似效果。

## 证据分层

分别记录静态校验、实际库解析、Blockbench 编辑器、浏览器交互、游戏内验证。饼蛙历史上通过的 Forge 1.20.1 GeckoLib 4.4.9 / 4.7 / 4.8.4 解析不能自动成为新模型、新目标或完整整合包的兼容保证。当前最低依赖回到构建配置确认。
