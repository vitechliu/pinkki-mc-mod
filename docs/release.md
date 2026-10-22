# GitHub 发布

## 首次上传

仓库使用 `main` 分支。GitHub 上创建一个空仓库后，在本地执行以下步骤；把 `<仓库URL>` 替换成实际地址：

```bash
git add .
git commit -m "Initialize PinkkiCore multi-version mod"
git remote add origin <仓库URL>
git push -u origin main
```

`.local/`、Gradle 缓存、构建结果、测试世界、运行日志和 IDE 个人配置均已忽略；官方 Gradle Wrapper JAR 需要提交。MIT 许可证已包含在根目录及发行 JAR 中，Minecraft 和加载器本体不在发行 JAR 内。

## 工作流

- `build.yml`：分支 push、PR、手动触发；三个独立矩阵任务分别准备 JDK 17/21、构建、验证 JAR、生成 SHA256，上传保留 14 天的 Actions artifacts。
- `release.yml`：推送 `v*` 标签时，校验标签等于 `v` + `mod_version`，调用同一套三目标构建。全部成功后下载六个文件（3 JAR + 3 SHA256），统一创建 GitHub Release。任何目标失败都不会进入发布任务。
- 发布权限只在发布任务中授予 `contents: write`。使用仓库自动提供的 `GITHUB_TOKEN`，无需个人访问令牌。构建任务有读取权限，PR 构建不会发布。
- 相同标签重新运行时更新对应附件。GitHub 上传不是事务，若上传中断可重跑同一任务；消费者应在工作流成功后下载并核对校验和。

Gradle Action 校验 Wrapper 并管理缓存。工作流为 GitHub 托管 runner 编写；尚未接入自托管 runner。首次在 GitHub 运行才可证明远端权限、网络与 Actions 环境正常。

## 发布一个版本

1. 修改 `mod.properties` 的 `mod_version`；必要时更新 README 中示例和变更记录。
2. 执行 `./scripts/build-all.sh`，并完成主版本客户端及独立服务端测试；跨版本功能应测试全部目标。
3. 将变更提交到 `main`，等待三目标 CI 成功。
4. 创建并推送匹配标签，例如版本为 `0.1.0` 时：

```bash
git tag v0.1.0
git push origin v0.1.0
```

5. 在 Actions 查看 Release 工作流，检查 GitHub Release 是否包含三个目标 JAR 和校验文件。

标签代表该次提交的源码，不能用本机未提交修改生成标签期待远端包含修改。正式发布前还应安装发行 JAR 测试；Actions 当前验证编译和包内容，不会打开 GUI 或自动接受 Minecraft EULA。

## Modrinth / CurseForge

当前没有项目 ID，也没有平台 Token，因此首期自动发布目标是 **GitHub Releases**。未来接入时，为三个文件分别设置 MC 版本与 loader（Forge / NeoForge），把 Token 放 GitHub Actions Secrets，在全部构建成功后添加发布任务。平台草稿、审核、版本号规则都应先核对，不把 Forge 文件标成 NeoForge。

## 官方参考

- [GitHub Actions artifacts](https://github.com/actions/upload-artifact)
- [Gradle GitHub Actions](https://github.com/gradle/actions)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
