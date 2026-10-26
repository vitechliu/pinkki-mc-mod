<script setup>
import { withBase } from 'vitepress'
import spec from '../../../../versions/1.20.1/src/main/resources/data/pinkkicore/guaba_potions.json'
const props = defineProps({ thunder: { type: Boolean, default: false } })
const names = { 'minecraft:awkward': '粗制的药水', 'pinkkicore:thunder_guaba': '雷霆瓜巴 I（3:00）', 'pinkkicore:long_thunder_guaba': '雷霆瓜巴 I（8:00）', 'pinkkicore:strong_thunder_guaba': '雷霆瓜巴 II（1:30）', 'pinkkicore:guaba': '瓜巴 I（3:00）', 'pinkkicore:long_guaba': '瓜巴 I（8:00）', 'pinkkicore:strong_guaba': '瓜巴 II（1:30）' }
const reagents = {
  'pinkkicore:thunder_guaba': { name: '雷霆瓜巴', image: '/images/thunder_guaba.png' },
  'pinkkicore:guaba': { name: '瓜巴', image: '/images/guaba.png' },
  'minecraft:redstone': { name: '红石粉', image: '/images/minecraft/redstone.png' },
  'minecraft:glowstone_dust': { name: '荧石粉', image: '/images/minecraft/glowstone_dust.png' }
}
</script>
<template>
  <figure v-for="rule in (props.thunder ? spec.thunder.brewing : spec.brewing)" :key="rule.output" class="brewing" :style="{ '--potion-color': props.thunder ? '#92d7f4' : '#a5ad35', '--potion-overlay': `url(${withBase('/images/minecraft/potion_overlay.png')})` }" :aria-label="`${names[rule.input]} + ${reagents[rule.ingredient].name} → ${names[rule.output]}`">
    <div class="reagent"><img :src="withBase(reagents[rule.ingredient].image)" :alt="reagents[rule.ingredient].name" />{{ reagents[rule.ingredient].name }}</div>
    <div class="row">
      <div class="bottle"><span class="potion" :class="{ awkward: rule.input === 'minecraft:awkward' }"><img :src="withBase('/images/minecraft/potion.png')" alt="药水瓶" /></span>{{ names[rule.input] }}</div>
      <div class="stand"><span>↓</span><img :src="withBase('/images/minecraft/brewing_stand.png')" alt="酿造台" /><span>→</span></div>
      <div class="bottle"><span class="potion"><img :src="withBase('/images/minecraft/potion.png')" :alt="names[rule.output]" /></span>{{ names[rule.output] }}</div>
    </div>
  </figure>
</template>
<style scoped>
.brewing { border: 1px solid var(--vp-c-divider); border-radius: 8px; padding: 16px; background: var(--vp-c-bg-soft); }
.reagent, .bottle { display:flex; flex-direction:column; align-items:center; gap:6px; font-size:14px; text-align:center; }
.reagent img, .bottle img, .stand img { width:40px; height:40px; image-rendering:pixelated; }
.row { display:flex; justify-content:space-around; align-items:center; gap:12px; }
.bottle { flex:1; }
.stand { display:flex; flex-direction:column; align-items:center; color:var(--vp-c-text-2); }
.potion { position:relative; display:block; width:40px; height:40px; }
.potion::before { content:''; position:absolute; inset:0; background:var(--potion-color); mask:var(--potion-overlay) center / contain no-repeat; }
.potion.awkward::before { background:#385dc6; }
.potion img { position:relative; }
</style>
