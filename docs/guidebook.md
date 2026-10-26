# 1.20.1 独立引导手册

## 范围与使用

PinkkiCore 自行实现手册物品、客户端阅读界面与资源格式，不引入或内嵌 Patchouli、GuideME、Mantle 等手册库。现有 GeckoLib 前置不因手册改变。

`pinkkicore:guide_book` 最大堆叠为 1，由书与瓜巴无序合成，也加入创造模式“工具与实用物品”。右键打开原生 Screen，提供七个章节、中文标题/正文搜索、内部链接、返回历史、滚轮与键盘翻阅、同次游戏会话的阅读位置。阅读不暂停世界；首次正文为中文，英文语言环境使用中文正文与英文控件，不宣称已有英文翻译。

玩家操作见 [Wiki 手册页](../wiki/handbook.md)。当前不提供进度解锁、奖励、三维多方块或世界投影。

## 内容来源与构建

`scripts/generate-guide.py` 将指定的 `wiki/*.md` 转成版本 1 的手册 JSON。1.20.1 Gradle 的 `generateGuide` 任务在 `processResources` 前运行，生成文件仅放在 `versions/1.20.1/build/generated/guide/assets/pinkkicore/guide/zh_cn.json`，不提交生成物。修改入册 Wiki 正文后，重新构建并部署 JAR 才会更新安装的离线手册；只推送 GitHub Pages 或只执行现有资源热同步不能更新这份生成 JSON。

支持段落、二/三级标题、列表、表格的文本行、内部链接、粗体与行内代码；当前图片映射为真实物品模型，合成/转换/酿造组件转换为原生图。药水数值从既有共享定义读取。动态 Vue 不是游戏运行时的一部分，不应在手册源页随意新增组件；未支持的标签、未映射图片或失效链接会阻止构建，需要先扩展编译器和阅读器。新增手册章节时更新 `PAGES`，同步 Wiki 导航与原生图适配。

`GuideBook` 是不引用客户端类的内容模型；`GuideScreen` 和事件订阅位于 `client/guide`。专属客户端 Forge 事件打开手册，物品类只处理使用结果，没有客户端类引用。正文按客户端资源系统读取，允许资源包覆盖 JSON；重载资源后关闭再打开手册即可重新读取。格式错误时显示读取失败提示并保留日志。

配方区每次绘制查询当前 `RecipeManager`，支持有序/无序合成、配方删除提示、标签候选轮换、瓜巴加工的实际消耗和产物数量。瓜巴酿造从实际 Forge BrewingRecipe 注册读取普通药水输入、原料与输出，喷溅/滞留规则由正文说明；它不将一份网页图片当作当前世界配方。悬停显示物品提示。

## 验证命令

```bash
python3 -m unittest discover -s scripts/tests -p test_guide.py -v
./scripts/mod.sh 1.20.1 build guideProbeJar -PguideProbe
python3 scripts/verify-artifact.py 1.20.1
python3 scripts/check-guide.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar
npm run wiki:build
python3 scripts/deploy-local.py
python3 scripts/hot-sync.py
```

探针只放入一次性发行 JAR 测试服务端，不打入 mod。验证手册注册、最大堆叠、2×2 合成、错误材料拒绝、服务端右键、Wiki 入包、中文检索与配方 ID；服务端仅绑定回环地址，使用已确认 EULA 的本地独立测试安装，完成后正常停服。

## 本次证据与边界

- 构建与探针打包通过：`.local/guide-build.log`。发行校验通过，JAR SHA256 为 `6a677a0e56623e5cf4600e48b0d4d3713fdb2a03b9d8cfa52a4874c5ecc660b4`。
- 37 项 Python 测试通过，包含本次四项正文同步、原生组件映射、断链与未知组件检查：`.local/guide-python-tests.log`。
- 发行 JAR 无头服务端探针通过：`.local/guide-tests/1792983004012653000/result.json`、`console.log`；正常就绪、停服、退出码 0，无 ERROR。未安装第三方手册库。
- Wiki 静态构建通过；远端 Pages 未运行。
- 默认完整部署成功：`.local/hmcl/deployments/20261026-105117-816481/receipt.json` 为 `installed`，clean、compat 两个实际安装文件的 SHA256 均与本次 JAR 一致。受管资源/数据包热同步见 `.local/guide-hot-sync.log`，需玩家手动 F3+T、`/reload`；本次没有在游戏内重载。
- 未启动 Minecraft 客户端。实际字体、排版、不同 GUI 缩放、鼠标/键盘、物品提示、双手右键与完整整合包界面仍需手动验收，不能由编译或服务端探针替代。

手动验收建议：分别在 clean/compat 使用书与瓜巴合成手册；右键打开，搜索“六面”，检查正文链接与返回位置；查看瓜巴合成、工作台加工与酿造图并悬停；关闭重开、调整 GUI 缩放后检查阅读位置与裁剪。若只验收正式 JAR，先按热同步文档用 `hot-sync.py remove` 移除受管覆盖包。该说明不授权代理启动或操作游戏。

## 2026-10-26 工作区整理复验

- 本次将手册、瓜巴剑、瓜巴弓、雷霆瓜巴与对应玩家内容一起复验。1.20.1 `build workbenchProbeJar guideProbeJar thunderGuabaProbeJar -PworkbenchProbe -PguideProbe -PthunderGuabaProbe --console=plain`、发行 JAR 校验、37 项 Python 测试、Wiki 构建均通过。日志 `.local/tidy-20261026-closeout-{build,tests,wiki}.log`；Wiki 的 mcfunction 高亮回退提示不影响构建。未构建次要版本。
- 三组独立发行 JAR 无头回归全部通过：工作台及武器 `.local/workbench-tests/1793000104345937000/result.json`；手册 `.local/guide-tests/1793000105984545000/result.json`；雷击转换 `.local/thunder-guaba-tests/1793000158057066000/result.json`。三组均就绪、探针通过、正常停服，exit_code=0、errors=false。日志 `.local/tidy-20261026-closeout-{workbench,guide,thunder}.log`。包含实际玩家攻击、箭矢碰撞、元数据转换与网络同步、手册 2×2 合成/中文搜索/实际配方引用、真实雷击批量转换及重复保护。
- Python AST、JSON 解析、22 份 PNG 的 CRC、设计/运行/Wiki 图标一致性、雷击示意与发行图标一致性、凭证及本机绝对路径扫描、差异空白检查通过。保留可编辑设计源、探针源码和图标；现有忽略规则正确覆盖构建、缓存、日志、实例、回执和备份，无需调整。
- 本次完整部署成功：`.local/hmcl/deployments/20261026-153625-262714/receipt.json` 为 installed，clean、compat 均更新，独立核对两个实际安装 JAR、发行包及三组探针输入 SHA256 均为 `9b52515f0b1f878cfb29db4712833e53ca3fb45d8b7bd386a0db10480c047aed`。旧 JAR 已由脚本备份，其他模组、配置与世界保留。日志 `.local/tidy-20261026-closeout-deploy.log`。
- 热同步完成，`.local/hmcl/hot-sync/20261026-153636-297048/receipt.json` 为 ok，两个实例资源包与所有已有测试世界数据包均处理；日志 `.local/tidy-20261026-closeout-hot-sync.log`。玩家按 F3+T、执行 `/reload` 重载资源/数据；新注册和 Java 行为需启动新游戏进程。正式 JAR 验收前仍应移除受管覆盖包。
- 本轮未启动图形客户端；手册布局、鼠标键盘、武器显示/手感、JEI 界面、玩家实际雷雨操作及整合包客户端行为仍未验收。未验证远端 CI/Pages，也未发布标签、Release 或真实服务端。
