# 2026-10-22：空 mod 骨架基线

- 变更 ID：2026-10-22-bootstrap
- 完成行为：PinkkiCore 在加载时输出成功日志，没有游戏内容。
- 源码位置：`common/src/main/`、`versions/*/src/main/`、`gradle/forge.gradle`、`mod.properties`。
- Git 参照：`2dc4d71aabac20b35b0e1346998c1773a9973994`。
- 验收场景：JAR 包元数据正确；无头服务端加载 PinkkiCore、世界就绪、stop 后保存完成并正常退出。
- 1.20.1 验证：初始化时构建、JAR 校验、开发服务端及普通 Forge 服务端安装测试通过；用户人工确认客户端 Mods 列表。见 [初始化记录](../verification.md)。
- 移植注意：1.19.1 保留 ForgeGradle 5 的 `forceExit false`；1.21.1 使用 NeoForge 独立入口与元数据。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 已验证 | 初始空骨架构建、JAR 校验与无头开发服务端通过，GUI 与发行包实际安装未验证 |
| 1.19.1 Forge | 已验证 | 初始空骨架构建、JAR 校验与无头开发服务端通过，GUI 与发行包实际安装未验证 |

此为历史基线，不把之后的功能自动视为已适配。当前开发流程调整只涉及 skill、记录与 CI，不新增游戏行为。
