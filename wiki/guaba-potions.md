<script setup>
import spec from '../versions/1.20.1/src/main/resources/data/pinkkicore/guaba_potions.json'
const numerals = ['I', 'II', 'III', 'IV']
</script>

# 瓜巴药水

适用 Minecraft 1.20.1 Forge。

获得瓜巴状态后，移动速度与额外护甲每秒分别随机变化一次。等级越高，两项属性的波动上下限都越高；效果结束后恢复，保留装备和其他效果的加成。

<table>
<thead><tr><th>等级</th><th>移动速度</th><th>额外护甲值</th></tr></thead>
<tbody><tr v-for="(level, index) in spec.levels" :key="index"><td>{{ numerals[index] }}</td><td>原来的 {{ Math.round(level.speed_min * 100) }}%–{{ Math.round(level.speed_max * 100) }}%</td><td>+{{ level.armor_min }}–{{ level.armor_max }}</td></tr></tbody>
</table>

I–III 可能暂时减速。护甲是增加护甲值，实际减伤仍遵循游戏规则。

## 酿造

使用酿造台，放入烈焰粉供能；底部可同时放入三瓶药水。

<GuabaBrewing />

延长版与增强版不能叠加。普通酿造最高 II，III 和 IV 暂无普通获取渠道。

## 喷溅与滞留

向瓜巴药水加入火药得到喷溅型，再加入龙息得到滞留型。也可以先转换瓶型，再酿造、延长或增强。喷溅持续时间随命中距离变化；滞留效果的单次持续时间为瓶装的四分之一。

整合包可能调整酿造规则。
