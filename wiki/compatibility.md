# 模组联动

<!-- wiki-only -->
各 Minecraft 版本支持的联动与安装要求见[项目版本与模组依赖](./guide.md#versions)。
<!-- /wiki-only -->

## JEI 配方预览 {#jei}

客户端安装 JEI 后，可在“瓜巴工作台”分类查看[加工配方](./guaba-workbench.md#游戏内配方预览)。还可在“雷击转换”分类查询[雷霆瓜巴](./items.md#thunder-guaba)的获取方式。在雷霆瓜巴上按 **R**，或在瓜巴上按 **U**。

JEI 为可选安装，不安装也能使用工作台和雷击转换。

## 瓜巴材料兼容 {#guaba}

<!-- guide-mod: silentgear,tconstruct,tetra -->
安装对应联动模组后，[瓜巴](./items.md#guaba)可作为装备材料。
<!-- /guide-mod -->

<!-- guide-mod: silentgear -->
- **寂静装备（Silent Gear）**：配合蓝图制作主部件和杆部件，再组合成工具。
<!-- /guide-mod -->

<!-- guide-mod: tconstruct -->
- **匠魂（Tinkers' Construct）**：在部件制造台制作工具头、手柄和绑定结，每个瓜巴提供 1 材料单位。瓜巴无需熔炼，适用于近战工具部件。
<!-- /guide-mod -->

<!-- guide-mod: tetra -->
- **Tetra**：在工作台制作接受木质材料的装备模块，用量与工具要求以工作台为准。
<!-- /guide-mod -->

<!-- guide-mod: silentgear,tconstruct,tetra -->
### 瓜巴之触 {#guaba-touch}

使用含瓜巴部件的装备近战命中目标，会施加[瓜巴 I](./guaba-potions.md)，持续 **10 秒**。重复命中会刷新时间，多个瓜巴部件不会提升等级，目标已有的更强效果继续生效。瓜巴会随机改变速度和护甲，因此也可能强化目标。
<!-- /guide-mod -->

## 雷霆瓜巴材料兼容 {#thunder-guaba}

<!-- guide-mod: silentgear,tconstruct,tetra -->
[雷霆瓜巴](./items.md#thunder-guaba)是二级装备材料，偏向快速攻击、挖掘和移动。成品属性随部件组合变化。
<!-- /guide-mod -->

<!-- guide-mod: silentgear -->
- **寂静装备**：可做主部件和杆。主部件基础耐久 1200、挖掘速度 10、近战伤害加成 4、攻速加成 0.4，采掘等级为下界合金。杆部件提高速度并降低耐久。
<!-- /guide-mod -->

<!-- guide-mod: tconstruct -->
- **匠魂**：可做近战工具头、手柄和绑定结，每个雷霆瓜巴提供 1 材料单位。工具头的基础耐久为 1200，挖掘速度 10，伤害加成 3.5，采掘等级为下界合金。手柄提高攻速和挖掘速度并降低耐久。
<!-- /guide-mod -->

<!-- guide-mod: tetra -->
- **Tetra**：用于接受木质材料的模块，加工需钻石级斧类工具。基础耐久 1200、挖掘效率 10，攻击强度接近下界合金，采掘等级为下界合金。
<!-- /guide-mod -->

<!-- guide-mod: silentgear,tconstruct,tetra -->
### 雷霆轻盈
<!-- /guide-mod -->

<!-- guide-mod: silentgear -->
- **寂静装备**：主手持用时提高 **5% 基础移速**，多个雷霆部件共用这一加成。
<!-- /guide-mod -->

<!-- guide-mod: tconstruct -->
- **匠魂**：提高 **15% 基础攻速**，主手持用还提高 **5% 基础移速**。多个雷霆部件共用这一加成，手柄的速度参数仍参与计算。
<!-- /guide-mod -->

<!-- guide-mod: tetra -->
- **Tetra**：每个雷霆模块提供 **0.2 攻速**和 **5% 总移速**加成。攻速在主手生效，也会影响挖掘效率。移速在主手或副手生效。
<!-- /guide-mod -->

<!-- guide-mod: silentgear,tconstruct,tetra -->
### 雷霆瓜巴之触

近战命中会施加[雷霆瓜巴 I](./guaba-potions.md#thunder-guaba)，持续 **10 秒**。重复命中会刷新效果，落雷倒计时继续。多个部件共用同一等级，目标已有的更强效果继续生效。它可以和普通瓜巴效果共存，落雷可能波及附近的攻击者、其他实体和地面。
<!-- /guide-mod -->

<!-- guide-version: 1.20.1 -->
<!-- guide-mod: silentgear,tconstruct,tetra -->
## 梦系材料 {#dream-materials}

在 1.20.1 Forge，[云梦](./items.md#cloud-dream)、[冥梦](./items.md#nether-dream)及两种瓜巴版的基础属性接近铁，瓜巴版略高并带有[瓜巴之触](#guaba-touch)。

- **轻盈**：主手持有或穿戴含云梦材料的装备时，移动速度提高 8%。多件装备共用这项加成。
- **下界共鸣**：在下界，含冥梦材料的主手武器提高 2 点攻击力，每件含该材料的护甲提高 2 点护甲。
<!-- /guide-mod -->

<!-- guide-mod: silentgear -->
- **寂静装备**：可制作主部件与杆。
<!-- /guide-mod -->

<!-- guide-mod: tconstruct -->
- **匠魂**：在部件制造台可做工具头、手柄、绑定结、锁链片和护甲板。
<!-- /guide-mod -->

<!-- guide-mod: tetra -->
- **Tetra**：可制作接受铁类材料的模块，按工作台提示操作。
<!-- /guide-mod -->
<!-- /guide-version -->

## 铁魔法 {#irons-spellbooks}

同时安装 Iron’s Spells ’n Spellbooks 后，可用[瓜巴充电桩](./recipes.md#irons-charging)把玻璃瓶加工为雷电瓶、空白符文加工为雷霆符文，每次 1→1、约 1 秒，无额外消耗。

铁魔法原有配方和获取方式继续保留；未安装时不加载这两条配方。
