<script setup>
import guabaRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guaba.json'

const items = {
  'minecraft:melon_slice': { name: '西瓜片', image: '/images/minecraft/melon_slice.png' },
  'minecraft:mud': { name: '泥巴', image: '/images/minecraft/mud.svg' },
  'pinkkicore:guaba': { name: '瓜巴', image: '/images/guaba.png', link: '/items#guaba' }
}
</script>

# 合成与加工

## 瓜巴 {#guaba}

1.20.1 Forge · 0.1.0 开发版（未发布）

<CraftingRecipe :recipe="guabaRecipe" :items="items" />

查看[瓜巴的用途](./items.md#guaba)。
