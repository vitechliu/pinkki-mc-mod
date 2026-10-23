# 日常开发与按需适配

## 日常只处理 1.20.1

普通需求默认只实现、构建、无头测试和准备部署 **Minecraft 1.20.1 / Forge**。即使修改了共享代码，也不因此自动构建、运行或部署其他版本；其他版本允许暂时落后。客户端仍由用户手动测试，代理遵守 AGENTS.md 的实机测试限制。

通常按修改需要执行：

```bash
./scripts/mod.sh 1.20.1 build
python3 scripts/verify-artifact.py 1.20.1
# 涉及服务端行为时，再执行适用的无头验证
python3 scripts/smoke-server.py 1.20.1
```

本地“部署”默认执行 `python3 scripts/deploy-local.py`：只构建与校验 1.20.1，并自动安装 PinkkiCore 和缺少的 GeckoLib 到项目 HMCL 的 clean、compat 两个实例。用户明确指定时使用 `--profile clean` 或 `--profile compat`。不能只完成构建就报告部署成功；检查安装回执与哈希。没有安装对应实例时说明待完成的操作，不启动游戏。构建日志、备份和回执均留在 `.local/hmcl/`，详见 [HMCL 测试流程](hmcl-testing.md)。不要默认使用 `build-all.sh`。

日常资源调试可以执行 `python3 scripts/hot-sync.py`，默认同步 1.20.1 两个项目 HMCL 实例的受管资源包及所有已有世界的数据包（用户确认全部为测试世界），无需逐次指定世界。完成适用的资源/数据变更后主动同步，游戏由用户重载；不把热同步报告当成完整部署或客户端验收。用法见 [资源热同步](hot-sync.md)。

## 每次功能变更留下适配记录

新增功能、行为修复、功能移除、数据/网络协议或资源格式变化，在 `docs/changes/` 新增一份记录。文件名使用日期和简短主题；同一需求的后续修正可更新原记录。纯文档、skill 或 CI 管理变更通常不需要制造游戏功能待办。

记录遵循 [变更模板](porting/change-template.md)，重点写行为、源码位置、验收场景、兼容注意事项和实际验证，不复制代码或大段日志。对现有次要目标默认标记“待适配/验证”；只有确实无关时注明“不适用”及原因。共享代码已能编译不代表目标行为已经验证。

记录和源码放在同一提交中。无需预先猜测本次提交哈希，可写工作开始时的 HEAD 作为参照；以后从 Git 历史定位实际变更。

## 模型设计与审阅

项目 skill [`design-minecraft-model`](../.agents/skills/design-minecraft-model/SKILL.md) 用于生物、NPC、Boss 和工作方块的造型、贴图、动作及三维审阅。例如：

```text
使用 $design-minecraft-model，参考这张图片设计一个生物，先交付可旋转审阅的模型和动作。
```

设计资产放在 `designs/<标识>/`，交付可编辑 Blockbench 工程、相应运行格式和与资产一致的交互审阅。初稿阶段检查资产与实际编辑器/浏览器效果，不启动 Minecraft；接入游戏时再按用户授权范围处理注册、行为和运行资源。完整样例见 [饼蛙](../designs/bingwa/README.md)。

## 需要其他版本时使用项目 skill

项目 skill 位于 `.agents/skills/port-minecraft-version/SKILL.md`。例如向 Codex 提出：

```text
使用 $port-minecraft-version，把当前功能补齐到 1.21.1。
```

也可以自然表达“把这批功能适配到 1.19.1”，由 skill 处理。目标是指定版本，不自动顺带处理其他版本。新增目标版本也走该 skill，先明确加载器和 JDK，再建立适配目录。

skill 结合三份信息补漏：

1. `docs/changes/` 的功能记录与验收场景。
2. `docs/porting/status.json` 中该目标上次适配完成时的源码参照、源码指纹和验证证据。
3. Git 差异、当前源码与未提交文件；即使有人忘记写变更记录，也检查实际差异。

查看状态不会构建或启动任何游戏：

```bash
python3 scripts/targets.py status
```

源码指纹覆盖共享源码/资源、主版本源码/构建配置、共同 mod 元数据及共享 Forge 构建约定；目标指纹覆盖对应适配层与构建配置。只改文档或 CI 不会产生新的游戏适配待办。指纹变化表示需要审查/重新验证，不等于一定需要复制或修改目标代码。

## 适配完成的检查点

指定目标补齐全部积压内容后，完成其构建、JAR 校验和适用的无头测试，再更新各变更记录。只移植部分功能、仍有遗漏或测试失败时，不能推进完整检查点。

验证完成后才执行，例如：

```bash
python3 scripts/targets.py checkpoint --targets 1.21.1 \
  --evidence .local/porting-1.21.1-verification.log
python3 scripts/targets.py status --targets 1.21.1 --check
```

`--evidence` 必须是已有的非空验证记录，执行者需确认内容足以支撑结论。脚本记录源码与目标指纹，不替代实际测试；不能为了让发布门禁通过而直接刷新检查点。证据细节留在本地日志，功能记录保留可审查的命令、结果和证明范围。`source_ref` 是记录检查点时的 HEAD；尚未提交的内容由指纹覆盖，之后需与源码、记录一起提交。

初始检查点仅覆盖 `2dc4d71` 的空 mod 骨架，依据初始化时已有的三版本构建、JAR 和无头服务端记录；不代表第三方 mod 或次要版本客户端兼容性已经验证。

## CI 与发行范围

普通 push / PR 只构建 1.20.1；普通版本标签也只发布 1.20.1。需要多版本时手动选择目标，不能因为注册表中存在某个版本就自动发布它。

`config/minecraft-targets.json` 是 CI 目标与 JDK/加载器的选择表。多版本构建可用于适配中的诊断，不要求检查点已完成；正式发布次要目标必须通过当前源码与目标指纹检查。详见 [发布说明](release.md)。

## 同步玩家 Wiki

道具、合成、结构、多方块、生物与 Boss 等玩家内容变更，必须与 `wiki/` 对应条目及玩家更新记录在同一次提交中更新。正式条目注明适用版本与状态，不能把设计稿当作已发布玩法。Wiki 更新与 `docs/changes/` 记录各自保留，维护和发布方式见 [Wiki 维护说明](wiki.md)。
