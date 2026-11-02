---
name: package-minecraft-release
description: 按用户指定目标手动触发 PinkkiCore 的 GitHub Actions 打包，或将已验证版本发布为含多版本 JAR 的 GitHub Release。用于“打三版本包”“发布三个版本”“补充 Release 附件”；调研、准备流程或创建本 skill 本身不触发构建或发布。
---

# 按需打包与 GitHub Release

先读 [协作规则](../../../AGENTS.md)、[发布流程](../../../docs/release.md)，核对当前工作流，不能只沿用历史核验结论。仓库为 `vitechliu/pinkki-mc-mod`，默认分支 `main`。

## 范围与授权

- “打包/构建”默认只运行 `build.yml`，产物在 Actions artifacts；“发布到 Release/补 Release 附件”才运行 `release.yml`。只有目标含糊、影响后续动作时才澄清；“三个版本/全部版本”可直接解析为 `all`，不再逐个询问。
- 三个已登记目标是 1.20.1 Forge / Java 17、1.21.1 NeoForge / Java 21、1.19.1 Forge / Java 17。目标与 JDK 仍以 `config/minecraft-targets.json` 为准；普通开发不能因此自动构建三版本。
- 调研、审核、准备发布流程及创建 skill 不授权实际 dispatch、提交推送、推标签或发布。本次明确要求发布时，在授权范围内完成必要操作；不要重复索要已有授权，也不要将无关未提交内容顺带推送。
- 不自动移植缺失玩法、不通过更新检查点绕过适配验证。检查点失效时先说明受阻目标和差异；用户要求补齐时衔接 `$port-minecraft-version`。

## 执行前确认源码与门禁

1. 检查 Git 状态、当前提交、远端 main、目标标签、工作流和 Release 现有附件。使用连接器读取源码；运行/Release 清单能力不足时用 `gh` 只读查询，不输出凭证。
2. 执行 `python3 scripts/targets.py matrix --targets all`（或用户指定的版本），核对 loader 与 Java。对正式 Release 执行 `python3 scripts/targets.py status --targets all --check`；仅诊断构建允许检查点落后。
3. 云端只会构建已推送的源码。存在待发布的本地修改时先分清来源、提交范围和推送授权；不能用本地成功证明远端源码已更新。发布必须检查**标签对应的源码**，而不是只检查当前工作区。可在项目 `.local/` 建立目标提交的只读快照运行门禁；不要改动正在开发的 checkout。
4. 标签必须已经存在，且等于目标源码 `mod.properties` 的 `v` + `mod_version`。验证远端标签指向预期提交。不要挪动已发布标签。需要新标签时，先完成验证并在本次发布授权内创建、推送；没有授权则只准备具体方案。
5. 当前推送 `v*` 标签本身会自动发布 1.20.1；手动 `targets=all` 再为同一标签补齐三个目标。提前说明这一行为，防止误以为推标签只是准备。如果用户要求完全只由手动工作流发布，需要另行修改标签触发配置，而非默默改变默认策略。
6. 任何未完成移植、构建失败或校验失败都不得声称可正式发布。打包不等于客户端/整合包兼容性已验收；遵守禁止实机测试规则。

## 手动触发与等待

确认工作流已存在于远端默认分支，且此次实际执行已获授权后：

```bash
# 仅打三个版本的包；使用 main 上的源码，不创建 Release
gh workflow run build.yml --repo vitechliu/pinkki-mc-mod --ref main -f targets=all

# 正式发布；TAG 必须先设为已核验存在、源码及版本匹配的标签
gh workflow run release.yml --repo vitechliu/pinkki-mc-mod --ref main -f tag="$TAG" -f targets=all
```

仅选择部分目标时，用明确的逗号分隔列表替换 `all`。网页等效入口为 Actions → 对应工作流 → Run workflow。

记录 dispatch 前的时间与已有运行 ID，再通过 `gh run list --workflow ... --event workflow_dispatch` 定位本次新运行，核对工作流、事件、分支、时间与预期输入；并发场景不能盲目选择“最新一条”。记录 run URL/ID，用 `gh run view` 查看阶段，或在保证用户可见进展的情况下等待。`gh run watch` 非零或工作流失败时，查看失败日志并报告具体阶段；不因失败反复创建 Release 或无限重跑。对于手动 Release，运行记录的 head SHA 可能是承载工作流的 main，发行源码实际来自输入标签，需分别核对。

## 验收产物

- 仅构建：确认所有所选矩阵任务成功，artifacts 名称与目标对应。Actions artifacts 当前保留 14 天，它们不是 Release 下载附件。
- 发布：必须等待 validate、build 与 release 全部成功。查看 `gh release view "$TAG" --repo vitechliu/pinkki-mc-mod --json url,tagName,assets`，确认每个目标各有正确命名的 JAR 和同名 `.sha256`。
- 三版本期望文件为 `pinkkicore-forge-1.20.1-<mod_version>.jar`、`pinkkicore-neoforge-1.21.1-<mod_version>.jar`、`pinkkicore-forge-1.19.1-<mod_version>.jar`，及对应的三个校验文件。文件名中的版本来自发行标签内元数据，不能混用其他提交或其他版本的旧文件。
- 将这六个指定附件下载到本次独立 `.local/` 目录，按校验文件逐项验证 SHA256；只数附件数量不足以证明完整性。忽略自动生成的 Source code ZIP/TAR，它们不是安装用 mod。
- 已有同标签 Release 会用 `--clobber` 替换所选目标的同名文件，保留其他附件；上传不是事务。中途失败时检查已有附件后再决定一次针对性重试，不删除整个 Release 或覆盖无关资产；异版本旧 JAR 不因重跑自动清除。
- 最终给出目标、标签和实际源码提交、run 链接、Release 链接、各版本 JAR 下载链接与 SHA256 核验结果，以及未验证范围。只完成文档/准备时明确说明未触发、未发布；不沿用旧运行作为本次结果。

本 skill 只处理构建/发行流程；没有改动发行内容时无需重复本地游戏部署。若为完成发布修改了游戏代码，适用的本地验证、HMCL 部署和玩家文档同步仍按 AGENTS.md 执行。
