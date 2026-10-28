<script setup>
import guabaHood from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_hood.json'
import pinkBlessing from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_blessing.json'
import pinkBlockFromIngots from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_block_from_pink_ingot.json'
import blastDeepslatePinkOre from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_blasting_deepslate_pink_ore.json'
import blastPinkOre from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_blasting_pink_ore.json'
import blastRawPink from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_blasting_raw_pink.json'
import pinkIngotsFromBlock from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_pink_block.json'
import pinkIngotFromNuggets from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_pink_nugget.json'
import smeltDeepslatePinkOre from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_smelting_deepslate_pink_ore.json'
import smeltPinkOre from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_smelting_pink_ore.json'
import smeltRawPink from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_ingot_from_smelting_raw_pink.json'
import pinkNuggetsFromIngot from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/pink_nugget_from_pink_ingot.json'
import rawPinkBlockFromRaw from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/raw_pink_block_from_raw_pink.json'
import rawPinkFromBlock from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/raw_pink_from_raw_pink_block.json'
import chargeLightningBottle from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_charging/compat/irons_spellbooks/glass_bottle_to_lightning_bottle.json'
import chargeLightningRune from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_charging/compat/irons_spellbooks/blank_rune_to_lightning_rune.json'
import chargingStation from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_charging_station.json'
import chargeGuaba from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_charging/guaba_to_thunder_guaba.json'
import chargeCreeper from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_charging/creeper_to_charged_creeper.json'
import bowConversion from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/bow_to_guaba_bow.json'
import swordConversion from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/diamond_sword_to_guaba_sword.json'
import guideRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guide_book.json'
import guabaRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba.json'
import dirtToMud from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/dirt_to_mud.json'

const items = {
  'pinkkicore:guaba_hood': { name: '瓜巴头套', image: '/images/guaba_hood.png', link: '/items#guaba-hood' },
  'pinkkicore:pink_blessing': { name: '粉红之祝', image: '/images/pink_blessing.png', link: '/items#pink-blessing' },
  'pinkkicore:pink_ore': { name: '粉红矿石', image: '/images/pink_ore.png' },
  'pinkkicore:deepslate_pink_ore': { name: '深层粉红矿石', image: '/images/deepslate_pink_ore.png' },
  'pinkkicore:raw_pink': { name: '粗粉红锭', image: '/images/raw_pink.png' },
  'pinkkicore:pink_ingot': { name: '粉红锭', image: '/images/pink_ingot.png' },
  'pinkkicore:pink_nugget': { name: '粉红粒', image: '/images/pink_nugget.png' },
  'pinkkicore:pink_block': { name: '粉红块', image: '/images/pink_block.png' },
  'pinkkicore:raw_pink_block': { name: '粗粉红块', image: '/images/raw_pink_block.png' },
  'minecraft:glass_bottle': { name: '玻璃瓶', image: '/images/minecraft/glass_bottle.png' },
  'irons_spellbooks:blank_rune': { name: '空白符文', image: 'https://raw.githubusercontent.com/iron431/irons-spells-n-spellbooks/b928995887aa362467fa999a9c9fb86419f00f2d/src/main/resources/assets/irons_spellbooks/textures/item/blank_rune.png' },
  'irons_spellbooks:lightning_rune': { name: '雷霆符文', image: 'https://raw.githubusercontent.com/iron431/irons-spells-n-spellbooks/b928995887aa362467fa999a9c9fb86419f00f2d/src/main/resources/assets/irons_spellbooks/textures/item/lightning_rune.png' },
  'irons_spellbooks:lightning_bottle': { name: '雷电瓶', image: 'https://raw.githubusercontent.com/iron431/irons-spells-n-spellbooks/b928995887aa362467fa999a9c9fb86419f00f2d/src/main/resources/assets/irons_spellbooks/textures/item/lightning_bottle.png', sprite: true },

  'minecraft:lightning_rod': { name: '避雷针', image: '/images/minecraft/lightning_rod.svg' },
  'minecraft:enchanting_table': { name: '附魔台', image: '/images/minecraft/enchanting_table.svg' },
  'minecraft:creeper_spawn_egg': { name: '苦力怕刷怪蛋', image: '/images/minecraft/creeper_spawn_egg.png' },
  'pinkkicore:charged_creeper_spawn_egg': { name: '闪电苦力怕刷怪蛋', image: '/images/charged_creeper_spawn_egg.png' },
  'pinkkicore:thunder_guaba': { name: '雷霆瓜巴', image: '/images/thunder_guaba.png' },
  'pinkkicore:guaba_charging_station': { name: '瓜巴充电桩', image: '/images/guaba_charging_station.png' },
  'minecraft:bow': { name: '弓', image: '/images/minecraft/bow.png' },
  'pinkkicore:guaba_bow': { name: '瓜巴弓', image: '/images/guaba_bow.png', link: '/items#guaba-bow' },
  'minecraft:diamond_sword': { name: '钻石剑', image: '/images/minecraft/diamond_sword.png' },
  'pinkkicore:guaba_sword': { name: '瓜巴剑', image: '/images/guaba_sword.png', link: '/items#guaba-sword' },
  'minecraft:book': { name: '书', image: '/images/minecraft/book.png' },
  'pinkkicore:guide_book': { name: '瓜巴手册', image: '/images/guide_book.png', link: '/handbook' },
  'minecraft:melon_slice': { name: '西瓜片', image: '/images/minecraft/melon_slice.png' },
  'minecraft:mud': { name: '泥巴', image: '/images/minecraft/mud.svg' },
  'minecraft:dirt': { name: '泥土', image: '/images/minecraft/dirt.svg' },
  'pinkkicore:guaba': { name: '瓜巴', image: '/images/guaba.png', link: '/items#guaba' }
}
</script>

# 合成与加工

## 瓜巴 {#guaba}

<CraftingRecipe :recipe="guabaRecipe" :items="items" />

查看[瓜巴的用途](./items.md#guaba)。

## 瓜巴工作台加工 {#guaba-conversion}

<GuabaConversion :recipe="dirtToMud" :items="items" />

左上角放瓜巴，左侧放原料，右侧取产物。操作与自动化见[瓜巴工作台](./guaba-workbench.md)。

## 瓜巴药水酿造 {#guaba-brewing}

<GuabaBrewing />

酿造台需要烈焰粉供能，底部可同时放入三瓶相同药水。持续时间、等级及喷溅/滞留见[瓜巴药水](./guaba-potions.md)。

## 瓜巴手册 {#guide-book}

<CraftingRecipe :recipe="guideRecipe" :items="items" />

右键阅读[瓜巴手册](./handbook.md)。

## 瓜巴剑 {#guaba-sword}

<GuabaConversion :recipe="swordConversion" :items="items" />

保留输入钻石剑的附魔、名称和已消耗耐久。查看[瓜巴剑](./items.md#guaba-sword)。

## 瓜巴弓 {#guaba-bow}

<GuabaConversion :recipe="bowConversion" :items="items" />

保留输入弓的附魔、名称和已消耗耐久。查看[瓜巴弓](./items.md#guaba-bow)。

## 瓜巴充电桩 {#guaba-charging-station}

<CraftingRecipe :recipe="chargingStation" :items="items" />

## 充电桩加工 {#guaba-charging}

<GuabaCharging :recipe="chargeGuaba" :items="items" />

<GuabaCharging :recipe="chargeCreeper" :items="items" />

查看[充电桩操作与自动化](./items.md#guaba-charging-station)。

## 铁魔法充能联动 {#irons-charging}

同时安装 Iron’s Spells ’n Spellbooks 时，瓜巴充电桩增加以下配方，每次 1 个原料转换为 1 个产物，耗时约 1 秒。

<GuabaCharging :recipe="chargeLightningBottle" :items="items" />

<GuabaCharging :recipe="chargeLightningRune" :items="items" />

不需要额外瓜巴或能量，也不改变铁魔法原有获取途径；支持漏斗、物品管道和 JEI 配方查询。

## 粉红矿熔炼 {#pink-smelting}

粗粉红锭、粉红矿石和深层粉红矿石均可炼成粉红锭；熔炉耗时 10 秒，高炉耗时 5 秒，每份产出 1 锭、1 点经验，需要燃料。

<SmeltingRecipe :recipe="blastDeepslatePinkOre" :items="items" />

<SmeltingRecipe :recipe="blastPinkOre" :items="items" />

<SmeltingRecipe :recipe="blastRawPink" :items="items" />

<SmeltingRecipe :recipe="smeltDeepslatePinkOre" :items="items" />

<SmeltingRecipe :recipe="smeltPinkOre" :items="items" />

<SmeltingRecipe :recipe="smeltRawPink" :items="items" />

## 粉红材料压缩与拆分 {#pink-storage}

<CraftingRecipe :recipe="pinkIngotFromNuggets" :items="items" />

<CraftingRecipe :recipe="pinkNuggetsFromIngot" :items="items" />

<CraftingRecipe :recipe="pinkBlockFromIngots" :items="items" />

<CraftingRecipe :recipe="pinkIngotsFromBlock" :items="items" />

<CraftingRecipe :recipe="rawPinkBlockFromRaw" :items="items" />

<CraftingRecipe :recipe="rawPinkFromBlock" :items="items" />

## 瓜巴头套 {#guaba-hood}

<CraftingRecipe :recipe="guabaHood" :items="items" />

## 粉红之祝 {#pink-blessing}

<CraftingRecipe :recipe="pinkBlessing" :items="items" />
