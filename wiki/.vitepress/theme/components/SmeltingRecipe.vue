<script setup lang="ts">
import { withBase } from 'vitepress'

type ItemVisual = { name: string; image: string }
type Recipe = { type: string; ingredient: { item: string }; result: string; cookingtime: number; experience: number }
const props = defineProps<{ recipe: Recipe; items: Record<string, ItemVisual> }>()
if (!['minecraft:smelting', 'minecraft:blasting'].includes(props.recipe.type)) throw new Error('不支持的熔炼类型')
const input = props.items[props.recipe.ingredient.item]
const output = props.items[props.recipe.result]
if (!input || !output) throw new Error('熔炼配方缺少物品图标')
const device = props.recipe.type === 'minecraft:blasting' ? '高炉' : '熔炉'
</script>

<template>
  <figure class="smelting-recipe" :aria-label="`${device}：${input.name} → ${output.name} × 1`">
    <div class="recipe-title">{{ device }}</div>
    <div class="recipe-panel">
      <div class="slot"><img :src="withBase(input.image)" :alt="input.name" :title="input.name" width="40" height="40" /></div>
      <div class="heat" aria-label="需要燃料"><span>♨</span><span>→</span></div>
      <div class="slot"><img :src="withBase(output.image)" :alt="output.name" :title="output.name" width="40" height="40" /><b>1</b></div>
    </div>
    <figcaption>{{ input.name }} → {{ output.name }} × 1 · {{ recipe.cookingtime / 20 }} 秒 · {{ recipe.experience }} 经验</figcaption>
  </figure>
</template>

<style scoped>
.smelting-recipe { max-width: 420px; margin: 24px 0; }
.recipe-title { margin-bottom: 10px; font-weight: 600; }
.recipe-panel { display: flex; align-items: center; justify-content: center; gap: 30px; padding: 24px 16px; border: 1px solid var(--vp-c-divider); border-radius: 8px; background: var(--vp-c-bg-soft); }
.slot { position: relative; display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; background: #8b8b8b; border: 2px solid; border-color: #373737 #fff #fff #373737; }
img { image-rendering: pixelated; object-fit: contain; }
b { position: absolute; right: 3px; bottom: 0; color: white; text-shadow: 2px 2px #333; }
.heat { display: flex; gap: 10px; color: #ad6947; font-size: 28px; }
figcaption { margin-top: 10px; color: var(--vp-c-text-2); text-align: center; font-size: 14px; }
</style>
