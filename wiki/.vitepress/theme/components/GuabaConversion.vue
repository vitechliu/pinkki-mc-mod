<script setup lang="ts">
import { withBase } from 'vitepress'
type Visual = { name: string; image: string }
const props = defineProps<{
  recipe: { type: string; ingredient: { item: string }; input_count: number; guaba_cost: number; result: { item: string; count: number } }
  items: Record<string, Visual>
}>()
if (props.recipe.type !== 'pinkkicore:guaba_conversion') throw new Error('不是瓜巴转换配方')
for (const id of [props.recipe.ingredient.item, props.recipe.result.item, 'pinkkicore:guaba']) {
  if (!props.items[id]) throw new Error(`缺少加工图标：${id}`)
}
const fuel = props.items['pinkkicore:guaba']
const input = props.items[props.recipe.ingredient.item]
const output = props.items[props.recipe.result.item]
</script>

<template>
  <figure class="guaba-conversion" aria-label="瓜巴工作台加工配方">
    <div class="heading"><img :src="withBase('/images/guaba_workbench.svg')" alt="瓜巴工作台" width="32" height="32" />瓜巴工作台 <span>1 秒</span></div>
    <div class="machine">
      <div class="fuel"><div class="slot" :title="fuel.name"><img :src="withBase(fuel.image)" :alt="fuel.name" /><span>{{ recipe.guaba_cost }}</span></div><span>{{ fuel.name }}</span></div>
      <div class="processing">
        <div class="slot" :title="input.name"><img :src="withBase(input.image)" :alt="input.name" /><span>{{ recipe.input_count }}</span></div>
        <span class="arrow" aria-label="转换为">→</span>
        <div class="slot" :title="output.name"><img :src="withBase(output.image)" :alt="output.name" /><span>{{ recipe.result.count }}</span></div>
      </div>
    </div>
    <figcaption>{{ input.name }} → {{ output.name }}</figcaption>
  </figure>
</template>

<style scoped>
.guaba-conversion { max-width: 420px; margin: 24px 0; }
.heading { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.heading span { margin-left: auto; color: var(--vp-c-text-2); }
.machine { padding: 18px 26px; border: 1px solid var(--vp-c-divider); border-radius: 8px; background: var(--vp-c-bg-soft); }
.fuel { display: flex; align-items: center; gap: 12px; }
.processing { display: flex; align-items: center; justify-content: center; gap: 28px; margin-top: 16px; }
.slot { position: relative; width: 48px; height: 48px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: #8b8b8b; border: 2px solid; border-color: #373737 #fff #fff #373737; }
.slot img { width: 40px; height: 40px; image-rendering: pixelated; }
.slot span { position: absolute; bottom: 0; right: 3px; color: white; text-shadow: 2px 2px #333; }
.arrow { font-size: 30px; color: var(--vp-c-text-2); }
figcaption { text-align: center; margin-top: 10px; color: var(--vp-c-text-2); }
@media (max-width: 380px) { .machine { padding: 16px; } .processing { gap: 16px; } }
</style>
