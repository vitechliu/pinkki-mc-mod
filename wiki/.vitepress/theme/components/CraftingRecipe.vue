<script setup lang="ts">
import { computed } from 'vue'
import { withBase } from 'vitepress'

type ItemVisual = { name: string; image: string; link?: string }
type Recipe = {
  type: string
  ingredients: { item: string }[]
  result: { item: string; count?: number }
}
const props = defineProps<{ recipe: Recipe; items: Record<string, ItemVisual> }>()
if (props.recipe.type !== 'minecraft:crafting_shapeless' || props.recipe.ingredients.length > 9) {
  throw new Error('CraftingRecipe 当前仅支持最多九格的无序合成')
}
for (const id of [...props.recipe.ingredients.map(i => i.item), props.recipe.result.item]) {
  if (!props.items[id]) throw new Error(`配方缺少物品图标：${id}`)
}
const slots = computed(() => Array.from({ length: 9 }, (_, i) => {
  const ingredient = props.recipe.ingredients[i]
  return ingredient ? props.items[ingredient.item] : null
}))
const output = computed(() => props.items[props.recipe.result.item])
const description = computed(() => `${props.recipe.ingredients.map(i => props.items[i.item].name).join(' + ')} → ${output.value.name} × ${props.recipe.result.count ?? 1}`)
</script>

<template>
  <figure class="crafting-recipe" :aria-label="description">
    <div class="recipe-heading">
      <span>合成</span>
      <span class="recipe-kind">无序合成</span>
    </div>
    <div class="recipe-panel">
      <div class="recipe-grid" aria-label="合成材料">
        <div v-for="(item, index) in slots" :key="index" class="recipe-slot">
          <img v-if="item" :src="withBase(item.image)" :alt="item.name" :title="item.name" width="40" height="40" />
        </div>
      </div>
      <svg class="recipe-arrow" width="42" height="30" viewBox="0 0 42 30" aria-hidden="true">
        <path d="M0 9h26V0l16 15-16 15v-9H0z" fill="currentColor" />
      </svg>
      <a v-if="output.link" class="recipe-slot recipe-output" :href="withBase(output.link)" :aria-label="`${output.name} × ${recipe.result.count ?? 1}`" :title="output.name">
        <img :src="withBase(output.image)" :alt="output.name" width="40" height="40" />
        <span class="recipe-count">{{ recipe.result.count ?? 1 }}</span>
      </a>
      <div v-else class="recipe-slot recipe-output" :title="output.name">
        <img :src="withBase(output.image)" :alt="output.name" width="40" height="40" />
        <span class="recipe-count">{{ recipe.result.count ?? 1 }}</span>
      </div>
    </div>
    <figcaption>{{ description }}</figcaption>
  </figure>
</template>

<style scoped>
.crafting-recipe { margin: 24px 0; max-width: 420px; }
.recipe-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-weight: 600; }
.recipe-kind { color: var(--vp-c-text-2); font-size: 13px; font-weight: 400; }
.recipe-panel { display: flex; align-items: center; justify-content: center; gap: 24px; padding: 26px 20px; border: 1px solid var(--vp-c-divider); border-radius: 8px; background: var(--vp-c-bg-soft); }
.recipe-grid { display: grid; grid-template-columns: repeat(3, 48px); gap: 2px; }
.recipe-slot { position: relative; display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; background: #8b8b8b; border: 2px solid; border-color: #373737 #fff #fff #373737; }
.recipe-slot img { width: 40px; height: 40px; object-fit: contain; image-rendering: pixelated; }
.recipe-arrow { flex-shrink: 0; color: #8b8b8b; }
.recipe-output { width: 60px; height: 60px; flex-shrink: 0; }
.recipe-output:focus-visible { outline: 3px solid var(--vp-c-brand-1); outline-offset: 4px; }
.recipe-count { position: absolute; right: 3px; bottom: 0; color: #fff; font-size: 18px; line-height: 22px; font-weight: 600; text-shadow: 2px 2px #333; }
figcaption { margin-top: 10px; color: var(--vp-c-text-2); text-align: center; font-size: 14px; line-height: 1.7; }
@media (max-width: 380px) {
  .recipe-panel { padding: 20px 10px; gap: 12px; }
  .recipe-grid { grid-template-columns: repeat(3, 40px); }
  .recipe-slot { width: 40px; height: 40px; }
  .recipe-slot img { width: 32px; height: 32px; }
  .recipe-output { width: 48px; height: 48px; }
  .recipe-arrow { width: 32px; }
}
</style>
