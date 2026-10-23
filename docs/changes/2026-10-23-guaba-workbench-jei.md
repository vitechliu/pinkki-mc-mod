# 2026-10-23：瓜巴工作台 JEI 加工配方预览

- 变更 ID：2026-10-23-guaba-workbench-jei。
- 原问题：只有自定义 `guaba_conversion` 类型和原生配方网络同步，没有 JEI 插件/分类；JEI 不会自动为任意机器类型生成预览。工作台本体没有生存合成配方，本次也没有凭空新增。
- 完成行为：注册独立 JEI 工作台加工分类，使用真实工作台图标；显示左上瓜巴、左侧原料、右侧结果及 1 秒时间；数量取实际配方，不写死泥土/泥巴。保留 Ingredient 的标签/自定义候选项，不修改原始物品栈。`guaba_cost=0` 使用催化剂角色、显示一个瓜巴并提示不消耗，正数使用输入角色和实际数量。产物可查配方、原料与瓜巴可查用途、工作台作为机器入口；菜单箭头可点击跳转。面配置展开/收起时向 JEI 报告当前绝对屏幕占用区域，避让右侧控件。
- 源码位置：`versions/1.20.1/.../forge/client/jei/{GuabaJeiPlugin,GuabaConversionCategory}.java`；`GuabaWorkbenchScreen#getExtraGuiAreas`；双语语言文件。构建 `versions/1.20.1/{build.gradle,gradle.properties}`；`gradle/forge.gradle` 与共享 `META-INF/mods.toml` 仅新增按目标属性生成的可选 JEI 依赖声明（只有 1.20.1 配置该属性），不扩展玩法或依赖到次要版本。
- 依赖：仅编译引用 JEI Common/Forge API **15.2.0.27**，与本地 Mekanism 1.20.x 的编译基线一致；客户端可选范围 `[15.2.0.27,16)`，不内嵌、不强制安装 JEI，也不依赖 Mekanism/EnderIO/JER。可用 `-PjeiRuntime` 显式加入开发运行依赖；此次没有运行图形客户端。
- 参考源码：本地 [参考库索引](../reference-mods.md) 的 Mekanism `RecipeRegistryHelper`、`ItemStackToItemStackRecipeCategory`；EnderIO `MachinesJEI`、`MachineJEIRecipes` 和 `PrimitiveAlloySmeltingCategory`，采用分类/配方/机器入口/点击区标准注册方式，自行实现版式，不复制其美术。官方 [JEI 15.2.0.27 API 发布源码](https://maven.blamejared.com/mezz/jei/jei-1.20.1-common-api/15.2.0.27/) 已逐项核对。本地官方 JEI 15.20.0.129 源码的 `mezz/jei/forge/startup/StartEventObserver.java` 在配方与标签事件齐备后启动/重启 JEI，插件每次注册从当前客户端 RecipeManager 读取服务端已同步配方，不额外维护平行配方表；实际客户端 `/reload` 行为仍待人工验收。
- JER 边界：[JER 官方仓库](https://github.com/way2muchnoise/JustEnoughResources) 明确其基于 JEI 扩展资源信息。机器加工属于 JEI RecipeCategory；不应把它登记为 JER 矿物生成/掉落数据。本次没有安装 JER 做客户端共存验收，也不宣称提供另一种配方查看器（如 REI）的专用插件。
- Git 参照：工作开始 HEAD `e44bc47`，承接前一轮未提交的模型与工作台功能；保留已有 README / 参考资料修改。本次未提交、推送或发布。

## 实际验证

- `./scripts/mod.sh 1.20.1 build workbenchProbeJar -PworkbenchProbe --console=plain`：通过，日志 `.local/jei-research/build.log`。
- `python3 scripts/verify-artifact.py 1.20.1`：通过，日志 `.local/jei-research/artifact.log`。JEI 依赖为可选客户端、插件已入包、API 未内嵌；发行 JAR SHA256 `2e1e4f4dbe2a4a7246a34d7a447828ad8d8135e797c164986412d361a881c7de`。
- `python3 scripts/check-jei-linkage.py --api <真实JEI API或发行JAR>`：分别用 **15.2.0.27 Common+Forge API** 和 **15.20.0.129 Forge 发行 JAR** 通过；3 个隔离插件类的 20 个 JEI 字段/方法引用均能解析，其他类无 JEI 字节码引用。日志 `.local/jei-research/linkage-floor.log`、`linkage-15.20.log`。这是静态二进制接口校验，不能替代 JEI 客户端实际执行。
- `python3 scripts/check-workbench.py --geckolib <4.4.9 JAR>`：不安装 JEI 的独立发行服务端通过全部工作台回归，结果 `.local/workbench-tests/1792741348149037000/result.json`，日志 `.local/jei-research/without-jei-runtime.log`。
- 同一命令增加 `--extra-mod <JEI 15.20.0.129 Forge JAR>`：独立服务端通过，结果 `.local/workbench-tests/1792741422916202000/result.json`，日志 `.local/jei-research/with-jei-runtime.log`。两次均为回环地址、`--nogui`、就绪并正常停服，无 ERROR。JEI 客户端插件不会在专用服务端运行，此项只证明可选依赖加载和工作台服务端回归。
- Wiki 源文件与玩家更新已同步；Node 24 下 `npm run wiki:build` 通过，日志 `.local/jei-research/wiki-build.log`；`git diff --check` 通过。本地构建不代表 Pages 已部署。自研游戏内手册尚未实现，已把 `guaba_workbench` 的 JEI 入口说明列入待接入内容；不能将 JEI 当作手册同步完成。

## 待客户端验收

没有启动 Minecraft 图形客户端。以下需用户自行验收：JEI 分类与真实图标、泥巴 R / 泥土 U / 瓜巴 U 的检索、工作台用途入口、箭头点击、设置面板与列表避让、数量/标签候选轮换、零消耗催化剂提示、数据包重载刷新，以及装有 JER 的整合包共存。此轮仅预览，没有添加一键填料或自定义配方转移网络包。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | JEI/菜单 API 与目标依赖需另行核对 |
| 1.19.1 Forge | 待适配/验证 | JEI/菜单 API 与目标依赖需另行核对 |
