# 2026-10-24：瓜巴工作台居中与瓜巴专用输入

- 变更 ID：2026-10-24-guaba-workbench-centering-input。
- 需求与完成行为：修正主面板偏左；在六面设置中追加“瓜巴输入”，专门向左上角瓜巴槽供料。
- 居中原因与修正：原 `GuabaWorkbenchScreen#init` 在父类初始化后，以 312 像素（主面板加抽屉预留）重新设置 `leftPos`；主面板实际宽 176，因此相对标准容器向左偏约 68 个 GUI 像素。移除该覆盖，恢复父类 `(width-imageWidth)/2` 水平居中。保留现有 JEI 点击区、绝对坐标扩展区域和动态抽屉避让，不以 JEI 物品列表宽度重新定位主面板。
- 端口语义：`GUABA_INPUT` 追加为编号 4，只接受向槽 0 插入瓜巴；拒绝其他物品和槽 1/2 插入、拒绝所有提取，瓜巴槽满时不流入原料槽。普通输入/双向保留原有槽 0/1 规则；输出、禁用、默认五面输入/底面输出不变。切换循环改为枚举长度；NBT、ContainerData、界面文案可承载新值，旧编号 0..3 不变。
- 源码位置：主版本 `forge/client/GuabaWorkbenchScreen.java`、`forge/workbench/{SideMode,GuabaWorkbenchEntity}.java` 与双语语言文件；扩展 `scripts/tests/workbench_probe/WorkbenchProbe.java`。
- Git 参照：工作开始 HEAD `27c5aaa`；保留用户已有 `designs/pinkki-skin/` 修改。不更改皮肤、转换配方、红石规则或次要目标。
- 验收场景：有/无 JEI、打开/收起设置时主面板中心一致；六面×五模式存取矩阵、模拟不修改库存、切换完整循环；专用供料拒绝错误物品与其他槽、满槽不串槽；模式编号 4 保存和菜单数据；旧 0..3 编号读取不变；真实漏斗夹带泥土和瓜巴时仅瓜巴进入左上槽，泥土留在漏斗；自动供给后的瓜巴可用于转换。客户端截图的最终居中和第三方管道仍由用户验收。
- 1.20.1 验证：`./scripts/mod.sh 1.20.1 build workbenchProbeJar -PworkbenchProbe --console=plain` 与 `python3 scripts/verify-artifact.py 1.20.1` 通过，日志 `.local/guaba-workbench-layout-io-build.log`、`-artifact.log`。发行 JAR SHA256 `7c3e7c5543abf9c7371be8e8deb5a18cc0ff29c810b6843c1febc54ab78fa5c8`。
- 独立无头回归：`python3 scripts/check-workbench.py --geckolib <4.4.9> --extra-mod <JEI 15.20.0.129>` 通过，日志 `.local/guaba-workbench-layout-io-runtime.log`；`.local/workbench-tests/1792847323488960000/result.json` 为 `passed=true`、`ready=true`、`stopped=true`、`errors=false`、`exit_code=0`。包含六面五模式、真实专用漏斗、满槽不串槽、新/旧 NBT 模式、菜单值 4、原有工作台功能回归。测试只在项目一次性服务端，使用 `--nogui` 和回环地址，不接触 HMCL 测试世界。
- 客户端静态检查：`javap -c -p` 检查编译后 `GuabaWorkbenchScreen#init` 调用父类初始化且不再写入 `leftPos`，输出 `.local/guaba-workbench-layout-bytecode.log`；这不等于实际客户端视觉验收。未启动图形客户端或操作用户游戏窗口。
- 文档检查：Node 24 下 `npm run wiki:build` 通过，日志 `.local/guaba-workbench-layout-io-wiki.log`；`git diff --check` 通过。本地 Wiki 源码与构建已同步，不代表线上 Pages 已部署。
- 玩家文档：已同步 Wiki 操作和玩家更新记录，玩家正文仅保留使用方法。自研游戏内手册仍未实现，需在 `guaba_workbench` 条目补充专用瓜巴供料说明；本次不能视作已完成游戏内手册同步。
- 热同步：`python3 scripts/hot-sync.py` 完成，回执 `.local/hmcl/hot-sync/20261024-211035-572511/receipt.json` 为 `status=ok`；clean/compat 两实例的语言覆盖均更新，两个既有测试世界的数据包无需变化。由用户按 F3+T 重载文案；数据包如需重载由用户执行 `/reload`。语言资源同步不能证明游戏内已启用或加载。
- 热同步边界：Java 居中和端口行为不能靠 F3+T 或 `/reload` 更新，需退出游戏后执行完整本地部署。本次未获本地部署请求，不替换用户运行实例的 JAR。
- 移植注意：目标版本的容器初始化、JEI GUI 扩展区域、物品 capability、模式编号持久化均需分别核对。仅验证 1.20.1，不更新次要版本检查点。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐 |
| 1.19.1 Forge | 待适配/验证 | 后续按需补齐 |
