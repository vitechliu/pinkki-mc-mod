# 瓜巴工作台：实现与开发

适用 Minecraft 1.20.1 / Forge，PinkkiCore 0.1.0 开发中，未发布。玩家操作见 [Wiki](../wiki/guaba-workbench.md)。

## 内容与边界

- 方块、方块物品、方块实体与菜单：`pinkkicore:guaba_workbench`。创造模式“功能方块”获取，或 `/give @s pinkkicore:guaba_workbench`；用户尚未指定生存合成配方，本次未自行添加。
- 使用用户确认的第三稿 Java 模型：1×1 占地，12.8/16 高度；四向放置和同高碰撞形状。设计源保留在 `designs/guaba-workbench/`。
- 三槽：0 瓜巴、1 原料、2 产物。Shift 点击瓜巴进槽 0、其他原料进槽 1；槽 2 拒绝放入。仅服务端进行处理与扣料，菜单使用原版容器槽同步、ContainerData 和按钮协议，没有另外开放任意坐标配置网络包。
- 每次固定 20 个服务端刻，正常 20 TPS 下约 1 秒。完成时同时消耗并产出；原料不匹配、瓜巴不足、输出不兼容/满、红石不允许时均不扣料。输出堵塞及红石暂停保留进度；原料或瓜巴发生存取时重新计时。服务器/区块卸载期间不离线加工，已保存进度在加载后继续。
- 占位配方为 1 泥土 + 1 瓜巴 → 1 泥巴。它是转换功能示例，不是最终平衡设计。
- 界面和菜单配置按钮限定当前容器、方块与距离，旁观者不能修改配置。拆除时内部三槽物品与方块分别掉落；重新放置为默认设置，不把配置写入掉落物。

## 自动化与持久化

红石四状态按用户确认：忽略、高、低、始终禁用。判断实际世界 `hasNeighborSignal`，不受面输入输出开关影响。红石仅暂停加工，不阻断物品端口。

端口按世界六面提供 Forge `ITEM_HANDLER`：禁用、输入、输出、双向、瓜巴输入。普通输入仅允许槽 0/1，槽 0 只接受瓜巴；输出仅允许槽 2。瓜巴输入模式仅允许向槽 0 送入瓜巴，拒绝槽 1/2 存入及所有提取；瓜巴槽满时不回落到原料槽。默认底面输出，其余五面输入。新模式追加为持久化编号 4，既有编号 0..3、默认设置与普通输入/双向语义保持不变。设置直接影响已经缓存的端口对象；`simulate=true` 不修改库存。无方向 capability 不开放，避免绕过六面配置。禁用面仍可被工具发现 capability，但所有实际存取均拒绝。

工作台主面板沿用 `AbstractContainerScreen` 的水平居中，JEI 通过动态扩展矩形避让右侧按钮和设置抽屉，不再为了给抽屉预留空间而提前移动主面板。居中及专用供料修正见 [2026-10-24 记录](changes/2026-10-24-guaba-workbench-centering-input.md)。

这是**被动端口**：原版漏斗或支持该接口的管道负责搬运，机器不自行轮询邻居或主动弹出。未安装、未运行 Mekanism / EnderIO 管道，不能用原版漏斗通过结论替代完整整合包验证。

库存、进度、当前配方、红石模式和六面模式保存在方块实体 NBT。异常模式编号回退到安全定义，进度约束为 0..19；配方不再匹配时重新计时。

## 数据包配方

主版本资源：`data/pinkkicore/recipes/guaba_conversion/dirt_to_mud.json`。

```json
{
  "type": "pinkkicore:guaba_conversion",
  "ingredient": { "item": "minecraft:dirt" },
  "input_count": 1,
  "guaba_cost": 1,
  "result": { "item": "minecraft:mud", "count": 1 }
}
```

`ingredient` 使用原生 Ingredient，可指定物品或标签；`input_count` 默认 1，范围 1..64；`guaba_cost` 默认 1，范围 0..64（0 表示仍需放入瓜巴，但不消耗）。结果数量必须在目标物品单堆叠范围内。原料直接消耗，不额外产生容器返还物。相同原料应只定义一个匹配配方，不依赖配方扫描顺序实现优先级。

转换使用当前服务端 RecipeManager，支持数据包覆盖、`/reload` 与登录时的原生配方同步。进度期间更换配方 ID 会重新计时；同 ID 内容更新后按当前配方结算。不把加工配方放进原版合成表，不自动解锁配方书。Wiki 加工图直接导入基准 JSON，整合包覆盖以后以实际配方为准。

## 实现位置与参考

Java 在 `versions/1.20.1/src/main/java/io/github/vitech/pinkkicore/forge/workbench/`；客户端仅在 `client/GuabaWorkbenchScreen.java` 与 `ClientEvents.java`。没有服务端类直接引用 `net.minecraft.client`。

参考本地固定版本 [Mod 索引](reference-mods.md)：EnderIO `src/api/.../RedstoneControl.java` 的四态语义、`src/machines/.../io/item/MachineInventory.java` 的逐面包装模式，以及 Mekanism `GuiRedstoneControlTab` / `GuiSideConfigurationTab` 的侧边控件组织。仅借鉴交互与接口思路；本次代码和 UI 绘制自行实现，不引入上述模组依赖、不复制其图标或其他美术。

## 验证

JEI 加工预览的实现、依赖和验证边界见 [JEI 兼容记录](changes/2026-10-23-guaba-workbench-jei.md)。工作台本体仍未添加生存合成配方。

```bash
./scripts/mod.sh 1.20.1 build workbenchProbeJar -PworkbenchProbe --console=plain
python3 scripts/verify-artifact.py 1.20.1
python3 scripts/check-workbench.py --geckolib <本地1.20.1-Forge-GeckoLib发行JAR>
python3 designs/guaba-workbench/validate.py
npm ci
npm run wiki:build
```

`check-workbench.py` 只使用已有测试安装的库和已确认 EULA，在 `.local/workbench-tests/<时间戳>/` 启动一次性发行 JAR 服务端，绑定 `127.0.0.1:25581`，使用 `--nogui`，随后正常停服。探针只进 `workbench-probe.jar`，不进 mod 发行包。

已验证记录见 [变更记录](changes/2026-10-23-guaba-workbench.md)。客户端界面、真实网络多人同步与第三方管道仍待人工验收；没有启动 Minecraft 图形客户端。手工步骤：取得并放置工作台，打开后按 Wiki 配方加工；检查 Shift 点击、红石按钮及信号提示、六面面板、漏斗；两名玩家同时查看时核对进度和配置同步；拆除时确认界面失效和物品掉落。

## 手册同步缺口

本项目自研手册系统尚未实现，本次没有用第三方手册库替代。Wiki、加工图和玩家更新已同步，但不能声称游戏内手册已同步可读，JEI 也不能替代本项目手册。待手册基础能力就绪，以稳定条目 ID `guaba_workbench` 接入获取、三槽使用、四种红石与六面端口说明及可选 JEI 预览入口；关联当前 `guaba_conversion` 配方，以实际 RecipeManager 的物品图标与结果展示加工示意。该缺口同时记录在本次变更中。
