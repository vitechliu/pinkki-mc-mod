# 代码结构与版本策略

```text
mod.properties                 名称、版本、许可证等共同元数据
common/src/main/java/          Java 17 语法的共享核心，不依赖 Minecraft/加载器
common/src/main/resources/     可跨目标复用的资源与 Forge 元数据模板
gradle/forge.gradle            Forge 5/6 共用构建约定
versions/1.20.1/               主目标，Forge，独立 Wrapper 与运行目录
versions/1.21.1/               NeoForge，独立入口和 neoforge.mods.toml
versions/1.19.1/               Forge 旧 API 适配层，独立 Wrapper
scripts/                      JDK 选择、构建、产物校验
designs/                      模型与动作设计源文件、生成校验工具和审阅稿，不自动打包
.github/workflows/             三目标构建与 GitHub Release
docs/                         上手、架构、发布与验证记录
```

## 共享范围

共用层适合放业务规则、计算、协议模型、平台接口。每个目标直接把共同源码编译进自己的 JAR，安装时只需要一个 JAR，不另外安装“common mod”。共用层遵守 Java 17 语法和 API 上限，不能因为 1.21.1 使用 Java 21 就引入 Java 21 专用 API。

Minecraft 对象、注册、事件、网络、存档、客户端渲染均放在版本适配层。将来需要跨版本业务时，再由共用层定义小接口，各适配层实现；当前玩法主要依赖 Minecraft API，保留版本专用实现，不预建空接口和大量占位模块。

相同的注册代码在两个 Forge 目标中出现是可接受的。优先保证语义正确，不通过反射、运行时判断版本或复杂预处理强行共用不同 API。资源格式也会变化，例如配方 JSON 与 pack format；只有确认兼容的资源才放 common。版本资源放各目标的 `src/main/resources`，禁止与共同资源同路径重复而依赖覆盖顺序。

客户端专属类放适配层的 `client` 包，并通过对应加载器提供的客户端生命周期入口加载。服务端可加载的入口与 common 中不能直接引用客户端类。

## 为什么是三个独立构建

1.19.1 官方工具链是 ForgeGradle 5 / Gradle 7 / Java 17；1.20.1 使用 ForgeGradle 6 / Gradle 8；NeoForge 使用 ModDevGradle / Java 21。独立构建能避免插件与 Java 运行环境冲突。脚本和 CI 统一调度，元数据与业务源码共享，所以不需要维护三个长期分支。

Forge 1.20.1 新 MDK 仍可使用 ForgeGradle 6。本仓库选择固定的 6.0.54；1.19.1 保留固定的 5.1.77。NeoForge 采用官方 MDK 当前固定版本，无需添加 Architectury 等额外兼容框架。未来若实际功能高度依赖 Minecraft API，再根据已出现的复用需求评估是否引入框架。

## 版本管理与部署

主线 `main` 保留各目标代码，日常只实现、验证和准备部署 1.20.1，普通 CI 也只构建该目标。次要目标允许暂时落后，在用户明确需要时由项目 skill 按功能记录、源码差异与检查点补齐，不维护长期版本分支。详见 [开发流程](development-workflow.md)。mod 版本号统一放在 `mod.properties`；发行文件名包含加载器、MC 和 mod 版本。

依赖范围声明当前精确 MC 版本；构建 SDK 与运行最低加载器版本可以分开配置，但最低版本必须有实际运行证据，避免把“看似接近”的版本声明为已验证。不同加载器 JAR 不通用。更新加载器最低版本前，应确认真实整合包的版本要求。

1.20.1 的 GeckoLib 仅由该目标引入：编译基线 4.4.9，运行允许 `[4.4.9,5)`，作为外部必需前置，不内嵌或重定位。模型、渲染和动画控制器代码仍留在版本适配层，不能把 GeckoLib 类带入 common。共享 Forge 元数据模板按目标配置展开 GeckoLib 依赖，1.19.1 独立采用 GeckoLib 3.1.30（`geckolib3`），使用旧动画控制器及实体事件适配。详见 [GeckoLib 兼容策略](geckolib-compatibility.md)。

GitHub Release 是分发产物；不会自动更新运行中的 Minecraft 服务端。部署采用“构建 → 测试实例 → 备份 → 停服 → 替换单个目标 JAR → 启动检查”。失败时回滚 JAR 和必要的存档备份。以后如需自动部署，应单独绑定测试/生产环境，不在发布工作流里直接 SSH 重启服务。
