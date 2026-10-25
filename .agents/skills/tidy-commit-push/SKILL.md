---
name: tidy-commit-push
description: 整理 PinkkiCore 当前 Git 工作区，甄别源码与本地产物、维护忽略规则、完成适用验证和本地收尾，再按用户要求提交并推送。适用于“该 ignore 的就 ignore，然后 commit 并 push”等请求；单纯状态查询、只读审核或仅创建本 skill 不授权提交推送。
---

# 整理工作区并提交推送

以仓库根目录为工作目录。先读取当前 `AGENTS.md`，遵守本轮用户的范围和限制。项目版本、部署与热同步规则以该文件为准，不复用本会话或历史文档的通过结论。

## 1. 盘点与归类

先说明将检查工作区并整理提交，读取：

```bash
git status --short --branch
git status --short --untracked-files=all
git diff --stat
git diff --cached --stat
git diff -- .gitignore
git log -3 --oneline
git remote -v
```

按范围阅读实际 diff、未跟踪文件和适用的嵌套协作规范；同时检查已有暂存内容。区分本次游戏功能、资源、设计、工具、文档和环境产物。用户要求整理整个工作区时，可纳入已有相关修改，不因不是自己编写就遗漏；发现并行写入、来源不明的异常改动或敏感文件时，先保留并查清，避免覆盖或误提交。

## 2. 忽略规则

用 `git status --short --ignored` 和 `git ls-files -ci --exclude-standard` 检查覆盖范围。

- `.local/`、依赖/构建缓存、运行目录、世界、日志、安装回执、备份、IDE 个人配置、临时编译产物与本地启动器不提交。优先使用已有目录规则，新增规则只覆盖实际需要；不要为了产生改动而改 `.gitignore`。
- 源码、配置模板、锁文件、测试探针源码、设计源工程及贴图、可重复生成所需脚本、明确交付的离线审阅页、项目级 `.agents/skills/` 应保留。
- 不使用 `*.jar` 等宽泛规则误排除已跟踪的各目标 Gradle Wrapper JAR。根目录 HMCL 下载可使用 `/HMCL-*.jar`。
- 对待提交文本定向检查凭证、账号信息和本机绝对路径。配置改用可移植路径，真正的本机配置移至忽略目录；不要打印凭证值。
- 已跟踪文件不会因新增 ignore 自动退出索引。确认确为不应跟踪的本地产物后，才对明确路径使用 `git rm --cached`，保留本地文件并检查暂存差异。

使用 `git check-ignore -v <代表路径>` 核对新增规则；正确覆盖且没有多余文件时保持规则不变。

## 3. 按改动范围验证与收尾

采用足以覆盖改动的检查，不把每次整理变成完整发布流程。

| 范围 | 适用检查 |
| --- | --- |
| 纯 Markdown、协作规则或 skill | 内容、引用路径、格式；skill 使用可用的创建工具校验 |
| Wiki/组件/配方展示 | `npm run wiki:build`；依赖或锁文件变化时先 `npm ci --cache .local/npm-cache`，已有缓存可加 `--offline` |
| Python 工具 | 语法检查及相关 `unittest`；项目脚本回归可用 `python3 -m unittest discover -s scripts/tests -v` |
| 游戏代码、注册、依赖或 JAR 内容 | `./scripts/mod.sh 1.20.1 build --console=plain`，成功后 `python3 scripts/verify-artifact.py 1.20.1` |
| 模型/贴图 | 已有资产校验脚本及适用的发行包一致性检查；不重新生成或重设计已确认资产 |
| 改动确需运行验证 | 选择已有无头冒烟/专用探针；探针构建属性和命令从当前变更文档核对 |

始终执行 `git diff --check`，提交前也执行 `git diff --cached --check`。默认只处理 1.20.1，不启动图形客户端，不跑空 GameTest，不以探针编译代表探针执行。检查失败先判断源码问题还是环境限制；沙箱阻止 Gradle 本地端口时，可按工具流程申请执行必要构建，不将该限制直接判为产品故障。

游戏行为/资源变化应已有对应 `docs/changes/`，玩家可见变化应同步 Wiki，手册尚未实现时记录缺口。整理任务不擅自扩大功能或更新次要版本检查点。

当前 `AGENTS.md` 要求的本地收尾也必须执行：

- 本轮涉及发行 JAR 内容并重新构建、适用检查通过后，使用 `python3 scripts/deploy-local.py` 完整更新 clean/compat；核对本次 `receipt.json` 及两个实例 JAR 与发行包的 SHA256。不能用手工复制或旧回执代替。
- 适用资源/数据修改执行 `python3 scripts/hot-sync.py`，核对本次回执，提示 F3+T / `/reload`；热同步不能代替 Java/JAR 部署。正式 JAR 验收才按项目要求移除覆盖包。
- 游戏运行、缺少实例、依赖冲突等导致部署受阻时，保留已完成验证，明确未更新的实例；不关闭游戏、不绕过检查。可独立完成 Git 提交推送，并如实报告本地部署阻碍。
- 纯文档、skill、Wiki、未接入游戏的设计稿无需构建部署 mod。用户指定只读、仅构建或暂不部署时遵守其范围。

## 4. 提交与推送

只有当前用户请求包含提交/推送时才执行相应操作；调用 skill 本身不额外授予发布、标签或远程部署权限。

1. 简要说明本次内容、ignore 结论和实际验证结果。
2. 用明确路径暂存检查过的文件，复查暂存统计及差异，确保没有漏掉已有暂存内容或混入验证产物。
3. 用概括最终行为的中文消息创建 commit。工作区无改动时不制造空提交；若存在已授权的未推送提交，仍需完成推送。
4. 推送当前分支到已确认的 remote/upstream。本项目通常为 `origin/main`，每次实际核对，不擅自切分支、改 remote 或建立版本长期分支。upstream 缺失或目标确有歧义时才询问。
5. 普通 push 被拒绝时读取原因；远端前进先 fetch 检查双方差异，必要时合并并解决明确冲突后复验。不得 force push、丢弃改动或盲目重试；存在无法确定取舍的冲突时报告具体阻碍。
6. 推送完成后核对 `git status --short --branch` 及 `git rev-list --left-right --count HEAD...@{upstream}`。只有工作区无遗漏且双方为 `0 0` 才报告干净且同步。

最终回复简洁列出提交短 SHA 与主题、目标分支、ignore 处理、实际验证和工作区状态。涉及部署/热同步时说明实际安装与重载范围；区分未执行的客户端、服务端运行、远端 CI 和 Pages 验证。push 可能触发已有 CI/Pages，但本地构建成功不代表远端成功。
