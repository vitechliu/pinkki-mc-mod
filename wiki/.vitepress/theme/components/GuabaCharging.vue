<script setup lang="ts">
import { withBase } from 'vitepress'
import { ingredientKey, type RecipeIngredient } from './recipe-ingredient'
type Visual = { name: string; image: string; sprite?: boolean }
const props = defineProps<{
  recipe: { type: string; ingredient: RecipeIngredient; input_count?: number; result: { item: string; count: number } }
  items: Record<string, Visual>
}>()
const niumo = props.recipe.type === 'pinkkicore:niumo_table'
if (!niumo && props.recipe.type !== 'pinkkicore:guaba_charging') throw new Error('不支持的单输入加工配方')
const input = props.items[ingredientKey(props.recipe.ingredient)]
const output = props.items[props.recipe.result.item]
if (!input || !output) throw new Error('加工配方缺少图标')
</script>
<template>
  <figure class="charging">
    <figcaption>{{ niumo ? "牛魔台" : "瓜巴充电桩 · 1 秒" }}</figcaption>
    <div class="charging-row">
      <div class="charging-slot"><img :src="withBase(input.image)" :alt="input.name" :style="input.sprite ? { objectFit: 'cover', objectPosition: 'top' } : {}" /><span>{{ recipe.input_count ?? 1 }}</span></div>
      <strong class="charging-arrow">
        <img v-if="niumo" :src="withBase('/images/pinkki_nm.png')" alt="点击牛头" />
        <span v-else>⚡</span> →
      </strong>
      <div class="charging-slot"><img :src="withBase(output.image)" :alt="output.name" :style="output.sprite ? { objectFit: 'cover', objectPosition: 'top' } : {}" /><span>{{ recipe.result.count }}</span></div>
    </div>
    <figcaption>{{ input.name }} → {{ output.name }}</figcaption>
  </figure>
</template>
<style scoped>
.charging{max-width:420px;margin:24px 0;padding:20px;border:1px solid var(--vp-c-divider);border-radius:8px;background:var(--vp-c-bg-soft)}
.charging-row{display:flex;gap:28px;align-items:center;justify-content:center;margin:14px}
.charging-slot{position:relative;display:flex;align-items:center;justify-content:center;width:52px;height:52px;background:#8b8b8b;border:2px solid;border-color:#373737 #fff #fff #373737}
.charging-slot img{width:44px;height:44px;object-fit:contain;image-rendering:pixelated}.charging-slot span{position:absolute;right:2px;bottom:0;color:white;text-shadow:2px 2px #333}
figcaption{text-align:center;color:var(--vp-c-text-2)}strong{color:#3e9fc4}
.charging-arrow{display:flex;align-items:center;gap:6px}.charging-arrow img{width:24px;height:24px;image-rendering:pixelated}
</style>
