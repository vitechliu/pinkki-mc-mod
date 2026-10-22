# 游戏内引导手册方案调研

调研日期：2026-10-22。范围为 Minecraft 1.20.1 / Forge / Java 17。本文仅记录方案与源码证据，尚未选择依赖、实现手册或执行游戏测试，不代表次要版本已适配。

## 结论

可以不依赖 Patchouli；进一步完全不依赖第三方手册库也可行。两种目标需要分开：

- 完全自带：PinkkiCore 自己注册手册物品、加载内容并绘制客户端 Screen，没有手册框架的运行时依赖。
- 玩家免装：将第三方库打入发行包，玩家不必单独下载，但内部仍然依赖该库，仍需处理版本协商、许可证与升级。

如果明确要求完全独立，建议做范围受控的原生手册，分阶段添加能力。如果首期就要复杂的交互式三维场景、排版和配方展示，则优先评估成熟框架，避免把手册本身变成大型子项目。

## 源码案例

### 沉浸工程：独立手册

核对的是仓库 `1.20.1` 分支：

- [ManualItem.java](https://github.com/BluSunrize/ImmersiveEngineering/blob/1.20.1/src/main/java/blusunrize/immersiveengineering/common/items/ManualItem.java)：自己的物品，客户端使用时通过代理打开手册。
- [ManualInstance.java](https://github.com/BluSunrize/ImmersiveEngineering/blob/1.20.1/src/manual/java/blusunrize/lib/manual/ManualInstance.java)：内容树、资源重载、配方索引，以及图片、物品、表格、实体等特殊页面元素。
- [ManualElementMultiblock.java](https://github.com/BluSunrize/ImmersiveEngineering/blob/1.20.1/src/main/java/blusunrize/immersiveengineering/client/manual/ManualElementMultiblock.java)：使用结构数据和模板世界绘制多方块，包含分层显示、逐步搭建、成型状态与材料检查。

这是完全自带手册的实际案例。可学习其“物品入口 → 手册内容模型 → 页面元素 → 客户端渲染”的结构，不需要将整个 mod 或其手册代码复制进来。其 [LICENSE](https://github.com/BluSunrize/ImmersiveEngineering/blob/1.20.1/LICENSE) 是自定义许可证，不能按本项目 MIT 处理源码和美术资产。

### 匠魂：Mantle 手册框架

- [TinkerBook.java，1.20.1](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/library/client/book/TinkerBook.java)：调用 Mantle 的 `BookLoader.registerBook` 注册多本手册，并注册工具、材料、词条等自定义页面类型。
- [Mantle BookLoader.java，1.20](https://github.com/SlimeKnights/Mantle/blob/1.20/src/main/java/slimeknights/mantle/client/book/BookLoader.java)：内置文字、图片、合成、熔炼、锻造、结构、索引等页面类型，通过资源系统加载。

不依赖 Patchouli，但依赖 Mantle；不应把它作为“完全不依赖手册库”的案例。只为本项目一本手册引入 Mantle，不是目前的首选。

### AE2 / GuideME：Markdown 与三维场景

必须区分版本，不把 AE2 历史实现与现在的独立库混为一谈：

- [AE2 Forge 15.4.0 build.gradle](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/forge/v15.4.0/build.gradle)：已经声明 `org.appliedenergistics:guideme` 依赖。
- [GuideME v20.1.15 gradle.properties](https://github.com/AppliedEnergistics/GuideME/blob/v20.1.15/gradle.properties)：明确配置 Minecraft 1.20.1、Java 17 和 Forge 最低 47.1.0，与本项目目标范围初步匹配。
- [v20.1.15 README](https://github.com/AppliedEnergistics/GuideME/blob/v20.1.15/README.md)：Markdown、配方显示、交互式三维结构、全文搜索、物品快速跳转及扩展 API。
- [v20.1.15 GuideBuilder.java](https://github.com/AppliedEnergistics/GuideME/blob/v20.1.15/src/main/java/guideme/GuideBuilder.java)：资源目录、首页、语言设置、索引、开发源目录及自定义扩展。中文搜索质量仍需要针对实际版本验证。

因此“不用 Patchouli”的成熟框架方案存在。GuideME 的嵌入分发尚未在本项目验证，不能仅由目标版本匹配推断兼容。其 README 标明 LGPL，内部还包含其他许可证的库，接入时应保留各组件的许可证和来源信息。

### Patchouli：作为能力基准

- [官方入门](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/getting-started/)：先安装 Patchouli，再以资源文件定义书籍、分类和条目。无需编写 Java 内容接入代码，不等于无需安装 Patchouli。
- [1.20.1 GuiBook.java](https://github.com/VazkiiMods/Patchouli/blob/1.20.1/Xplat/src/main/java/vazkii/patchouli/client/book/gui/GuiBook.java)：原生客户端 Screen、翻页、历史返回、书签、缩放和多方块展示入口。

自研时这些都是要自行实现和维护的功能，不能把“右键打开一个页面”视为完整手册的全部工作。

## 内嵌库的可行性边界

[ForgeGradle 6 Jar-in-Jar 文档](https://docs.minecraftforge.net/en/fg-6.x/dependencies/jarinjar/)支持将依赖放在 mod JAR 内并生成加载元数据，版本选择通过支持范围协商。它提供了实现“玩家不用另装”的机制，但不自动证明某个库可以直接内嵌。

如果选择 GuideME，需要验证：依赖和映射配置、发行 JAR 内嵌元数据、独立加载、与整合包已有 GuideME 的版本协商，以及无头服务端加载。不得将第三方 mod 类随意解包、改包名再当作完全独立实现。

## 对 PinkkiCore 的建议

如果采用完全自研，第一阶段建议包括：

- 一个本项目命名空间内的手册物品，右键打开原生客户端界面；物品获取方式另行确定。
- 分类目录、条目、文字和图片、物品图标与悬停提示、内部链接、返回历史和阅读位置。
- 中文搜索；以实际中文标题与正文检索场景验收。
- 按配方 ID 从当前游戏 RecipeManager 查询配方，避免整合包修改后仍显示硬编码配方。
- 内容随 JAR 提供、离线可读；支持资源重载与语言资源。缺失条目或配方时显示可诊断的提示。

三维多方块逐层展示、世界投影、Boss 动态模型、进度解锁等按实际需求分阶段实现。三维场景不仅是画一个方块，还涉及模型、方块实体、光照、相机、裁剪与不同渲染环境的兼容。

GUI 和 Minecraft API 放在 `versions/1.20.1/.../client`；`common` 最多保留 Java 17 的纯数据模型和与游戏 API 无关的规则。服务端可加载类不得直接引用客户端类。纯阅读界面可以使用客户端 Screen，无需仅为打开手册引入容器同步；若加入进度解锁或奖励等权威状态，另行设计服务端校验和同步。

## 与现有 Wiki 同步

目标是共享条目来源，生成两个展示入口，而不是手写两套正文：

- 维护稳定条目 ID、适用版本、标题、正文、关联物品与配方 ID。
- 普通文字可约定 Markdown 子集；合成、多方块等用明确的结构化节点或标签。
- 构建时分别生成 VitePress 页面和游戏内资源。完整 VitePress Markdown/Vue 不能直接交给游戏 Screen 渲染。
- 游戏内配方展示查询当前服务器同步的配方；Wiki 展示发行基准数据，并注明整合包可能修改。
- 游戏内手册对应安装的 mod 版本，Wiki 的新内容应标注未发布或版本差异，避免旧 JAR 玩家误用。

GuideME 当前文档另有 [网站导出](https://guideme.appliedenergistics.org/integration/website-export/)，但 API 标为实验性，导出流程包含客户端运行配置。未证实这些能力完整存在于 v20.1.15，也未证明能直接生成当前 VitePress 网站；本项目不能默认采用该流程，更不能为此擅自启动游戏客户端。

## 实际完成与未验证范围

已阅读上述官方资料和关键源码，核对了本项目 1.20.1 构建配置及客户端隔离要求。未增加任何依赖、游戏源码或资源，未启动 Minecraft、未执行适配、编译或运行兼容性测试。所有自研功能和内嵌接入均为方案，尚未实现。
