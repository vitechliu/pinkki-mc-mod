# 2026-10-25：修复瓜巴药水 JEI 获取来源缺失

- 变更 ID：2026-10-25-guaba-potions-jei。
- 原问题：自定义匿名 `IBrewingRecipe` 可以被 Forge 酿造台执行，但 JEI 的 Forge 酿造适配器只枚举 `VanillaBrewingRecipe` 与标准 `BrewingRecipe`；因此基础瓜巴、延长和增强配方缺失。工作台有自己的 JEI 插件，原版瓶型转换由原版酿造适配器处理，所以其他条目可显示。
- 源码证据：本地 JEI 15.20.0.129 JAR 的 `mezz.jei.forge.platform.BrewingRecipeMaker#addModdedBrewingRecipes` 字节码按 `instanceof BrewingRecipe` 筛选，读取 `getInput/getIngredient/getOutput`；参考源码中较新版本的 `BrewingHelper` 同样仅给标准类型注册扩展。无需把所有自定义酿造推断为 JEI 自动支持。
- 完成行为：以标准 `BrewingRecipe` 子类注册三个瓶型 × 三条酿造规则，共九条可枚举配方；输入候选保留药水 NBT，输出瓶型确定。继续按瓶型和 Potion 匹配真实输入，保留重命名和其他原有标签。配方来源仍为 `guaba_potions.json`，无第二套 JEI 配方表，不新增 JEI 强制依赖，不更改原配方、时长、等级上限或普通获取渠道。
- 源码位置：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/effect/GuabaPotions.java`；回归探针 `scripts/tests/guaba_effect_probe/GuabaEffectProbe.java`；Wiki `wiki/guaba-potions.md`、`wiki/changelog.md`。
- Git 参照：开始 HEAD `27c5aaa`；保留工作区已有工作台与皮肤等改动，不修改其实现。
- 验收场景：JEI 按瓜巴药水查询酿造来源、按瓜巴查看用途；三个瓶型的基础/延长/增强候选均包含正确药水 NBT；预览输出与实际酿造一致；重命名瓶继续酿造并保留名称；水瓶不能代替粗制药水；原有酿造矩阵、属性采样、周期和过期行为无回归。
- 回归证据：新探针运行旧发行包，失败 `JEI standard brewing recipes: expected 9, got 0`，且正常停服；日志 `.local/guaba-jei-before.log`，隔离目录 `.local/guaba-effect-tests/1792914557377488000/`。修复后的验证与部署记录见下方。
- 玩家文档与手册：Wiki 补充酿造查询方法和玩家修复记录；自研游戏内手册尚未实现，`guaba_potions` 接入缺口沿用原功能记录，不能视为已同步游戏内阅读。
- 未验证范围：不启动图形客户端；无头验证证明 JEI 标准接口的数据可枚举且与酿造一致，不替代 JEI 界面、按键查询及整合包客户端实机验收。未推送、发布 Release 或部署真实服务端。
- 移植注意：仅 1.20.1 Forge。其他目标需要核对酿造类型、NBT 与 JEI 的平台适配，不更新适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐 |
| 1.19.1 Forge | 待适配/验证 | 后续按需补齐 |

## 本轮验证与部署结果

- `./scripts/mod.sh 1.20.1 build guabaEffectProbeJar -PguabaEffectProbe` 成功，日志 `.local/guaba-jei-build.log`；`python3 scripts/verify-artifact.py 1.20.1` 成功，发行 SHA256 `88091ad0619a9e20d3339c3fef9ccea6fe9fc8ea40ce0e63e8af8255a6700965`。
- `python3 scripts/check-guaba-effect.py --geckolib .local/hmcl/runtime-cache/geckolib-forge-1.20.1-4.4.9.jar` 修复后通过：九条标准配方的完整 NBT、预览与实际产物一致、命名保留、水瓶拒绝；原有三个瓶型酿造、真实三瓶酿造台、I–IV 1600 次采样、重载及自然过期均通过。日志 `.local/guaba-jei-after.log`；隔离测试 `.local/guaba-effect-tests/1792914586307340000/result.json`：passed/ready/probe_pass/stopped=true，exit_code=0，errors=false。
- `npm ci --cache .local/npm-cache --offline`、`npm run wiki:build` 成功，日志 `.local/guaba-jei-npm-ci.log`、`.local/guaba-jei-wiki-build.log`。`git diff --check` 通过。
- 已执行 `python3 scripts/deploy-local.py`；脚本检测到目标游戏仍在运行并阻止安装。此次回执 `.local/hmcl/deployments/20261025-155023-994417/receipt.json` 为 failed、instances=[]。clean、compat 两个实例均未由本次部署更新。构建完成、部署待退出游戏；未关闭、重启或操作游戏，未绕过运行状态检查。
- `python3 scripts/hot-sync.py` 成功，同步两个实例及已有测试世界覆盖包；记录 `.local/hmcl/hot-sync/20261025-155024-136975/`，日志 `.local/guaba-jei-hot-sync.log`。F3+T、`/reload` 仅重载资源/数据，不能让这次 Java 酿造修复生效，仍需退出游戏后完整部署。

## 2026-10-25 工作区整理复验与部署收尾

- 重新执行 1.20.1 `build workbenchProbeJar guabaEffectProbeJar -PworkbenchProbe -PguabaEffectProbe --console=plain` 和发行包校验通过；发行 SHA256 与上方一致。构建日志 `.local/tidy-20261025-build.log`。默认沙箱禁止 Gradle 本机端口，按工具审批流程放宽沙箱后构建成功。
- 工作台独立无头回归通过，覆盖六面五模式、真实漏斗专用供料、满槽隔离、旧模式编号及菜单/NBT；结果 `.local/workbench-tests/1792915027224319000/result.json`。药水回归同时装载 JEI 15.20.0.129，通过九条标准配方及原有酿造、属性、周期和过期检查；结果 `.local/guaba-effect-tests/1792915058273591000/result.json`。两次服务端均就绪、探针通过、正常停服，exit_code=0、errors=false；日志 `.local/tidy-20261025-workbench.log`、`.local/tidy-20261025-guaba-effect.log`。
- Python 回归 33 项通过、Wiki 构建通过（仅有 mcfunction 高亮回退提示），日志 `.local/tidy-20261025-python-tests.log`、`.local/tidy-20261025-wiki.log`；新增整理 skill 校验、皮肤 PNG CRC/尺寸、生成器与 PNG 一致性、两份模型内嵌纹理及几何不变检查通过。皮肤仅为设计资产，不接入本次发行 JAR。
- 本次完整部署已成功解除此前待退出阻碍：`.local/hmcl/deployments/20261025-155723-702606/receipt.json` 为 installed，clean、compat 两个实例均已更新；独立重新计算两个已安装 JAR 的 SHA256，均与本次发行产物一致。保留第三方 mod、配置、存档并由脚本备份旧 JAR。日志 `.local/tidy-20261025-deploy.log`。
- 资源与所有已有测试世界数据包热同步完成，`.local/hmcl/hot-sync/20261025-155739-446134/receipt.json` 为 ok，日志 `.local/tidy-20261025-hot-sync.log`。游戏内资源/数据重载由用户按 F3+T、执行 `/reload`；正式发行 JAR 验收前按项目流程移除受管覆盖包。
- 现有忽略规则正确覆盖本地缓存、构建、启动器、日志、回执和备份，无需修改；设计源文件、探针源码和项目 skill 保留。手册缺口沿用上方记录；本轮未启动图形客户端，未验证第三方管道、客户端界面或远端 CI/Pages。
