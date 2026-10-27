<script setup lang="ts">
import { withBase } from 'vitepress'
type Visual = { name: string; image: string; sprite?: boolean }
const props = defineProps<{
  recipe: { type: string; ingredient: { item: string }; result: { item: string; count: number } }
  items: Record<string, Visual>
}>()
if (props.recipe.type !== 'pinkkicore:guaba_charging') throw new Error('不是充电桩配方')
for (const id of [props.recipe.ingredient.item, props.recipe.result.item]) if (!props.items[id]) throw new Error(`缺少图标：${id}`)
</script>
<template>
  <figure class="charging">
    <figcaption>瓜巴充电桩 · 1 秒</figcaption>
    <div class="charging-row">
      <div class="charging-slot"><img :src="withBase(items[recipe.ingredient.item].image)" :alt="items[recipe.ingredient.item].name" :style="items[recipe.ingredient.item].sprite ? { objectFit: 'cover', objectPosition: 'top' } : {}" /><span>1</span></div>
      <strong>⚡ →</strong>
      <div class="charging-slot"><img :src="withBase(items[recipe.result.item].image)" :alt="items[recipe.result.item].name" :style="items[recipe.result.item].sprite ? { objectFit: 'cover', objectPosition: 'top' } : {}" /><span>{{ recipe.result.count }}</span></div>
    </div>
    <figcaption>{{ items[recipe.ingredient.item].name }} → {{ items[recipe.result.item].name }}</figcaption>
  </figure>
</template>
<style scoped>
.charging{max-width:420px;margin:24px 0;padding:20px;border:1px solid var(--vp-c-divider);border-radius:8px;background:var(--vp-c-bg-soft)}
.charging-row{display:flex;gap:28px;align-items:center;justify-content:center;margin:14px}
.charging-slot{position:relative;display:flex;align-items:center;justify-content:center;width:52px;height:52px;background:#8b8b8b;border:2px solid;border-color:#373737 #fff #fff #373737}
.charging-slot img{width:44px;height:44px;object-fit:contain;image-rendering:pixelated}.charging-slot span{position:absolute;right:2px;bottom:0;color:white;text-shadow:2px 2px #333}
figcaption{text-align:center;color:var(--vp-c-text-2)}strong{color:#3e9fc4}
</style>
