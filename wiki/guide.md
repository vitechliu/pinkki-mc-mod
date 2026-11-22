<script setup>
import guideRecipe from '../versions/1.20.1/src/main/resources/data/pinkkicore/recipes/guide_book.json'
const items = {
  'minecraft:book': { name: '书', image: '/images/minecraft/book.png' },
  'pinkkicore:guaba': { name: '瓜巴', image: '/images/guaba.png' },
  'pinkkicore:guide_book': { name: '瓜巴手册', image: '/images/guide_book.png' }
}
</script>

# 序章

PinkkiCore 是 Minecraft Java 版模组。你可以收集瓜巴与梦系材料，用工作方块加工装备、酿造瓜巴药水，也能遇见野生瓜巴龙并孵出自己的龙。

## 瓜巴手册 {#guide-book}

用书和瓜巴无序合成瓜巴手册，手持右键打开。[阅读操作](./handbook.md)适用于游戏内的离线手册。

<CraftingRecipe :recipe="guideRecipe" :items="items" />

## 项目版本与模组依赖 {#versions}

PinkkiCore 当前项目版本为 **0.1.0**。按你的 Minecraft 和加载器版本选择对应的模组包。客户端和服务端均需安装对应的 GeckoLib，整合包已有满足范围的版本时可以直接复用。

| Minecraft 与加载器 | 必装前置 | 可选联动 |
| --- | --- | --- |
| 1.20.1 · Forge 47.1.0 或更高的 47.x | GeckoLib 4.2.3 或更高的 4.x | JEI 15.2.0.27 或更高的 15.x、Curios 5.3.5 或更高的 5.x、寂静装备、匠魂、Tetra、铁魔法 |
| 1.21.1 · NeoForge 21.1.72 或更高的 21.1.x | GeckoLib 4.6.6 或更高的 4.x | JEI 19.21.0.247 或更高的 19.x、Curios 9.2.2 或更高的 9.x、寂静装备 4.2.1.1（需 Silent Lib 10.6.0）、铁魔法 3.16.3 |
| 1.19.1 · Forge 42.0.9 或更高的 42.x | GeckoLib 3.1.30 | JEI 11.2.0.244、Curios 5.1.6.4、Tetra 5.6.0（需 Mutil 5.2.0） |

参见[模组联动](./compatibility.md)，了解配方查询、装备材料特性与铁魔法加工。

## 阅读目录

- [材料](./items.md#guaba)
- [工作方块](./guaba-workbench.md)
- [基础武器](./items.md#guaba-sword)
- [药水效果](./guaba-potions.md)
- [生物](./bosses.md#guaba-dragon)

## 下载与反馈

通过 [GitHub Releases](https://github.com/vitechliu/pinkki-mc-mod/releases) 查看可用发行包。安装说明参见 [仓库 README](https://github.com/vitechliu/pinkki-mc-mod#readme)。

遇到问题可在 [GitHub Issues](https://github.com/vitechliu/pinkki-mc-mod/issues) 反馈，附上 Minecraft、加载器和 mod 版本。
