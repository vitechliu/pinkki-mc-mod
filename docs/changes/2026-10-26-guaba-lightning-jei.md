# 2026-10-26：雷击转换 JEI 页面

- 变更 ID：2026-10-26-guaba-lightning-jei
- 需求与完成行为：JEI 新增“雷击转换”分类，用瓜巴输入槽、雷霆瓜巴输出槽、雷电/箭头展示获取方式。显示单位比例 1:1，说明掉落物必须遭受雷击、可同时转换多组且数量不变；提供中英文文案。
- 查询：输入和输出使用 JEI 的 INPUT / OUTPUT 槽角色，可分别由瓜巴用途和雷霆瓜巴配方索引。没有要求工作台、避雷针或额外消耗品，不添加合成配方，不改变实际雷击行为。静态展示项独立注册，不依赖世界配方管理器；原工作台分类保持原有注册方式。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/client/jei/GuabaLightningCategory.java`、`GuabaJeiPlugin.java`；目标语言资源；发行校验脚本要求新分类类文件存在。所有 JEI 类型继续限制在客户端可选集成包，不增加强制依赖。
- 手册与 Wiki：同步 `wiki/items.md`、`wiki/compatibility.md`、`wiki/changelog.md`；构建从 Wiki 源生成离线手册的道具与兼容条目。
- Git 参照：开始时 HEAD `c451415`。
- 验收场景：安装 JEI 后，在雷霆瓜巴上按 R、瓜巴上按 U，均可进入雷击转换分类；检查真实图标、1:1 槽位、落地雷击和批量说明。切换中英文检查排版；原工作台加工仍可查询。不装 JEI 时原获取方式照常可用。
- 1.20.1 验证：`./scripts/mod.sh 1.20.1 build` 成功；`python3 scripts/verify-artifact.py 1.20.1` 通过；`scripts/check-jei-linkage.py` 对实际发布的 JEI 15.2.0.27 common/Forge API 检查通过（5 个隔离插件类、30 个符号链接）；`python3 -m unittest discover -s scripts/tests -p test_guide.py` 的 4 项手册测试通过。
- Wiki 验证：`npm ci --cache .local/npm-cache --prefer-offline --no-audit --no-fund` 与 `npm run wiki:build` 成功；仅有既有 mcfunction 高亮回退提示。`git diff --check` 通过。
- 完整部署：`python3 scripts/deploy-local.py` 成功；本次 `.local/hmcl/deployments/20261026-154327-978673/receipt.json` 为 installed。两个实例实际 JAR 与发行产物 SHA256 均为 `79995d923dc473ea632c9195159a60d3b346650a133568c2d9826b122514e520`。构建日志保存在同目录 `build.log`。
- 热同步：`python3 scripts/hot-sync.py` 成功，本次 `.local/hmcl/hot-sync/20261026-154344-294474/receipt.json` 为 ok，两个实例资源及已有测试世界数据均同步。F3+T 与 /reload 分别重载资源和数据；Java 页面注册需要重新启动游戏，热同步不能代替安装新 JAR。
- 未验证：按项目规则未启动图形客户端，JEI 页面实际渲染、中英文排版和 R/U 交互仍待人工验收；未推送或部署线上 Wiki。只改客户端展示，没有重跑已经独立验证过的雷击服务端行为。
- 移植注意：仅 1.20.1 接入，次要目标需要核对各自 JEI API、客户端 GUI 绘制和可选集成配置；不推进适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 按需核对 JEI API 和 GUI API |
| 1.19.1 Forge | 待适配/验证 | 按精确 1.19.1 API 适配 |
