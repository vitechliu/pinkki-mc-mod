# 三平台整合包依赖调研（2026-10-30）

本次针对 Minecraft 1.19.1 Forge、1.20.1 Forge、1.21.1 NeoForge 检索 CurseForge、Modrinth 和 FTB 的热门公开整合包，读取固定发行版本的真实清单，调整 PinkkiCore 的编译基线与安装门槛。目标是减少不必要的前置升级，不承诺消除所有 mod 交互冲突。

## 取样与可追溯性

- Modrinth 按下载量检索三个精确游戏版本和加载器，检查候选项目的最新及较早正式发行；从 `.mrpack` 的 `modrinth.index.json` 再核对游戏与加载器。成功获取 20 份快照，另外 7 次查询或提取未取得合适清单，保留失败记录。项目搜索标签不直接作为兼容证据。
- FTB 从官方 popular 安装量列表筛选，读取 Evolution、OceanBlock 2、StoneBlock 4、Direwolf20 1.20 的 8 份版本 manifest。其他游戏版本排除，尤其 Direwolf20 1.19 实际为 1.19.2，不能算作 1.19.1。
- CurseForge 读取 All the Mods 10 8.4、All the Mods 9 1.1.1、Better MC BMC4 v62hf 原始 ZIP 的 `manifest.json`。manifest 只给 projectID/fileID，须从官方文件页或该次发行更新说明解析依赖版本；ATM10 的 JEI fileID 9353559 页面未取得，保留固定 ID，不推测版本。
- 共保存 31 份快照，28 份属于可检查的目标组合，3 份排除。不是全平台穷举或统一排名；不同平台的下载量、安装量口径不同。国内私有包无法由公开平台样本代表。
- [机器可读清单](modpack-samples.json) 保存来源、固定 release/file ID、提取后 manifest SHA256、版本、排除原因及失败项；原始响应和提取脚本保存在忽略目录 `.local/modpack-research-20261030/`。HTTP Range 只提取 ZIP 中清单，不执行包内脚本。

## 依赖决策

| 目标 | 加载器最低版本 | GeckoLib 必需 | JEI 可选 | Curios 可选 |
| --- | --- | --- | --- | --- |
| 1.19.1 Forge | 42.0.9（不变） | `[3.1.30,4)`（不变） | `[11.2.0.244,12)`（不变） | `[1.19.2-5.1.6.4,1.19.3)`（不变） |
| 1.20.1 Forge | 47.1.0（不变；构建 SDK 47.4.28） | `[4.2.3,5)`，原下限 4.4.9 | `[15.2.0.27,16)`（不变） | `[5.3.5,6)`，原下限 5.14.1 |
| 1.21.1 NeoForge | 21.1.72，原下限 21.1.249 | `[4.6.6,5)`，原下限 4.9.2 | `[19.21.0.247,20)`，原下限 19.56.0.438 | `[9.2.2,10)`，原下限 9.5.1 |

编译版本随运行下限降低，避免只放宽 TOML 却继续调用新 API。保留同主版本上限、精确 Minecraft 与加载器，不使用任意版本通配或打包第二份 GeckoLib。JEI、Curios 不存在时保持可选；GeckoLib 不存在时仍须由用户补装。

1.20.1 的早期 Better MC 使用 GeckoLib 4.2.4、Curios 5.4.2，已低于旧下限。FTB Direwolf20 1.20 1.0.0 提供 GeckoLib Forge 4.2.3、Curios Forge 5.3.5 的真实候选；其加载器为早期 NeoForge 47.1.76，整包排除在本项目 Forge 声明之外，仅将这两个正式 Forge 库放到独立 Forge 环境验证。

1.21.1 的早期 OceanBlock 2 使用官方 Curios 9.2.2、JEI 19.21.0.247；早期 Evolution 使用 GeckoLib 4.6.6、NeoForge 21.1.72。Evolution 同一清单列出两个声明 `curios` 的分支 JAR（9.0.14 和 Adorned 9.1.4），检查真实 JAR 元数据确认身份后排除该整包组合，不把它们当成官方 Curios 下限。

1.19.1 仅取得两份精确 Forge 快照，未取得足够的旧 GeckoLib/Curios 使用证据，因此保留现有基线。Curios 的 Maven 版本前缀虽然含 `1.19.2`，不能据此前缀修改 Minecraft 目标或替换成未经支持的宿主。

## 实际清单

“—”表示未发现该前置；ATM10 JEI 版本未解析，见上文。门槛通过只表示已解析的依赖数值在 PinkkiCore 声明范围内。

| 整合包 | 固定包版本 | 游戏 / 加载器 | GeckoLib | Curios | JEI | 处理 |
| --- | --- | --- | --- | --- | --- | --- |
| [Distant Horizons & Iris Shaders 0.3.0+1.21.1.neoforge](https://modrinth.com/modpack/yiYYuulE/version/VlqeLjfG) | 0.3.0+1.21.1.neoforge | 1.21.1 neoforge 21.1.216 | — | — | — | 版本门槛通过 |
| [Distant Horizons & Iris Shader NeoForge](https://modrinth.com/modpack/yiYYuulE/version/yJMEAttb) | 0.1.0-OUTDATED+1.21.1.neoforge | 1.21.1 neoforge 21.1.74 | — | — | — | 版本门槛通过 |
| [命运齿轮FOM 航空学](https://modrinth.com/modpack/fom4Ovsz/version/nRZoft5B) | 2.49 | 1.21.1 neoforge 21.1.248 | — | 9.5.1+1.21.1 | 19.43.0.392 | 版本门槛通过 |
| [命运齿轮FOM](https://modrinth.com/modpack/fom4Ovsz/version/ZY9AP5zN) | 1.0.4 | 1.21.1 neoforge 21.1.139 | — | 9.4.0+1.21.1 | 19.21.0.247 | 版本门槛通过 |
| [the horrors in the fog](https://modrinth.com/modpack/uPkIzDYf/version/yR0Djyc9) | 1.0.4 | 1.20.1 forge 47.2.17 | 4.4.2 | — | 15.3.0.4 | 版本门槛通过 |
| [the horrors in the fog](https://modrinth.com/modpack/uPkIzDYf/version/5EZPwiAq) | 1.0.0 | 1.20.1 forge 47.2.17 | 4.3.1 | — | 15.2.0.27 | 版本门槛通过 |
| [Optimized fps 1.21.1](https://modrinth.com/modpack/aOOJ2mte/version/8xoAevJv) | 3.1.23 | 1.21.1 neoforge 21.1.230 | — | — | — | 版本门槛通过 |
| [Optimized fps 1.21.1](https://modrinth.com/modpack/aOOJ2mte/version/cElSvGL2) | 3.1.0 | 1.21.1 neoforge 21.1.72 | — | — | — | 版本门槛通过 |
| [Better MC [FORGE] 1.20.1](https://modrinth.com/modpack/4BV47HRn/version/HKWduR7O) | v1 | 1.20.1 forge 47.2.0 | 4.2.4 | 5.4.2+1.20.1 | 15.2.0.27 | 版本门槛通过 |
| [Optimized fps 1.20.1](https://modrinth.com/modpack/aOOJ2mte/version/bf2aWUzR) | 2.4.10 | 1.20.1 forge 47.4.3 | — | — | — | 版本门槛通过 |
| [1.20.1](https://modrinth.com/modpack/aOOJ2mte/version/TLCtPeM4) | 2.4.1 | 1.20.1 forge 47.2.0 | — | — | — | 版本门槛通过 |
| [Pixelmon Mod](https://modrinth.com/modpack/vwgtbO0y/version/VjT88DbG) | 9.3.9 | 1.21.1 neoforge 21.1.172 | — | — | 19.21.2.313 | 版本门槛通过 |
| [Zombie Invade 100 Days](https://modrinth.com/modpack/l9m9tuPN/version/IyLHiIBF) | 2.0 | 1.20.1 forge 47.3.12 | 4.4.9 | 5.11.0+1.20.1 | — | 版本门槛通过 |
| [BattleArmory TACZ](https://modrinth.com/modpack/rQiXwLhB/version/xKY728HR) | 1.6.4-hotfix.2 | 1.20.1 forge 47.4.20 | — | — | 15.20.0.130 | 版本门槛通过 |
| [枪战](https://modrinth.com/modpack/rQiXwLhB/version/GsR4XI0n) | 1.0.0 | 1.20.1 forge 47.3.5 | — | 5.12.1+1.20.1 | 15.20.0.106 | 版本门槛通过 |
| [UO+ 1.19.1 Forge](https://modrinth.com/modpack/JEa64spL/version/tXUvCYTq) | 2.2.5 | 1.19.1 forge 42.0.9 | — | — | — | 版本门槛通过 |
| [Cobblemon Official Modpack [NeoForge]](https://modrinth.com/modpack/mHMxXbIu/version/fF4SbfGc) | 1.8.4 | 1.21.1 neoforge 21.1.264 | — | — | 19.70.0.460 | 版本门槛通过 |
| [Cobblemon Official [NeoForge]](https://modrinth.com/modpack/mHMxXbIu/version/jQdH6yBZ) | 1.6 | 1.21.1 neoforge 21.1.90 | — | — | — | 版本门槛通过 |
| [Fresh and smooth 1.21.1 NeoForge](https://modrinth.com/modpack/ICQPO4fX/version/VaiuEdQN) | 1.5.0 | 1.21.1 neoforge 21.1.169 | 4.7.5.1 | — | — | 版本门槛通过 |
| [Essentials](https://modrinth.com/modpack/PVbuz0gb/version/tWsGhCpO) | 1.0.0 | 1.19.1 forge 42.0.9 | — | — | 11.2.0.244 | 版本门槛通过 |
| [FTB Evolution](https://api.feed-the-beast.com/v1/modpacks/public/modpack/125/100560) | 1.45.1 | 1.21.1 neoforge 21.1.250 | 4.9.3 | 9.5.1+1.21.1 | 19.57.0.450 | 版本门槛通过 |
| [FTB Evolution](https://api.feed-the-beast.com/v1/modpacks/public/modpack/125/12575) | 1.0.0 | 1.21.1 neoforge 21.1.72 | 4.6.6 | — | 19.21.0.247 | 排除，见清单原因 |
| [FTB OceanBlock 2](https://api.feed-the-beast.com/v1/modpacks/public/modpack/128/100482) | 1.21.1 | 1.21.1 neoforge 21.1.248 | 4.9.2 | 9.5.1+1.21.1 | 19.39.0.369 | 版本门槛通过 |
| [FTB OceanBlock 2](https://api.feed-the-beast.com/v1/modpacks/public/modpack/128/100028) | 1.0.0 | 1.21.1 neoforge 21.1.115 | 4.7.3 | 9.2.2+1.21.1 | 19.21.0.247 | 版本门槛通过 |
| [FTB Presents Direwolf20 1.20](https://api.feed-the-beast.com/v1/modpacks/public/modpack/119/100051) | 1.16.1 | 1.20.1 neoforge 47.1.84 | 4.4.6 | 5.9.1+1.20.1 | 15.3.0.8 | 排除，见清单原因 |
| [FTB Presents Direwolf20 1.20](https://api.feed-the-beast.com/v1/modpacks/public/modpack/119/6935) | 1.0.0 | 1.20.1 neoforge 47.1.76 | 4.2.3 | 5.3.5+1.20.1 | 15.2.0.27 | 排除，见清单原因 |
| [FTB StoneBlock 4](https://api.feed-the-beast.com/v1/modpacks/public/modpack/130/100574) | 1.24.1 | 1.21.1 neoforge 21.1.262 | 4.9.3 | 9.5.1+1.21.1 | 19.69.0.456 | 版本门槛通过 |
| [FTB StoneBlock 4](https://api.feed-the-beast.com/v1/modpacks/public/modpack/130/100140) | 1.0.0 | 1.21.1 neoforge 21.1.209 | 4.8.2 | 9.5.1+1.21.1 | 19.25.0.323 | 版本门槛通过 |
| [All the Mods 10](https://www.curseforge.com/minecraft/modpacks/all-the-mods-10/files/9366926) | 8.4 | 1.21.1 neoforge 21.1.265 | 4.9.3 | 9.5.1+1.21.1 | — | 版本门槛通过 |
| [All the Mods 9](https://www.curseforge.com/minecraft/modpacks/all-the-mods-9/files/7097953) | 1.1.1 | 1.20.1 forge 47.4.0 | 4.8.2 | 5.14.1+1.20.1 | 15.20.0.116 | 版本门槛通过 |
| [Better MC BMC4](https://www.curseforge.com/minecraft/modpacks/better-mc-forge-bmc4/files/9104992) | v62hf | 1.20.1 forge 47.4.20 | 4.8.4 | 5.14.1+1.20.1 | 15.21.0.138 | 版本门槛通过 |

## 复查与证据边界

```bash
python3 scripts/check-dependency-baselines.py --check
python3 -m unittest discover -s scripts/tests -p test_dependency_baselines.py
```

检查器离线读取固定快照并与当前 `gradle.properties` 比较；缺少可选库不报冲突，缺少必需 GeckoLib 单独提示。它不会解析任意 Maven 预发行版本，也不把数字范围通过冒充整包运行成功。新增清单时必须先确认正式前置身份、精确 MC/加载器和版本来源。以后提高下限时运行本检查，并记录被排除的包版本及原因。

实际构建与无头日志位于 `.local/modpack-research-20261030/`：三个目标构建；1.20.1 和 1.21.1 发行校验；1.20.1 的低版本 Curios 饰品行为；1.21.1 的注册、机器、药水、战斗、实体、矿物与 Curios 核心探针；JEI 静态符号链接。具体最终结果与限制见[变更记录](../changes/2026-10-30-modpack-dependency-baselines.md)。

1.19.1 完整发行校验仍缺 `forge/entity/WildlifeSpawns.class`，属于此前主版本野外生成改动的待移植项；本次未补齐该玩法，不推进完整适配检查点。无 GUI 或完整整合包测试，不推断模型渲染、JEI 界面、渲染优化 mod 和大型整合包交互均已兼容。

### 旧加载器停服限制

NeoForge 21.1.72 加载低版 GeckoLib/Curios（含 JEI 与不含 JEI 两组）均完成核心功能探针和世界保存，但 JVM 未自动退出，300 秒超时后测试脚本清理进程。仅加载 GeckoLib/Curios、不加载 PinkkiCore 的宿主对照也出现相同驻留；线程快照保存在 `neo72-shutdown-threads.txt`。因此记录为“加载与行为通过，正常退出未通过”，不推进完整适配检查点，不将超时标记成功。

相同低版 GeckoLib 4.6.6、Curios 9.2.2、JEI 19.21.0.247 配合 NeoForge 21.1.249，完整探针、无 ERROR、保存、退出码 0 全部通过，见 `runtime-neo249-low-libs.log`。NeoForge 的声明下限 21.1.72 仅表示 API/加载门槛，服务器有此停服问题时使用较新 21.1.x；并不要求为 PinkkiCore 升级 GeckoLib/JEI/Curios。
