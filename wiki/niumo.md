<script setup>
import table from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table.json'
import helmet from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_helmet.json'
import whiskers from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_whiskers.json'
import splash from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table/splash_potion.json'
import niumo from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table/niumo.json'
import beef from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table/raw_beef.json'
import steak from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table/steak.json'
import apple from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table/enchanted_apple.json'
import soul from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/niumo_table/demon_soul.json'

const vanilla = {
  enchanting_table: '附魔台', iron_helmet: '铁头盔', string: '线',
  potion_water: '水瓶', splash_potion_niumo: '喷溅型牛魔药水',
  beef: '生牛肉', cooked_beef: '牛排', golden_apple: '金苹果', enchanted_golden_apple: '附魔金苹果'
}
const mod = {
  guaba: '瓜巴', pinkki_nm: '牛魔', niumo_table: '牛魔台', galgame_soul: '旮旯之魂',
  demon_soul: '魔人之魂', niumo_helmet: '牛魔头盔', niumo_whiskers: '牛魔绸鬓'
}
const items = {
  ...Object.fromEntries(Object.entries(vanilla).map(([id, name]) => ['minecraft:' + id, {name, image: '/images/minecraft/' + id + '.png'}])),
  ...Object.fromEntries(Object.entries(mod).map(([id, name]) => ['pinkkicore:' + id, {name, image: '/images/' + id + '.png'}])),
  'minecraft:potion': {name: '水瓶', image: '/images/minecraft/potion_water.png'},
  'minecraft:splash_potion': {name: '喷溅型牛魔药水', image: '/images/minecraft/splash_potion_niumo.png'},
  '#pinkkicore:raw_meats': {name: '生肉', image: '/images/minecraft/porkchop.png'},
  '#pinkkicore:cooked_meats': {name: '熟肉', image: '/images/minecraft/cooked_porkchop.png'}
}
</script>

# 牛魔

适用于 Minecraft 1.20.1 / Forge。

## 牛魔材料 {#pinkki-nm}

![牛魔](/images/pinkki_nm.png)

[牛人](#bullman)和[又菜又爱玩](#lao-cai)各掉落 1 个牛魔。用[牛魔台](#niumo-table)也可把瓜巴加工为牛魔。

## 牛魔效果 {#niumo}

立即清除生物的所有正面状态效果。原版普通牛受到牛魔效果时会变成牛人，幼牛也会变身。喷溅型牛魔药水可影响落点附近的生物。

## 牛魔台 {#niumo-table}

![牛魔台](/images/niumo_table.png)

<CraftingRecipe :recipe="table" :items="items" />

左侧放原料，点击牛头后从右侧取产物。每次点击加工一份。侧栏可设置六面进出料，默认底面出料、其余面进料。

<GuabaCharging :recipe="splash" :items="items" />

<GuabaCharging :recipe="niumo" :items="items" />

<GuabaCharging :recipe="beef" :items="items" />

<GuabaCharging :recipe="steak" :items="items" />

<GuabaCharging :recipe="apple" :items="items" />

<GuabaCharging :recipe="soul" :items="items" />

生肉和熟肉可用鸡肉、兔肉、羊肉、猪肉、牛肉，以及整合包中加入相应标签的肉类。

## 牛魔头盔 {#niumo-helmet}

<CraftingRecipe :recipe="helmet" :items="items" />

安装 Curios 后，在头饰或粉饰槽佩戴可免疫[牛魔效果](#niumo)。

## 牛魔绸鬓 {#niumo-whiskers}

<CraftingRecipe :recipe="whiskers" :items="items" />

安装 Curios 后，在项链槽佩戴。近战命中或佩戴时发射的投射物命中后附带[牛魔效果](#niumo)。

## 魔人之魂 {#demon-soul}

![魔人之魂](/images/demon_soul.png)

用[牛魔台](#niumo-table)把旮旯之魂加工成魔人之魂。三级装备材料，基础属性与旮旯之魂一致。

<!-- guide-mod: tetra -->
Tetra 可用于制作支持该材料的金属剑刃和弓身。
<!-- /guide-mod -->

<!-- guide-mod: tconstruct -->
匠魂可在部件制造台制作武器部件与护甲板。
<!-- /guide-mod -->

<!-- guide-mod: silentgear -->
寂静装备可用蓝图制作武器和护甲主部件。
<!-- /guide-mod -->

用魔人之魂武器实际命中会施加[牛魔效果](#niumo)。穿戴含该材料的防具遭到攻击时，对攻击者施加同样的效果，远程攻击会反制发射者。

## 牛人 {#bullman}

夜晚可在主世界野外遇见，牛遭到[牛魔效果](#niumo)也会变成牛人。不会被阳光灼伤，近战命中附带牛魔。死亡掉落 1 个牛魔，另有 50% 概率掉落 1 个皮革。

## 又菜又爱玩 {#lao-cai}

在主世界村庄的露天地面低概率出现。遭到攻击后会反击，近战命中附带牛魔。与村民和铁傀儡保持中立。死亡掉落 1 个牛魔，另有 50% 概率掉落 1 朵虞美人、20% 概率掉落 1 瓶喷溅型牛魔药水。
