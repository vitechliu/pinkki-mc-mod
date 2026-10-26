<script setup>
import spec from '../versions/1.20.1/src/main/resources/data/pinkkicore/guaba_potions.json'
const numerals = ['I', 'II', 'III', 'IV']
</script>

# 瓜巴药水

适用 Minecraft 1.20.1 Forge。

获得瓜巴状态后，移动速度与护甲修正每秒分别随机变化一次。两项修正的正负范围相同，等级越高只扩大波动幅度，不提高加成的平均值；效果结束后恢复，保留装备和其他效果的加成。

<table>
<thead><tr><th>等级</th><th>移动速度</th><th>护甲修正</th></tr></thead>
<tbody><tr v-for="(level, index) in spec.levels" :key="index"><td>{{ numerals[index] }}</td><td>原来的 {{ Math.round(level.speed_min * 100) }}%–{{ Math.round(level.speed_max * 100) }}%</td><td>{{ level.armor_min }} 至 +{{ level.armor_max }}</td></tr></tbody>
</table>

所有等级都可能加速或减速、增加或降低护甲。移速以原来的 100% 为中心对称波动；护甲修正以 0 为中心对称波动。实际护甲最低为 0，不足以抵扣负修正时不会变成负护甲，实际减伤仍遵循游戏规则。

使用瓜巴材料制作的[兼容装备](./compatibility.md#guaba)近战命中时，也可使目标获得持续 10 秒的瓜巴 I。

## 酿造

使用酿造台，放入烈焰粉供能；底部可同时放入三瓶药水。

<GuabaBrewing />

安装 JEI 后，可在酿造分类查询瓜巴药水的获取配方，也可查看瓜巴、红石粉和荧石粉的酿造用途。

延长版与增强版不能叠加。普通酿造最高 II，III 和 IV 暂无普通获取渠道。

## 喷溅与滞留

向瓜巴药水加入火药得到喷溅型，再加入龙息得到滞留型。也可以先转换瓶型，再酿造、延长或增强。喷溅持续时间随命中距离变化；滞留效果的单次持续时间为瓶装的四分之一。

整合包可能调整酿造规则。

## 雷霆瓜巴药水 {#thunder-guaba}

使用[雷霆瓜巴](./items.md#thunder-guaba)代替瓜巴酿造。修正仍每秒独立随机，同级波动幅度是普通瓜巴的 150%；两种状态可以同时存在。

<table data-effect="thunder_guaba">
<thead><tr><th>等级</th><th>移速修正</th><th>护甲修正</th></tr></thead>
<tbody><tr v-for="(level, index) in spec.levels" :key="index"><td>{{ numerals[index] }}</td><td>±{{ Math.round((level.speed_max - 1) * spec.thunder.amplitude_multiplier * 1000) / 10 }}%</td><td>±{{ level.armor_max * spec.thunder.amplitude_multiplier }}</td></tr></tbody>
</table>

III、IV 的强减速可能使实体暂时停住；最终移速和护甲仍受原版属性下限限制。

持有效果期间，每隔随机 **5–10 秒**，实体所在位置会落下一次真实雷击。首次雷击也需等待 5–10 秒；雷击强度和间隔不随等级变化，刷新或升级不会重新开始等待。雷击会造成伤害，并可能波及附近实体、引火或触发雷击转换。

<GuabaBrewing thunder />

延长与增强不能叠加，普通酿造最高 II，III、IV 暂无普通获取渠道。支持喷溅、滞留以及 JEI 酿造查询。
