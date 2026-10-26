<script setup>
import bowConversion from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/bow_to_guaba_bow.json'
import swordConversion from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/diamond_sword_to_guaba_sword.json'
import guideRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guide_book.json'
import guabaRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba.json'
import dirtToMud from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/dirt_to_mud.json'

const items = {
  'minecraft:bow': { name: '弓', image: '/images/minecraft/bow.png' },
  'pinkkicore:guaba_bow': { name: '瓜巴弓', image: '/images/guaba_bow.png', link: '/items#guaba-bow' },
  'minecraft:diamond_sword': { name: '钻石剑', image: '/images/minecraft/diamond_sword.png' },
  'pinkkicore:guaba_sword': { name: '瓜巴剑', image: '/images/guaba_sword.png', link: '/items#guaba-sword' },
  'minecraft:book': { name: '书', image: '/images/minecraft/book.png' },
  'pinkkicore:guide_book': { name: '引导手册', image: '/images/minecraft/book.png', link: '/handbook' },
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

## 引导手册 {#guide-book}

<CraftingRecipe :recipe="guideRecipe" :items="items" />

右键阅读[引导手册](./handbook.md)。

## 瓜巴剑 {#guaba-sword}

<GuabaConversion :recipe="swordConversion" :items="items" />

保留输入钻石剑的附魔、名称和已消耗耐久。查看[瓜巴剑](./items.md#guaba-sword)。

## 瓜巴弓 {#guaba-bow}

<GuabaConversion :recipe="bowConversion" :items="items" />

保留输入弓的附魔、名称和已消耗耐久。查看[瓜巴弓](./items.md#guaba-bow)。
