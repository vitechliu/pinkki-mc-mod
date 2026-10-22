# GitHub 构建与发布

仓库为 [vitechliu/pinkki-mc-mod](https://github.com/vitechliu/pinkki-mc-mod)，主线为 `main`。

## 默认只处理 1.20.1

- `build.yml` 在分支 push / PR 时只构建 1.20.1，验证发行 JAR 并上传 JAR 与 SHA256，Actions artifacts 保留 14 天。
- `release.yml` 在推送 `v*` 标签时默认只发布 1.20.1。标签必须等于 `v` + `mod.properties` 的 `mod_version`。
- 多版本只能通过手动工作流显式选择，不根据修改到共享文件而自动扩展目标。
- 配置来源是 `config/minecraft-targets.json`，目标选择由 `scripts/targets.py` 校验。`targets` 支持逗号分隔的版本，`all` 表示明确选择全部已登记目标。

## 普通版本发布

1. 完成 1.20.1 功能及必要的无头验证，在 `docs/changes/` 记录行为、验收与次要目标待办。
2. 修改 `mod_version`，重新构建并校验 1.20.1。客户端测试由用户按需要手动执行，代理不默认启动游戏。
3. 提交并推送变更，等待默认 1.20.1 CI 成功。
4. 用户要求发布时创建匹配标签，例如：

```bash
git tag v0.1.0
git push origin v0.1.0
```

5. 查看 Release 工作流；默认附件仅为 1.20.1 的 JAR 与 SHA256。

日常无需执行 `build-all.sh`。工作流不会自动更新运行中的 Minecraft 实例。

## 按需构建与发布其他版本

先使用 `$port-minecraft-version` 补齐需要的目标，参见 [开发与适配流程](development-workflow.md)。

仅构建某个目标，可在 Actions → Build Minecraft targets → Run workflow 的 `targets` 填写 `1.21.1`，或 `1.20.1,1.21.1`。构建用于验证或诊断，不自动发布，也不自动推进适配检查点。

正式发布次要目标：

1. 完成该目标积压功能和验证，更新功能记录与适配检查点，一起提交。
2. 在要发行的提交上设置与 `mod_version` 匹配的标签。
3. Actions → Release Minecraft targets → Run workflow，`tag` 填写已有标签，例如 `v0.1.0`；`targets` 填写需要的目标。手动运行使用标签对应源码；新增适配代码不能只放在标签之后的提交里。
4. 工作流检查标签版本、次要目标的源码/适配层指纹，构建所有选定目标，核对附件数量与 SHA256，全部通过后发布。

若检查点已过期，先完成适配或重新验证。不要为绕过门禁直接刷新检查点。旧标签中没有新配置时，需要使用包含该流程的新标签。

推送标签可能先触发默认 1.20.1 发布；随后手动选择其他目标可为同一标签补充已验证附件。发布按标签串行执行。重跑更新所选目标的同名附件，保留未选目标已有附件；上传不是事务，中断后可重跑并核对校验和。

## 权限和验证范围

发布任务单独使用 `contents: write` 与仓库自带的 `GITHUB_TOKEN`，无需个人 Token。PR 构建不发布。工作流为 GitHub 托管 runner 编写，Gradle Action 管理缓存及 Wrapper 校验。

适配检查点记录的是已验证源码，不替代当前 CI。Actions 验证编译与包内容，不自动启动 GUI、不接受 EULA，也不证明整合包或生产兼容性。本次调整仅做本地脚本/工作流验证；需提交后才能验证远端运行。

`.local/`、`dist/`、缓存、测试世界和 IDE 个人配置已忽略；Wrapper JAR 必须提交。MIT 许可证在源码与发行 JAR 中，Minecraft 和加载器本体不随 mod 分发。

## 后续分发平台

当前自动分发使用 GitHub Releases。以后接入 Modrinth / CurseForge 时，为每个文件分别标注 MC 版本及 loader，Token 存放于 Actions Secrets，并沿用显式目标选择和适配完成检查。

官方参考：[可复用工作流](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows)、[触发工作流](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow)。

## 玩家 Wiki 发布

玩家 Wiki 由独立的 `.github/workflows/wiki.yml` 构建并发布到 GitHub Pages：`main` 上相关文件的 push 自动部署，PR 仅构建。它不依赖 mod 标签、不触发 Release，也不操作游戏实例。配置与维护见 [Wiki 维护说明](wiki.md)。
