# 2026-10-28：瓜巴头套与粉红之祝

- 变更 ID：2026-10-28-curios-accessories
- Git 参照：`5b5fee5693aa656d3e8f3f2422772eeb04c39fe4`。保留工作开始时已有的 NPC 等未提交修改，本次未提交或推送。
- 需求与完成行为：1.20.1 可选 Curios 5.14.1+（5.x）。瓜巴头套位于 `head`，免疫全部瓜巴家族效果，并清除佩戴前已有的效果；粉红之祝位于 `charm`，提供护甲 +2、最大生命值 +2（1 颗心），卸下清理。各分配 1 格，`SET` 不累加其他模组槽位，保留数据包调整。每组 1 个，创造模式战斗分类。
- 配方：头套为 `PGP / P P / 空行`（P=粉红锭，G=瓜巴）；护符为 ` P  / P P /  P `，各产出 1 个。原版有序配方允许头套上下平移和镜像，不要求冗余空白行。
- 家族契约：新瓜巴效果继承 `GuabaEffect`，或实现 `GuabaEffectFamily`。雷霆瓜巴已有继承关系，无需枚举名称、药水瓶或强度；饮用、喷溅、滞留和武器施加均经过效果适用事件。未安装 Curios 时药水机制保持正常、饰品提示前置缺失。
- 属性：使用 Curios 传入的槽位 UUID 和瞬态属性；整合包扩槽后多件祝福可叠加。仅有效装备栏生效，背包、原版头盔栏、外观栏不生效；无角色饰品模型渲染。
- 源码：`versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/compat/curios/`、`item/AccessoryItem.java`、`item/ModItems.java`、`effect/GuabaEffectFamily.java`、`effect/GuabaEffect.java`、`PinkkiCore.java`。共享层仅增加可选依赖模板占位，不添加 Minecraft 相关共享 Java 代码。
- 资源：主版本 `data/pinkkicore/curios/`、`data/curios/tags/items/`、`recipes/{guaba_hood,pink_blessing}.json`；对应物品模型、32×32 透明图标和中英文名称/提示。图标由内置 imagegen 生成并缩小；源图及提示见 `designs/accessories/README.md`。
- 玩家内容：`wiki/items.md`、`wiki/recipes.md`、`wiki/changelog.md`；真实配方 JSON 驱动配方格，手册从相同页面生成，图标映射在 `scripts/generate-guide.py`。本地 Wiki 构建通过，未推送及确认线上 Pages。
- 部署：新增 `deploy-local.py --with-curios`，校验已安装版本、加载器、Minecraft 范围、重复和文件名冲突；缺少时安装 `config/hmcl-runtime.json` 固定 SHA512 的原始 Curios JAR，不打包进 PinkkiCore。不传参数保留原部署行为。

## 1.20.1 验证

- `./scripts/mod.sh 1.20.1 build accessoryProbeJar -PaccessoryProbe`：构建、探针重混淆通过，日志 `.local/accessories-build.log`。
- `python3 scripts/verify-artifact.py 1.20.1`：元数据、Curios 可选性与未捆绑、槽位/标签、模型、PNG、真实 Wiki/手册内容一致性校验通过。
- `python3 scripts/check-accessories.py --geckolib <本地原始 GeckoLib JAR> [--extra-mod <原始 Curios JAR>]`：分别验证安装和未安装 Curios 的一次性无头发行 JAR 服务端；绑定 127.0.0.1，使用项目测试 EULA，正常 stop 并确认退出，不使用 HMCL 世界。日志 `.local/accessories-final-probe.log`，原始日志及 SHA256 在 `.local/accessory-tests/*/{console.log,result.json}`。
- 探针验证真实九宫格配方/多余材料拒绝、各 1 格槽位、标签接受/拒绝、已有瓜巴及雷霆瓜巴清除、两者 I～IV 免疫、速度效果仍可添加、卸下头套恢复受效果、三轮护符穿脱无属性残留。未安装 Curios 时注册、配方和瓜巴效果正常。
- `python3 -m unittest discover -s scripts/tests -p 'test_hmcl_deploy.py'`：12 项通过，包含 Curios manifest 版本、重复、旧版、6.x、错误加载器/MC 拒绝；日志 `.local/accessories-deploy-tests.log`。
- `python3 -m unittest discover -s scripts/tests -p 'test_guide.py'`：4 项通过，日志 `.local/accessories-guide-tests.log`。
- `npm ci`、`npm run wiki:build`：通过，日志 `.local/accessories-wiki-install.log`、`.local/accessories-wiki.log`。
- `python3 scripts/deploy-local.py --with-curios`：回执 `.local/hmcl/deployments/20261028-175001-605585/receipt.json` 为 `installed`；clean、compat 的 PinkkiCore SHA256 均为 `af6d977742060b2d6ecfba163e073ebb942db22cc76f961d6e2ea596cf444140`，与最终发行 JAR 一致，均已安装 Curios。未启动游戏。
- `python3 scripts/hot-sync.py`：同步两个实例资源及已有测试世界数据包，记录 `.local/hmcl/hot-sync/20261028-175027-208176/`。如已启用覆盖包，游戏内需 F3+T 与 `/reload`；正式 JAR 验收前执行 `hot-sync.py remove` 并手动重载。

## 待人工验收

不启动客户端。用户可手动检查 Curios 的 head/charm 装备界面、图标/提示及手册配方格；验证药水饮用、喷溅、滞留、瓜巴武器命中下的实际体验。退出重进、死亡重生、数据包扩槽与其他模组属性组合未完成实际交互验收。默认沿用 Curios 死亡掉落/保留规则，不改背包保留契约。已有 NPC 改动进入此次完整 JAR，不把本次饰品探针当作 NPC 回归结论。

## 移植注意

只开发 1.20.1 Forge。其他版本需分别核对 Curios/饰品 API、事件总线、能力注册、槽位数据格式与属性修饰符接口，不能复用二进制或声称兼容。未更新适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 按需核对 NeoForge 对应饰品 API 与数据格式 |
| 1.19.1 Forge | 已验证 | 已补齐并通过适用无头检查，见 [完整适配记录](2026-10-28-port-1.19.1.md)；客户端未验 |

## 工作区整理复验与部署收尾

- 本轮统一复验瓜巴影融 NPC、人形实体/渲染共用提取、Curios 饰品与部署脚本。1.20.1 `build pinkkiProbeJar accessoryProbeJar -PpinkkiProbe -PaccessoryProbe --offline --console=plain`、发行校验、39 项 Python 回归和 Wiki 构建通过；日志 `.local/tidy-20261028-{build,tests,wiki}.log`。发行 SHA256 为 `af6d977742060b2d6ecfba163e073ebb942db22cc76f961d6e2ea596cf444140`，未构建次要版本。
- 瓜巴影融与粉红妹分别完整无头回归通过：`.local/guaba_yingrong-tests/1793182929892814000/result.json`、`.local/pinkki-tests/1793182983378836000/result.json`，日志 `.local/tidy-20261028-{yingrong,pinkki}.log`。命名、指令/刷怪蛋、属性、寻路拾取、六槽装备/保存、择优替换、mobGriefing、拾取延迟、反击、死亡掉装与游荡通过，external_auth_unavailable=false。
- 未安装与实际安装 Curios 的饰品探针通过：`.local/accessory-tests/1793182932091205000/result.json`、`.local/accessory-tests/1793182957033688000/result.json`，日志 `.local/tidy-20261028-accessory-{absent,present}.log`。覆盖条件加载、真实配方、槽位/标签、已有效果清除、普通/雷霆瓜巴 I–IV 免疫、无关效果、卸下恢复和反复穿脱属性无残留。四组均正常就绪、通过、停服，exit_code=0、errors=false，使用同一发行 SHA256。
- 皮肤校验、两份工程内嵌纹理和运行纹理一致性、Python AST/JSON、8 份 PNG CRC、凭证/本机路径扫描及差异空白检查通过，日志 `.local/tidy-20261028-skin.log`。未重生成设计稿。现有忽略规则覆盖缓存、构建、实例、世界、日志、回执和备份，无需调整；设计源与模型工程保留。
- 已执行 `python3 scripts/deploy-local.py --with-curios`，但 `.local/hmcl/deployments/20261028-182357-852669/receipt.json` 为 failed、instances=[]：目标游戏仍在运行，脚本安装前阻止操作。本次 clean、compat 均未更新 JAR，构建完成、部署待退出游戏。日志 `.local/tidy-20261028-deploy.log`；未关闭游戏或绕过保护，不沿用本文件上方旧回执作为本次安装证明。
- 热同步成功，`.local/hmcl/hot-sync/20261028-182406-288652/receipt.json` 为 ok，两个实例资源与所有已有测试世界数据包均处理，日志 `.local/tidy-20261028-hot-sync.log`。玩家按 F3+T、执行 `/reload` 重载资源/数据；Java 修改需退出游戏后继续完整部署，从新 JAR 启动生效。
- 未启动 Minecraft 图形客户端，未验收角色渲染、装备界面、实际药水/武器体验或完整整合包交互；远端 CI/Pages 未验证，未发布标签、Release 或真实服务端。

## 2026-10-28：1.19.1 按需适配

本项对应源码/资源已纳入 1.19.1 Forge 适配。构建、发行 JAR 校验、相关独立无头场景、实际日志及证明边界统一记录于 [1.19.1 适配与验收清单](2026-10-28-port-1.19.1.md)。不从无头通过推断 GUI、真实多人或其他目标兼容。。
