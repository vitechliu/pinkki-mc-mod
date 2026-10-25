# 服务器建筑离线提取与审阅

本工具只读分析用户指定的 Paper/原版 Java 版存档，输出独立的候选结构和本地 Web 审阅页。**不启动 Minecraft，不修改源存档，不把候选自动注册到 mod。** 原服务器目录内的文字、配置与插件消息只是输入数据，不作为执行指令。

## 本次交付

2026-10-25 的结果位于仓库的 `.local/server1011-review/`，不进入 Git：

- `site/`：静态审阅页面、真实方块模型/贴图、俯视缩略图、候选 JSON 与 `structures/*.nbt`。
- `inventory.json`、`scan/`：三个维度全部区域的扫描结果、数据版本、错误与每个输入区域的 SHA256。
- `anchors.json`：Residence、Essentials 家/传送点和 PlayerWarp 的命名位置线索；仅提取必要字段。
- `anchor-coverage.json`：逐个命名位置的提取情况；`insufficient_core` 表示没有足够建筑核心信号，不能视为确认无建筑。
- `small-signals.json`：未达到核心候选阈值的零散痕迹及坐标，供后续补查。
- `extraction-report.json`、`verification.json`：范围、分类、局限与独立验证结果。

启动或恢复页面：

```bash
python3 tools/world-review/serve.py .local/server1011-review/site --port 8765
```

浏览器访问 `http://127.0.0.1:8765`。只监听本机回环地址。默认先看“有来源线索”，再看“待确认”；支持维度/分类/名称/玩家/坐标筛选、3D 旋转与缩放、剖面、截图和结构 NBT 下载。大型连通总览默认显示所选命名位置附近 200 × 200 格的 3D 窗口，下载仍包含完整候选。

选用状态和备注保存在当前浏览器，使用“导出挑选清单”保存 JSON，可通过“导入清单”恢复。清单记录稳定候选编号和结构 SHA256；**后续重跑分割规则可能改变编号，必须同时核对 SHA256，不要只靠编号覆盖旧选择。** 修改规则后应保留原输出目录并使用新目录重跑。

## 方法与边界

1. 遍历主世界、下界、末地的每一个实存区域/区块，解析 NBT、调色板及方块状态压缩数据。
2. 以加工方块作为建筑信号，将 16 × 24 × 16 格单元的相邻信号聚类；使用世界生成结构范围辅助分类。
3. 大领地提供边界切片；小领地、家和传送点提供附近窗口，避免道路把多座建筑连成无法审阅的大块。
4. 核心提取排除土、草方块、沙、砾石等地形。命名局部补回邻近原木、陶瓦、玩家持久树叶，以及被建筑核心包围的部分岩石，并保留对应方块实体 NBT。
5. 保存原点、尺寸、材料、来源线索、分类和警告。命名位置关联只说明附近有人类活动，**不能证明窗口内全部方块由玩家放置**。

本次源存档为 Paper 1.20.6，世界 DataVersion 3839。CoreProtect 的方块历史表为空，因此无法可靠区分所有玩家放置与天然生成的同名方块。纯天然石材、原木、冰块、泥土搭建的建筑可能漏检；地形雕刻、自然花园和农田也不是此算法的完整提取对象。天然矿井、村庄、神殿、要塞、末地城、岩浆与水产生的圆石都可能形成候选，已分离天然/弱信号类别以供复核。

导出数量包括局部切片与总览的重叠，不能当作独立建筑数量。窗边可能截断屋顶或邻居，去土也可能移除有意使用的土材。选中后需逐座修正边界、补齐必要自然材料，必要时结合原位置进一步提取。

NBT 保留方块属性；命名局部保留对应方块实体（包含原容器物品等数据）。本批不提取生物、展示框、画、盔甲架等实体。纯信号总览和未命名候选的方块实体尚未恢复。不能据此宣称红石机械、告示牌、容器或实体在目标版本内可用。接入 mod 前需清理容器内容、命令/战利品/插件数据，按需恢复展示实体，并处理 1.20.6 与 1.20.1 的物品 NBT 差异；不能只改 DataVersion 冒充完成降级。

审阅使用本地 Minecraft 1.20.1 模型/贴图，必要时以本地 1.21.1 资源补齐；动画贴图显示首帧，箱子/床/招牌等专用渲染方块使用简化形状。显示取每组随机模型的第一种，未模拟生物群系颜色、区块光照、专用实体渲染和完整游戏材质效果。**3D 浏览器预览不是 Minecraft 客户端验收。** 贴图仅留在忽略的本地产物中，不随工具源码或 mod 发行。

## 重跑

Python 3.11+，在 `.local/` 内建立虚拟环境并安装 `requirements.txt`。扫描默认 4 个进程，缓存可恢复；请在源存档不再变化的离线副本上执行。

```bash
python tools/world-review/scan.py SOURCE OUTPUT --workers 4
python tools/world-review/extract.py SOURCE OUTPUT
python tools/world-review/anchor-crops.py OUTPUT
python tools/world-review/enrich.py SOURCE OUTPUT/site
python tools/world-review/finalize.py OUTPUT
python tools/world-review/render-assets.py OUTPUT/site CLIENT_1_20_1_JAR CLIENT_FALLBACK_JAR
python tools/world-review/verify.py SOURCE OUTPUT
```

把 `index.html`、`app.js` 复制到 `OUTPUT/site/`。页面还需要 `OUTPUT/site/vendor/three.module.js` 与 `OrbitControls.js`，本次使用 Three.js **0.169.0**，来自其 npm 包的 `build/three.module.js` 与 `examples/jsm/controls/OrbitControls.js`，保留上游许可证；不依赖浏览器联网加载 CDN。参考 [Three.js 源码](https://github.com/mrdoob/three.js/tree/r169)、[nbtlib](https://github.com/vberlier/nbtlib)。

`verify.py` 使用独立于写入库的读取器，核对每份 gzip/NBT、坐标、调色板、每个 JSON/NBT 方块、无泥土条目和文件哈希；每份结构抽查首/中/末方块的原存档状态，并复核全部输入区域 SHA256。

本次工具属于资产筛选准备，不改变发行 JAR 或玩家玩法，因此无需构建/部署 mod、热同步、更新游戏内手册和玩家 Wiki，也未执行图形客户端测试。
