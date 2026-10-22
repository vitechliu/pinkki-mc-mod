# 玩家 Wiki 维护

玩家 Wiki 使用 VitePress 1.6.4，源文件放在 `wiki/`，开发资料仍放在 `docs/`。这是独立的文档构建，不需要编译或启动 Minecraft。

## 本地操作

使用 Node.js 24 和 npm，在仓库根目录执行：

```bash
npm ci
npm run wiki:dev
```

构建和预览发行页面：

```bash
npm run wiki:build
npm run wiki:preview
```

命令输出带 `/pinkki-mc-mod/` 的本地地址。预览默认只绑定回环地址。`node_modules/`、Wiki 缓存和构建产物不提交；`package-lock.json` 必须提交，CI 使用 `npm ci`。

## 内容同步

每次面向玩家的玩法变更，在同一次提交中更新对应条目及 `wiki/changelog.md`。道具、配方、结构、多方块、生物与 Boss 均适用；移除功能也必须删除或标记旧说明，并修正相关链接。

正式条目写明适用 Minecraft 版本、mod 版本及实现/发布状态。设计稿标为设计中，未实现的占位页不编造属性、合成或掉落。次要目标未适配时明确说明，不从主版本推断支持情况。

新增条目时同步 `wiki/.vitepress/config.mts` 的导航或从分类页添加入口。图片随文档提交并使用相对链接。开发与适配记录仍按 `docs/changes/` 流程维护。

## GitHub Pages

仓库 Settings → Pages → Source 选择 **GitHub Actions**，用户已完成该设置。`.github/workflows/wiki.yml` 在 `main` 上的 Wiki 源码、依赖或自身配置变更时构建并部署；PR 仅构建，不发布。也可在 `main` 上手动运行 **Build and deploy Wiki**。

默认网址为 <https://vitechliu.github.io/pinkki-mc-mod/>；配置中的 `base` 与仓库路径一致。更换仓库名或自定义域名时同步调整。工作流使用仓库自带 Token，无需新增个人 Token。

Pages 工作流独立于 mod 构建与 Release；发布 Wiki 不发布 mod，也不部署或重启游戏服务端。Wiki 构建失败（包括无效内部页面链接）会阻止该次文档部署；它不能自动判断说明与游戏行为是否一致，仍需审查内容。

## 验证范围

本地构建通过不代表远端 Pages 已发布。提交并推送后，需要查看 Wiki 工作流及部署网址才能确认在线结果。占位 Wiki 不涉及游戏功能变更，无需创建版本适配待办或运行游戏测试。

当前稳定版工具链的 `npm audit` 报告 3 项开发依赖告警（2 moderate、1 high，涉及 Vite/esbuild），暂无该稳定依赖范围内的自动修复。开发与预览服务器仅绑定 `127.0.0.1`；GitHub Pages 仅接收构建后的静态文件。这不代表开发依赖告警已修复，后续应随 VitePress 稳定版升级复查。预览使用 Vite CLI，避免 VitePress 1.x 预览命令忽略 host 参数。
