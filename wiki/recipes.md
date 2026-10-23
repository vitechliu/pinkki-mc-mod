<script setup>
import guabaRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba.json'
import dirtToMud from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba_conversion/dirt_to_mud.json'

const items = {
  'minecraft:melon_slice': { name: '西瓜片', image: '/images/minecraft/melon_slice.png' },
  'minecraft:mud': { name: '泥巴', image: '/images/minecraft/mud.svg' },
  'minecraft:dirt': { name: '泥土', image: '/images/minecraft/dirt.svg' },
  'pinkkicore:guaba': { name: '瓜巴', image: '/images/guaba.png', link: '/items#guaba' }
}
</script>

# 合成与加工

## 瓜巴 {#guaba}

1.20.1 Forge · 0.1.0 开发版（未发布）

<CraftingRecipe :recipe="guabaRecipe" :items="items" />

查看[瓜巴的用途](./items.md#guaba)。

## 瓜巴工作台加工 {#guaba-conversion}

1.20.1 Forge · 0.1.0 开发版（未发布）

<GuabaConversion :recipe="dirtToMud" :items="items" />

左上角放瓜巴，左侧放原料，右侧取产物。当前“泥土 → 泥巴”为占位配方；整合包可修改配方。操作与自动化见[瓜巴工作台](./guaba-workbench.md)。

## 瓜巴药水酿造 {#guaba-brewing}

<GuabaBrewing />

酿造台需要烈焰粉供能，底部可同时放入三瓶相同药水。持续时间、等级及喷溅/滞留见[瓜巴药水](./guaba-potions.md)。
