# HMCL 资源热同步

`scripts/hot-sync.py` 只处理项目内的 Minecraft 1.20.1 实例，要求 Python 3.11+，支持 macOS/Linux。用户已确认这两个项目实例的所有世界均为测试世界；默认自动处理它们全部已有世界，无需指定名称。它无需 IDEA，不启动游戏，不编译 Java，不替换 mod JAR。可以在用户手动启动的游戏运行期间同步；同步完成后由用户触发游戏重载，避免在文件写入过程中重载。

## 贴图、语言、模型和动画

在项目根目录执行：

```bash
# 默认同步 clean、compat 的资源包和全部已有世界的数据包
python3 scripts/hot-sync.py

# 仅同步一个实例
python3 scripts/hot-sync.py --profile compat
```

首次在对应游戏实例的“选项 → 资源包”启用 **pinkkicore-dev-overlay**，放到最高优先级。以后每次修改资源后执行命令，再在游戏内按 **F3+T**（Mac 视键盘设置配合 Fn）。命令不会替你操作界面或修改 `options.txt`。受管目录：

```text
.local/hmcl/.minecraft/versions/PinkkiCore-1.20.1-<实例>/resourcepacks/pinkkicore-dev-overlay/
```

同步来源是共享和 1.20.1 资源目录的 `assets/`，版本专属文件覆盖同名共享文件，保持所有原 namespace 与相对路径；包含 PNG、语言、模型、GeckoLib 模型和动画 JSON。游戏内模型与活动动画的实际刷新仍需手动验收，必要时重新生成测试实体。

## 配方等数据：默认自动同步全部测试世界

每次运行 `python3 scripts/hot-sync.py`，自动发现两个实例 `saves/` 下含 `level.dat` 的世界并同步数据包，不按最后访问时间猜测。新建世界会在下一次同步自动纳入；无世界的实例只同步资源包。跳过符号链接和不完整的世界目录，不创建世界，也不修改 `level.dat`、区块等存档文件。

可选的范围控制：

```bash
# 查看本次可发现的世界
python3 scripts/hot-sync.py list-worlds

# 仅同步两个实例全部世界的数据包
python3 scripts/hot-sync.py --data-only

# 仅同步客户端资源，不写世界数据包
python3 scripts/hot-sync.py --resources-only

# 如需缩小范围，仍可指定单个实例及其 saves 下的世界文件夹名
python3 scripts/hot-sync.py --profile clean --world '测试世界文件夹名'
```

`--world` 是可选筛选项，指定时必须选择单个实例，不接受路径；`--data-only` 默认也处理全部已有世界，`--resources-only` 不能与 `--world` 同用。受管目录：

```text
<实例>/saves/<指定世界>/datapacks/pinkkicore-dev-overlay/
```

进入所选世界后执行 `/reload`。首次用 `/datapack list` 检查，如未启用则执行：

```text
/datapack enable "file/pinkkicore-dev-overlay" last
```

上述操作需要命令权限。仅限项目内这两个 HMCL 实例的本地测试世界，多人服务器应另行更新其服务端数据包。同步所有源 `data/` namespace，包括 PinkkiCore 配方与对其他 mod 的数据兼容条目；第三方 mod 的自定义数据是否接受 `/reload`，必须按对应 mod 行为验收。

这适合已有配方类型的配方、标签、战利品等修改，不会热注册新物品、新方块或配方类型。Java 逻辑仍使用完整部署或独立的 IDEA HotSwap 流程。详细边界见 [可行性调研](hot-reload-feasibility.md)。

## 状态、日志与退出覆盖模式

```bash
# 检查两个实例资源包和全部测试世界数据包的清单及文件哈希
python3 scripts/hot-sync.py status

# 检查单个世界的数据覆盖包
python3 scripts/hot-sync.py status --profile clean --world '测试世界文件夹名' --data-only

# 正式发行 JAR 验收前，移除两个实例的受管资源包及全部世界的数据包
python3 scripts/hot-sync.py remove

# 同时移除此实例资源包与指定测试世界的数据包
python3 scripts/hot-sync.py remove --profile clean --world '测试世界文件夹名'
```

移除包后，手动检查资源包/数据包列表并重载，确认没有开发覆盖再验收发行 JAR。默认 `remove` 与同步范围一致，会处理两个实例全部已有世界，只删除本工具的受管覆盖包并备份，不删除世界或其他数据包。完整部署命令保留覆盖包，但会提示它们可能遮住新 JAR 的资源。

每次同步、移除或状态检查的记录位于 `.local/hmcl/hot-sync/<时间>/receipt.json`，包含选择的实例/世界、处理结果、变更和删除文件清单。受管包内的 `.pinkkicore-hot-sync.json` 保存文件 SHA256；更新/移除前备份旧包到该次记录的 `backups/`。`hmcl-test.py report` 也会记录本实例已准备的覆盖包清单，清单不等于游戏内已启用或重载成功。

所有待同步 JSON 和全部选定目标先检查再开始写文件。同名非受管目录、手动修改/删除受管文件、额外文件、路径越界或符号链接会拒绝处理，不使用强制覆盖。遇到人工编辑冲突，先自行保存修改并移出整个受管包目录，再重新生成；后续资源修改以仓库源码为准。工具只清理清单内过期资源，不接管其他资源包、mod、配置或世界文件。若写入阶段发生磁盘错误，回执会标为失败且可能已有部分更新；修复错误后结合备份处理，失败期间不要触发游戏重载。

## 验证记录

2026-10-23 完成 16 项临时目录内的文件测试，新增默认双实例多世界同步、新世界自动发现、无世界与链接世界处理、全部世界受管移除以及仅资源模式等覆盖；原有覆盖包括：双实例资源同步、版本覆盖优先级、指定世界数据同步、非法世界参数拒绝、改名清理与备份、无效 JSON 在写入前拒绝、同名非受管目录保护、人工修改保护、符号链接拒绝、受管移除、清单越界与额外文件拒绝、只读状态及诊断报告快照。

```bash
python3 -m unittest discover -s scripts/tests -p test_hot_sync.py -v
```

最初的仅资源同步记录为 `.local/hmcl/hot-sync/20261023-170750-351180/`。用户授权全部世界后，已改为默认资源与全部已有世界的数据包同步，本次结果位于 `.local/hmcl/hot-sync/20261023-172314-878880/receipt.json`，确认 clean、compat 各一个“新的世界”已安装数据覆盖包，两个实例资源覆盖包均已同步。没有启动游戏或执行图形验收；配方与资源的实际重载、JEI 刷新仍待用户手动验证。本次仅新增开发工具，不改变玩法或游戏资源，无需同步玩家手册/Wiki、执行游戏构建或推进次要版本检查点。
