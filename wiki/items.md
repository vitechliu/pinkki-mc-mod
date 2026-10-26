# 道具与方块

## 瓜巴（Guaba） {#guaba}

![瓜巴图标](/images/guaba.png)

> 一切瓜巴的起源

瓜巴是基础合成材料，可用于[酿造瓜巴药水](./guaba-potions.md)、[瓜巴工作台加工](./guaba-workbench.md)和[其他模组的装备制作](./compatibility.md#guaba)。不可食用，每组最多堆叠 64 个。

获取方式：[西瓜片与泥巴合成](./recipes.md#guaba)。

## 雷霆瓜巴（Thunder Guaba） {#thunder-guaba}

![雷霆瓜巴图标](/images/thunder_guaba.png)

将[瓜巴](#guaba)丢在地上，让雷击命中掉落物，即可按 **1:1** 转为雷霆瓜巴。整组或多组可以一起转换，数量不变。

![雷击转换：瓜巴到雷霆瓜巴](/images/thunder_guaba_conversion.svg)

可在雷雨中借助避雷针把落雷引到瓜巴附近；只有实际被闪电击中的掉落物会转换，背包里的瓜巴不会变化。

安装 JEI 后，在雷霆瓜巴上按 **R**，或在瓜巴上按 **U**，可查看“雷击转换”页面。

雷霆瓜巴不可食用，每组最多 64 个。它天然耐火，也不会被后续雷击销毁；可用于[酿造雷霆瓜巴药水](./guaba-potions.md#thunder-guaba)。

## 饼蛙刷怪蛋 {#bingwa-spawn-egg}

注册 ID：`pinkkicore:bingwa_spawn_egg`。

功能：对方块使用，召唤一只[饼蛙](./bosses.md#bingwa)。

## 瓜巴工作台 {#guaba-workbench}

![瓜巴工作台](/images/guaba_workbench.svg)

消耗瓜巴转换原料的工作方块，支持红石与六面输入输出控制。从创造模式“功能方块”获取，没有生存合成配方。

查看[操作与自动化说明](./guaba-workbench.md)和[加工配方](./recipes.md#guaba-conversion)。

## 粉红妹刷怪蛋 {#pinkki-spawn-egg}

注册 ID：`pinkkicore:pinkki_spawn_egg`。使用后生成[粉红妹](./bosses.md#pinkki)，位于创造模式的刷怪蛋分类。

## 瓜巴手册 {#guide-book}

使用书和瓜巴[无序合成](./recipes.md#guide-book)，右键打开。可离线查阅道具、生物、工作台与配方，使用方法见[瓜巴手册](./handbook.md)。

## 瓜巴剑 {#guaba-sword}

![瓜巴剑](/images/guaba_sword.png)

在[瓜巴工作台](./recipes.md#guaba-sword)中消耗 1 个瓜巴，将钻石剑转为瓜巴剑，耗时 1 秒，保留附魔、名称和已消耗耐久。

玩家每次攻击的基础攻击力在钻石剑基础上随机增减 0～3 点；普通满蓄力攻击为 4～10 点。攻击速度、耐久和修理材料与钻石剑相同。命中赋予目标 5 秒瓜巴 I，重复命中刷新时间，不降低已有更强的瓜巴效果。

## 瓜巴弓 {#guaba-bow}

![瓜巴弓](/images/guaba_bow.png)

用弓与 1 个瓜巴在[瓜巴工作台加工](./recipes.md#guaba-bow)获得，耗时 1 秒，保留附魔、名称和已消耗耐久。

箭矢每次命中在原版伤害上随机 −3～+3 点，最低为 0，并赋予目标 5 秒瓜巴 I；再次命中刷新时间，不降低已有更强效果。蓄力、箭种、耐久及力量、冲击、火矢、无限等附魔沿用原版弓。放箭后切换武器不影响已射出箭矢的瓜巴效果。

## 瓜巴充电桩 {#guaba-charging-station}

![瓜巴充电桩](/images/guaba_charging_station.png)

用[避雷针、附魔台与雷霆瓜巴有序合成](./recipes.md#guaba-charging-station)。右键打开，左侧放原料、右侧取产物；每次消耗 1 个原料，约 1 秒完成，不需要额外瓜巴、燃料或能量。

可把瓜巴转成雷霆瓜巴、苦力怕刷怪蛋转成闪电苦力怕刷怪蛋，查看[充能配方](./recipes.md#guaba-charging)。右上角提供与瓜巴工作台相同的四态红石控制；六面可设为输入、输出、双向或禁用，默认底面输出、其余面输入。输出满、产物不兼容或红石暂停时会等待，不吞材料。可用漏斗或物品管道自动化，并在 JEI 查询加工来源。

## 闪电苦力怕刷怪蛋 {#charged-creeper-spawn-egg}

![闪电苦力怕刷怪蛋](/images/charged_creeper_spawn_egg.png)

由[瓜巴充电桩](./recipes.md#guaba-charging)加工苦力怕刷怪蛋获得。对方块使用或由发射器发射，生成带电的苦力怕；不改变原版苦力怕刷怪蛋。对着刷怪笼使用时生成苦力怕，不修改刷怪笼。
