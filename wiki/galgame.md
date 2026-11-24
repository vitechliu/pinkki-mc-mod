<script setup>
import swordInStone from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/sword_in_stone.json'
import arcade from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/galgame_arcade.json'
import soul from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/galgame_soul.json'
import person from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/favorite_person.json'
import heart from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/galgame_heart.json'

const vanilla = {
  stone_sword: '石剑', cobblestone: '圆石', diamond: '钻石', emerald: '绿宝石', redstone: '红石',
  gold_ingot: '金锭', black_dye: '黑色染料', armor_stand: '盔甲架', netherite_ingot: '下界合金锭',
  heart_of_the_sea: '海洋之心', pink_wool: '粉色羊毛'
}
const pinkki = {
  guaba: '瓜巴', thunder_guaba: '雷霆瓜巴', pink_ingot: '粉红锭', zhang_fav: '张哥之爱',
  sword_in_stone: '石中剑', galgame_arcade: '旮旯游戏机', galgame_soul: '旮旯之魂',
  favorite_person: '最爱之人', galgame_heart: '旮旯之心'
}
const items = Object.fromEntries([
  ...Object.entries(vanilla).map(([id, name]) => ['minecraft:' + id, { name, image: '/images/minecraft/' + id + '.png' }]),
  ...Object.entries(pinkki).map(([id, name]) => ['pinkkicore:' + id, { name, image: '/images/' + id + '.png' }])
])
</script>

# 张家与旮旯

适用于 Minecraft 1.20.1 / Forge。

## 石中剑 {#sword-in-stone}

<CraftingRecipe :recipe="swordInStone" :items="items" />

将石中剑放在地面，用空的主手右键。它会碎裂并召出[张小弟或张哥](#zhang)。有 5% 的概率抽到张哥，空间太小时改为召唤张小弟。和平难度或空间不足时石中剑会保留。

## 张小弟与张哥 {#zhang}

张小弟和张哥会追逐 48 格内的生物，包括其他张家角色。张小弟有 20 颗心，接触伤害为 7 颗心。张哥体型翻倍，生命、攻击力和移速均为张小弟的 4 倍。穿戴[最爱之人](#favorite-person)可安全接近他们。

## 张哥之爱 {#zhang-fav}

![张哥之爱](/images/zhang_fav.png)

张小弟掉落 1–2 个，张哥掉落 4–10 个。用于制作旮旯游戏机和最爱之人。

## 旮旯游戏机 {#galgame-arcade}

![旮旯游戏机](/images/galgame_arcade.png)

<CraftingRecipe :recipe="arcade" :items="items" />

右键获得 10 分钟[旮旯](#galgame)。没有效果时获得 I，已有基础 I 时升至 II，再次使用可续时。

## 旮旯 {#galgame}

每级增加 50% 攻击力和 10% 移动速度，并减免 20% 来自生物近战或有生物来源的投射物伤害。摔落、火焰等环境伤害按原规则结算。

## 最爱之人 {#favorite-person}

<CraftingRecipe :recipe="person" :items="items" />

装备在粉饰槽后，张小弟与张哥不会攻击你，即使你先攻击。佩戴需要 Curios。

## 旮旯之魂 {#galgame-soul}

<CraftingRecipe :recipe="soul" :items="items" />

三级装备材料，采掘等级为下界合金。

<!-- guide-mod: tetra -->
Tetra 可在工作台制作金属剑刃和弓身。
<!-- /guide-mod -->

<!-- guide-mod: tconstruct -->
匠魂可在部件制造台制作武器、工具部件和护甲板。
<!-- /guide-mod -->

<!-- guide-mod: silentgear -->
寂静装备可用蓝图制作武器、工具或护甲的主部件。
<!-- /guide-mod -->

用含旮旯之魂的武器近战或远程命中可获得 10 秒旮旯 I，穿着含该材料的有效护甲会持续续时。持用有效武器或穿戴护甲额外增加 8% 移速，穿着护甲还增加 1 颗心的生命上限。重复装备同种材料不会叠加这些加成。

## 旮旯之心 {#galgame-heart}

<CraftingRecipe :recipe="heart" :items="items" />

装备在粉饰槽后，当前旮旯提高一级，取下时恢复基础等级，剩余时间照常倒数。佩戴需要 Curios，可与最爱之人同时装备。
