# 2026-10-28：瓜巴工作台合成配方

- 变更 ID：2026-10-28-guaba-workbench-crafting
- 需求与行为：新增原版九宫格有序合成，八个 `pinkkicore:guaba` 围绕中央 `minecraft:crafting_table`，产出一个 `pinkkicore:guaba_workbench`。加工、槽位和自动化机制不变。使用标准配方类型供原版合成、手册与 JEI 查询。
- 资源：1.20.1 的 `data/pinkkicore/recipes/guaba_workbench.json`，以及 `advancements/recipes/misc/guaba_workbench.json`。取得瓜巴、工作台或已解锁配方任一条件触发配方书解锁；实际合成不依赖解锁条件。
- 玩家内容：Wiki 配方图直接导入该 JSON，以真实瓜巴、工作台和瓜巴工作台图标展示；同步道具、操作页、更新记录。既有手册构建器从同份 Wiki 生成离线正文与配方引用，无新增客户端实现。`scripts/render-workbench-icon.py --minecraft-assets ...` 使用本地原版纹理生成工作台图标，不启动游戏；原版素材权属不因仓库许可证改变。
- Git 参照：开始时 HEAD `2e522e0`，保留已有生物生成/掉落及 Tetra 本地化改动。
- 验收场景：原版工作台摆成 GGG/GWG/GGG（G 为瓜巴、W 为工作台），得到 1 个瓜巴工作台；缺少任一外围瓜巴或中心不是工作台时不匹配。在 JEI 查询产物配方，在瓜巴手册中查阅合成图。
- 1.20.1 验证：`./scripts/mod.sh 1.20.1 build --console=plain` 通过（`.local/guaba-workbench-crafting-build.log`），`python3 scripts/verify-artifact.py 1.20.1` 校验实际发行包中九宫格、两种材料、产物数量及解锁定义通过。四项手册来源测试、生成手册中本体合成引用和旧说明清除检查通过；Wiki 构建通过（`.local/guaba-workbench-crafting-wiki.log`），`git diff --check` 通过。纯原版静态配方未额外启动服务端或客户端；JEI 查询和手动合成未实机操作，远端 Pages 未发布。
- 默认完整部署：`python3 scripts/deploy-local.py` 成功，`.local/hmcl/deployments/20261028-204520-753792/receipt.json` 为 installed。clean、compat 两个实例的实际 JAR SHA256 均与发行包 `59ee8598f1c27caa26e4b07e9025bdd7b4c166d9ef0bbc273422fd32373ea238` 一致。保留原有第三方 mod、配置和存档，未启动游戏。
- 热同步：`python3 scripts/hot-sync.py` 成功，`.local/hmcl/hot-sync/20261028-204535-287490/receipt.json` 为 ok，已同步两个实例资源包和全部已有测试世界数据包；未代替用户执行 F3+T 或 /reload。
- 移植注意：纯配方/进度资源与玩家文档；按目标核对有序合成 result 与 advancement 格式。次要目标未修改，检查点不推进。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 未构建/部署 |
| 1.19.1 Forge | 待适配/验证 | 未构建/部署 |
