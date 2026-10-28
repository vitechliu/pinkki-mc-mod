# 饰品槽位与 Curios 接入约定

适用目标：Minecraft 1.20.1 / Forge。核对日期：2026-10-28；源码参考 Curios `5.14.1+1.20.1`，提交 `a91da6ba2396f6ed9042c8a47a471ca5efc97da8`。

1.20.1 已接入可选 Curios 5.14.1+1.20.1：瓜巴头套使用 `head`，粉红之祝使用 `charm`，各以 `SET` 分配 1 格给玩家；保留整合包扩槽和标签修改。未安装 Curios 时两件物品仍可合成，但不能佩戴生效。下文 `pinkki` 是已确定的槽位 ID，“粉饰”为暂定显示名称。数量、具体饰品和效果待实际功能设计时确定；不据此声明次要版本支持。

## 官方预设槽位

官方建议：适合已有分类的饰品优先复用预设，减少重复槽位，让不同 Mod 的饰品共用装备位置。以下为官方完整预设列表，中文为本地源码中的翻译；用途以官方说明为准。

| 槽位 ID | Curios 中文显示 | 官方用途与典型物品 | 默认物品标签 |
| --- | --- | --- | --- |
| `head` | 首饰 | 头部佩戴物：皇冠、帽子等；本项目可理解为头饰 | `curios:head` |
| `necklace` | 项链 | 颈部佩戴物：项链、坠饰 | `curios:necklace` |
| `back` | 背饰 | 背部佩戴物：披风、背包 | `curios:back` |
| `body` | 胸饰 | 躯干佩戴物：斗篷、衬衣等 | `curios:body` |
| `bracelet` | 手镯 | 手腕佩戴物：腕带、手镯 | `curios:bracelet` |
| `hands` | 手饰 | 手部佩戴物：手套、护手 | `curios:hands` |
| `ring` | 戒指 | 手指佩戴物：戒指 | `curios:ring` |
| `belt` | 腰带 | 腰部佩戴物：腰带、腰包 | `curios:belt` |
| `charm` | 护符 | 不对应特定身体部位的杂项饰物 | `curios:charm` |
| `curio` | 饰品 | 通用槽位；存在跨槽位接受规则，使用前确认平衡设计 | `curios:curio` |

出处：[官方预设说明](https://docs.illusivesoulworks.com/1.20.x/curios/slots/preset-slots)。预设已定义并不等于自动出现在玩家身上，仍需分配给玩家。

`curio` 的特殊性：默认验证规则下，通用槽接受其他类别的饰品，而带 `curios:curio` 标签的物品也能进入其他类别槽。不要给所有 PinkkiCore 饰品添加这个标签作为兼容补丁。

## 槽位 ID、标签与显示名称

- 槽位 ID 是兼容契约。`hands`、`glove`、`gloves` 是不同槽位，不会因含义相近或汉化相同而自动合并。普通手套使用 `hands`，不另造同义槽位。
- 显示名称只负责界面翻译，例如 `curios.identifier.hands`；修改翻译不改变可装备范围。
- 默认验证器 `curios:tag` 使用 `curios:<槽位ID>` 物品标签决定可放入范围。通过标签后，物品的 `canEquip` 和其他 Mod 的事件仍可能限制装备。
- 1.20.1 的槽位定义按 ID 合并：不同 Mod 命名空间下的 `ring.json` 会共同影响 `ring`。资源目录的 Mod 命名空间不自动隔离槽位 ID。
- 保留整合包通过数据包调整标签与槽位的能力。标签通常使用 `replace: false`；不要额外写死无必要的同义槽位检查。

## PinkkiCore 专属槽位：粉饰

| 项目 | 约定 |
| --- | --- |
| 槽位 ID | `pinkki`，后续保持稳定 |
| 暂定中文名称 | 粉饰 |
| 翻译键 | `curios.identifier.pinkki` |
| 默认物品标签 | `curios:pinkki` |
| 定义资源 | `data/pinkkicore/curios/slots/pinkki.json` |
| 玩家槽位分配资源 | `data/pinkkicore/curios/entities/player.json` |
| 可佩戴物品清单 | `data/curios/tags/items/pinkki.json` |
| 用途 | 本 Mod 需要独立装备位置的专属饰品；具体物品范围待设计 |
| 默认数量、图标、外观槽、叠加规则 | 待首个饰品功能确定，不提前锁定 |

普通戒指、项链、手套等仍优先使用公共预设，不把本 Mod 的全部饰品都归入 `pinkki`。确有独立装备位置需求的物品才归入粉饰。

“独立槽位”不自动意味着“只能放本 Mod 物品”或“粉饰只能放这里”：其他 Mod / 数据包可以加入 `curios:pinkki`，默认通用 `curio` 规则也可能允许跨槽装备。如果后续要求粉饰严格专用、不能绕过数量限制，需要同时设计槽位验证、物品 `canEquip` 和通用槽交互，验证后再承诺独占行为。

### 粉饰槽位后续实现清单

本次不创建尚无对应物品的 `pinkki` 槽。公共 `head` / `charm` 已实现；下列专属槽位清单按需执行。以下路径位于 `versions/1.20.1/src/main/resources/` 下；目前仅记录设计，不创建发行资源。

1. 将匹配版本的 Curios 加入构建依赖及 `mods.toml` 依赖声明，明确必需还是可选。若采用可选依赖，隔离直接引用 Curios 类型的类，验证未安装时仍可加载。
2. 创建 `data/pinkkicore/curios/slots/pinkki.json`，配置确定后的数量、图标等。默认数量优先使用 `SET`，不要无条件 `ADD` 导致多个 Mod 叠加扩槽。
3. 创建 `data/pinkkicore/curios/entities/player.json`，向 `minecraft:player` 分配 `pinkki`。使用公共预设的饰品也要分配相应槽位。
4. 创建 `data/curios/tags/items/pinkki.json`，使用 `replace: false`，只列入实际注册的饰品。不写入尚不存在的占位物品。
5. 在对应语言文件中添加 `curios.identifier.pinkki` 的中文“粉饰”及英文翻译；槽位 ID 不随显示名称调整而改变。
6. 自有饰品通常实现 `ICurioItem`。静态属性优先用属性接口及传入的槽位 UUID；持续效果明确服务端执行、重复佩戴叠加与卸下清理。角色上的模型显示单独通过客户端渲染接入。
7. 补充实际游戏功能的 `docs/changes/` 记录、游戏内手册和 Wiki；完成适用的 1.20.1 无头检查、发行 JAR 校验及本地部署。客户端操作遵守默认禁止实机测试规则。

同 ID 多份槽位定义在默认 `SET` 下取最大数量，`ADD` / `REMOVE` 是额外数量修饰。不要覆盖公共槽位的验证器或用 `replace: true` 清空其他 Mod 的物品来实现本 Mod 的限制。

## 后续验收重点

- 普通饰品共用预设槽位，不出现 `hands` / `glove` 等同义重复槽。
- 粉饰槽位、中文名称和物品提示一致；允许与拒绝的物品均符合设计。
- 明确粉饰放进通用槽、通用饰品放进粉饰槽是否允许，以及是否影响数量与叠加限制。
- 覆盖卸下、死亡与重生、退出重进、数据包调整槽位数量等场景，检查属性残留和物品保留。
- 专用服务端正常加载，客户端渲染类型不进入服务端加载路径。

## 当前实现

- `compat/curios/CuriosIntegration.java` 仅在 Curios 存在时加载，使用 `CuriosApi.registerCurio` 为普通物品注册行为，避免必需依赖。
- 瓜巴家族通过 `GuabaEffectFamily` 标记，`GuabaEffect` 及其子类（含雷霆瓜巴）自动纳入；未来独立实现的瓜巴类效果也必须实现此标记，不能仅依赖名称。
- 头套在 `MobEffectEvent.Applicable` 中拒绝瓜巴类效果，装备和服务端饰品 tick 清除已有瓜巴类效果。仅扫描有效装备槽，不把外观槽或背包当作佩戴。
- 粉红之祝通过传入的槽位 UUID 向护甲、最大生命值各添加 2 点；Curios 负责装备/卸下和瞬态属性，整合包扩槽后多件可叠加。两件物品均不提供角色身上的模型渲染。
- 本地测试实例可用 `python3 scripts/deploy-local.py --with-curios` 补齐 Curios；发行 JAR 不捆绑 Curios。未传参数时保持原部署行为。
- 无头验证与玩家内容同步见 [本次变更](changes/2026-10-28-curios-accessories.md)。

## 参考资料

- [官方预设槽位](https://docs.illusivesoulworks.com/1.20.x/curios/slots/preset-slots)
- [槽位注册与合并规则](https://docs.illusivesoulworks.com/1.20.x/curios/slots/slot-register)
- [物品标签与验证器](https://docs.illusivesoulworks.com/1.20.x/curios/items/assign-slots)
- [官方数据包示例](https://docs.illusivesoulworks.com/1.20.x/curios/slots/datapack-example)
- [本地参考源码索引](reference-mods.md)：源码位于 `.local/mod-sources/Curios/`；优先查看 `api/type/capability/ICurioItem.java`、`common/data/CuriosSlotManager.java` 与 `common/inventory/DynamicStackHandler.java`（相对于源码的 `src/main/java/top/theillusivec4/curios/`）。
