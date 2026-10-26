# 瓜巴材料兼容性调研与试作

初次调研：2026-10-22；命中效果实装更新：2026-10-25。仅针对 Minecraft 1.20.1 / Forge / Java 17，PinkkiCore 0.1.0 开发中，尚未发布。基础属性数值仍是兼容性占位；已接入“瓜巴之触”命中状态，不新增粒子、音效或另一个状态实现。

## 可行性与实现选择

三者都能通过随 PinkkiCore JAR 分发的 JSON 数据增加瓜巴材料，无需为了基础接入引入第三方 Java API、Mixin、反射或强制依赖。玩家只安装其中一个时启用对应材料；一个都不安装时瓜巴原有合成与属性保持可用。原材料 ID 始终为 `pinkkicore:guaba`，不把瓜巴加入木板、金属或矿物的通用物品标签。

| 模组 | 材料入口与可玩路径 | 本次试作范围 |
| --- | --- | --- |
| Silent Gear（寂静装备） | `data/pinkkicore/silentgear_materials/guaba.json`；原生材料系统识别瓜巴，按蓝图制作主部件和杆部件 | 主部件/杆属性、原料匹配、通用着色模型；主材和杆带 `pinkkicore:guaba_touch` 特性 |
| Tinkers' Construct（匠魂 3） | `data/pinkkicore/tinkering/materials/` 的 definition、stats、traits，加 `tconstruct:material` 道具映射配方 | 部件制造台材料、工具头/手柄/绑定结、维修材料映射；每个瓜巴等于 1 材料单位；各受支持部件带 `pinkkicore:guaba_touch` modifier；不新增熔融流体、熔炼/浇铸、盔甲或远程部件 |
| Tetra | `data/tetra/materials/wood/pinkkicore_guaba.json`；已有接受 `tetra:wood/` 路径的模块和加工方案自动派生瓜巴变体 | 采用木质类模块接纳组作为试作入口，独立显示类别为“瓜巴（试作）”；材料 key 为 `pinkkicore_guaba`，不覆盖橡木等原有材料；材料 effects 带 `pinkkicore_guaba_touch` |

Tetra 的“wood”在这里是模块接纳路径，**不是**把瓜巴变成原版木板，也不代表其最终材质定位。它的加载器限定 `tetra` 命名空间，因而这个兼容文件不能放在 `data/pinkkicore/materials/`。文件名和 key 加 `pinkkicore` 前缀防止冲突。这样无需拷贝、替换原版模块/加工方案；后续若决定不同的可用部件范围，可以改为独立材料组并增加对应模块和方案，但应保留旧部件 key 或设计迁移，避免已造装备失效。仅将文件移到 `misc/` 不足以让原有剑刃或剑柄方案接受它。

## 加载与缺失依赖

- Silent Gear 自己扫描 `silentgear_materials`；缺失时没有对应读取器，不执行第三方类。
- 匠魂自己扫描 `tinkering/materials`。材料 definition 增加 `forge:mod_loaded` 条件；普通配方目录下的道具映射配方同样增加该条件，避免缺失匠魂时解析不存在的 `tconstruct:material` 序列化器。
- Tetra 自己读取 `data/tetra/materials/`；缺失时这些专属数据不参与原版配方加载。
- 三种兼容不改变项目已有 GeckoLib 必需依赖。不内嵌三个模组或其前置。匠魂运行时仍需要 Mantle，这是匠魂的依赖，不用于实现 PinkkiCore 自研手册。
- 整合包/数据包可以覆盖这些稳定路径。后续属性变化影响已有材料装备时，是否立即重算由各模组的缓存和同步机制决定，需要另做存档升级与多人验证。

## 占位参数与后续修改点

| 项目 | 初始占位 | 修改位置 |
| --- | --- | --- |
| Silent Gear | tier 0、木采掘级、主材耐久 60、采掘速度 2、近战伤害加值 0；杆使用中性属性；护甲基础占位 5 | `silentgear_materials/guaba.json` 的 `availability`、`stats`、`harvest_tier` |
| 匠魂 | tier 1、工具头耐久 60、木采掘级、速度 2、攻击加值 0；手柄四项中性偏移 0；绑定结无数值 | `tinkering/materials/definition/guaba.json`、`stats/guaba.json` |
| Tetra | primary 3、secondary 1.7、tertiary 6、耐久基值 60、完整度消耗 1/提供 4、魔力容量 0、木采掘级、效率 2 | `data/tetra/materials/wood/pinkkicore_guaba.json` |
| 颜色 | `C87945` 棕橙色通用模型着色 | Silent Gear `model`、匠魂 render info 的 `color`、Tetra `tints` |
| 特性/特效 | 瓜巴之触：近战命中施加瓜巴 I，200 tick，重复命中刷新，不按部件数量升阶 | 下文三边原生数据定义 |

各系统的数值计算公式不同，不能把同一个“60”理解成三边最终工具耐久完全相同。匠魂本次核对的 3.11.2 手柄数据使用 **0 为中性偏移**，不是把耐久或速度乘以零。Tetra 模块会继续应用自己的完整度、攻击和耐久公式；模块自带的横扫等能力不等于瓜巴专属特效。

## 瓜巴之触的原生实现

复用已经注册的 `pinkkicore:guaba` MobEffect（`GuabaPotions.GUABA`），不复制速度/护甲规则，也不新增全局攻击监听器、第三方 Java 依赖或反射适配。效果会强化目标护甲，速度可能增减；这是既有瓜巴状态的本意，不改造成中毒伤害。

| 系统 | 数据与参考 | 触发规则及参数 |
| --- | --- | --- |
| Silent Gear 3.6.7 | `silentgear_traits/guaba_touch.json`；参考发行版原生 Venom 的 `silentgear:target_effect` | 主部件/杆均赋 1 级 trait，`max_level=1`；原生玩家近战工具攻击链施加；`duration=10.0` 的单位为秒，amplifier 0 |
| 匠魂 3.11.2.166 | `tinkering/modifiers/guaba_touch.json`；参考该发行包的 Spectral、Venom | `tconstruct:mob_effect` 仅挂 `tconstruct:melee_hit`，不挂反击/远程钩子；概率 1，`time.flat=200` tick、`level.flat=1`（以 1 为起点） |
| Tetra 6.17.0 | `data/tetra/item_effects/pinkkicore_guaba_touch.json`；参考原生 Infested 的 `tetra:apply_effect` | `tetra:apply_hit_effects` -> target，source=attacker，duration 200 tick、amplifier 0；不配置 stackDurationCap/stackAmplifierCap |

三边的材料 trait/effect 值可以合并，但输出状态等级和时间均固定。沿用原版 `MobEffectInstance` 合并规则：刷新到 200 tick，不累加秒数；更强、剩余更久的既有状态不被削弱。不新增受击反击模块或通用投射物监听器。Tetra 的原生命中链也可由其投掷、射击、横扫或格挡反射等机制调用，材料效果会沿原生链生效；本轮实际攻击探针只覆盖普通近战，不宣称这些扩展链均已实测。攻击命中时机沿用各自机制：Silent Gear 的原生 Venom 在 `LivingAttackEvent` 阶段施加（并非统一的最终扣血后回调），匠魂使用原生近战钩子，Tetra 使用原生命中回调；本轮不改写其他模组的免疫/取消伤害判定。

重要版本差异：当前上游源码分支已把匠魂 `weapon_mob_effect` 等拆分成新模块，但本轮固定发行 **3.11.2.166** 不认识该名字。实际测试发现并修正为此发行包的 `mob_effect`，用 hooks 明确限制为近战。今后升级匠魂时需重新验证此模块的兼容性，不能只检查新分支示例。


## 源码与发行基准

官方文档用于确定方向，实际字段另与下载的 1.20.1 发行 JAR 内置木材示例交叉核对，避免使用其他 Minecraft 版本的旧教程。发行文件从 Modrinth 项目 API 获取并逐个核对 API 提供的 SHA512，下载记录在 `.local/compat-research/downloads.json`。

| 模组 | 本轮发行包基准 | 前置 |
| --- | --- | --- |
| Silent Gear | 3.6.7 | Silent Lib 8.0.0 |
| Tinkers' Construct | 3.11.2.166 | Mantle 1.11.104 |
| Tetra | 6.17.0 | mutil 6.3.0 |

源码分支用于检查扫描、合并及派生逻辑，不将分支当前版本视为与发行包完全相同：

- [Silent Gear 官方项目说明](https://github.com/SilentChaos512/Silent-Gear)：支持以 JSON/data pack 增加材料。核对 `1.20.x` 分支提交 `8af569e03ad29e0f8aeeea6bef616bd29d1f4a97` 的 `MaterialManager`、`AbstractMaterial`、`MaterialSerializers` 和内置材料。
- [匠魂官方材料指南](https://slimeknights.github.io/docs/guides/material/)：材料定义/属性/特性、道具映射和客户端渲染是不同资源。核对 `1.20.1` 分支提交 `f4e77d43f39ba1047bf93ae816b7e1beaab514c2`，最终使用发行包 3.11.2.166 内置 JSON 核对字段。
- [Tetra 1.20 官方材料文档](https://tetra.mickelus.se/wiki/1.20/tech/materials)：材料路径、模块引用、属性与原料。核对 `1.20` 分支提交 `d212377491eaf2ef58ea841f0e45d532d5e69f95` 的 `DataManager`、`MaterialStore`、`MaterialVariantData`，以及 [mutil 资源读取器](https://github.com/mickelus/mutil/blob/1.20/src/main/java/se/mickelus/mutil/data/DataStore.java) 提交 `b3ebf73cefbc077082679a8598b23fcb91f6b69e` 的命名空间过滤；最终对照 Tetra 6.17.0 发行资源。

## 验证与证明范围

初次注册验证见 [旧记录](changes/2026-10-22-guaba-material-compat.md)，本轮命中验证见 [新记录](changes/2026-10-25-guaba-touch.md)。测试探针位于 `scripts/tests/compat_probe/GuabaCompatProbe.java`，只编译到独立测试 mod，不进入 PinkkiCore 发行包；它使用已固定发行版本的直接 API，无反射。专用服务端只在项目隔离目录运行，绑定回环地址，随后正常停服。

无头探针能够验证服务器端材料注册、原料匹配、匠魂部件属性和映射、Tetra 实际派生剑刃/剑柄变体及原有橡木变体保留。命中测试构造实际材料装备，由服务端 FakePlayer 调用正常 Player.attack，包含非瓜巴对照、各部位材料、固定 I/200 tick、刷新、更强状态保留，以及匠魂 3.11.2 未蓄满力的正常近战也会触发。它不代替客户端图标、工具纹理、悬停、蓝图/部件制造台/工作台操作、多人与存档升级测试，也不证明其他发行版本或完整整合包均兼容。

玩家 Wiki 同步说明瓜巴之触的使用及限制。PinkkiCore 自研游戏内手册尚未实现，后续须按 `guaba` 条目接入三种获取/使用路径并查询当前配方数据，不能把匠魂手册当作本项目手册完成。

### 可复现的无头检查

先使用官方 Forge 安装器在项目独立测试目录准备 **1.20.1 / 47.4.28** 服务端库；从 `scripts/tests/compat_probe/dependencies.json` 列出的官方发行链接下载六个模组及前置 JAR。检查脚本不会自行下载，会按清单核对 SHA512。准备 GeckoLib 4.4.9，并沿用项目测试服务端已经确认的 EULA；未确认时脚本直接停止。

```bash
./scripts/mod.sh 1.20.1 build
python3 scripts/verify-artifact.py 1.20.1
python3 scripts/check-guaba-compat.py \
  --libraries .local/package-test/1.20.1/libraries \
  --dependencies .local/compat-research \
  --geckolib .local/geckolib-research/geckolib-forge-1.20.1-4.4.9.jar \
  --java-home .local/jdk-17/Contents/Home
```

示例路径均位于项目忽略目录，按本机实际准备位置替换。默认依次执行 `none,silentgear,tconstruct,tetra,all`；可用 `--cases all` 只核对三者共存。脚本将每次测试写入新的 `.local/guaba-compat-tests/<运行标识>/`，使用全新世界，绑定 `127.0.0.1:25579`，编译隔离的测试探针 mod，读取实际材料注册表后发送 `stop`。探针失败、服务器错误、超时或未正常停服都不能算通过。不要将测试探针 JAR 放入真实实例，也不要把这些运行目录提交或分发。

无头脚本将已知的 Yggdrasil 后台公钥下载失败单独记为 `auth_key_unavailable`（不作为离线材料测试失败）；其他 ERROR 或探针失败仍判失败。这不证明正版认证联机可用。
