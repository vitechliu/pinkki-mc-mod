# 2026-10-22：瓜巴的三模组材料兼容试作

- 变更 ID：2026-10-22-guaba-material-compat
- 需求与完成行为：为 1.20.1 的寂静装备 Silent Gear、匠魂 Tinkers' Construct、Tetra 添加可选的瓜巴材料数据。基础耐久、采掘级等使用可运行的占位值；专属 traits/effects 为空，未擅自决定最终特性。不增加强制依赖、不内嵌第三方模组；瓜巴原有获取配方与说明不变。详细可行性、参数和后续扩展方式见 `docs/guaba-material-compatibility.md`。
- 源码位置：`versions/1.20.1/src/main/resources/data/pinkkicore/silentgear_materials/guaba.json`；同命名空间 `tinkering/materials/{definition,stats,traits}/guaba.json`、`recipes/compat/tconstruct/guaba_material.json`；`data/tetra/materials/wood/pinkkicore_guaba.json`；`assets/pinkkicore/tinkering/materials/guaba.json` 与双语语言文件。`scripts/check-guaba-compat.py`、`scripts/tests/compat_probe/` 仅用于无头验证，`verify-artifact.py` 扩展发行包边界校验；不改变生产 Java 代码或依赖配置。
- Git 参照：工作开始时 HEAD `70b05f9`，基于上一轮尚未提交的瓜巴实现继续。保留用户已有 `AGENTS.md` 修改和 `docs/guidebook-research.md`。
- 验收场景：不安装三个可选模组时正常加载，瓜巴原始合成仍在；单独安装任一模组及前置时识别瓜巴；三者共存时三个材料系统均识别相同原料。Silent Gear 用于主部件/杆部件；匠魂在部件制造台用于头、柄、绑定结，每个瓜巴提供 1 单位材料，并可进入维修材料映射；Tetra 派生接受木质类材料的模块变体，保留原有橡木变体，不把瓜巴加入木板物品标签。客户端后续人工检查着色、菜单、实际制作和维修。
- 1.20.1 构建：`./scripts/mod.sh 1.20.1 build --console=plain` 通过（`.local/compat-research/build.log`）；`python3 scripts/verify-artifact.py 1.20.1` 通过（同目录 `verify-artifact.log`）。本次发行包 SHA256：`39fa84e5fcf7b2ec514a65e1f7784958dabaf284ff00cc41765260d7b7832f19`。校验专属 JSON 入包、匠魂缺失依赖条件、原料映射、空特性与测试探针/第三方代码未进入发行包。
- 发行 JAR 无头安装矩阵：Forge 47.4.28 + GeckoLib 4.4.9。五组分别为无可选模组、Silent Gear 3.6.7 + Silent Lib 8.0.0、TConstruct 3.11.2.166 + Mantle 1.11.104、Tetra 6.17.0 + mutil 6.3.0，以及三个模组同时安装。五组均通过加载、服务器就绪、探针断言和正常停服，退出码 0，日志未出现 ERROR。证据 `.local/compat-research/matrix-results.json`、`matrix.log` 与其中列出的各新测试目录 `console.log`；依赖来源/哈希记录 `downloads.json`。服务端仅绑定 `127.0.0.1:25579`，使用全新测试世界，所有进程已退出。
- 探针证明：Silent Gear 实际注册及原料匹配；匠魂实际注册、可制作标识、head/handle/binding 三种属性、空 traits、1:1 原料映射；Tetra 实际材料数据、原料存在、占位数值、基础剑刃和剑柄模块的瓜巴派生变体，以及原有橡木变体仍在。探针不模拟玩家合成或界面操作。
- 可复现脚本：仓库脚本 `python3 scripts/check-guaba-compat.py` 的完整参数见调研文档。整理时修复参数局部变量遮蔽问题后，使用 `--cases all` 验证通过，证据 `.local/compat-research/reproducible-probe.log` 与 `.local/guaba-compat-tests/1792662326802520000/matrix-results.json`。该脚本核对固定第三方发行包 SHA512，测试 probe 只进入隔离实例，既不编译进 mod 也不部署到真实游戏实例。
- 文档检查：`npm run wiki:build` 通过（`.local/compat-research/wiki-build.log`）；Python 语法、JSON 解析、文档本地路径及 `git diff --check` 检查通过。
- 手册与 Wiki：同步瓜巴道具、配方、兼容说明、玩家更新记录及导航；注明 1.20.1 / 0.1.0 开发中未发布、占位属性及验证边界。自研游戏内手册系统仍未实现；后续以 `guaba` 条目补充三种材料系统说明并按当前游戏配方展示，不宣称本轮已完成游戏内手册同步。
- 未验证：客户端纹理、悬停、蓝图/部件制造台/Tetra 工作台的实际交互、玩家维修、多人与断线同步、旧世界升级、其他模组发行版本及完整整合包。未启动客户端、未提交推送、未执行 Pages/Release 部署。
- 移植注意：Silent Gear/匠魂材料 ID `pinkkicore:guaba`，Tetra 资源 ID `tetra:wood/pinkkicore_guaba`、材料 key `pinkkicore_guaba` 应保持稳定。次要目标需核对各自可用模组、材料/配方格式与加载方式，不能直接复制 1.20.1 兼容声明。未修改适配检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 需先确认三个目标模组的可用版本与资源/API，再按需接入 |
| 1.19.1 Forge | 待适配/验证 | 需核对精确 1.19.1 可用版本，不能以 1.19.2 代替 |
