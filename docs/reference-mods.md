# Mod 源码参考库

本地源码统一放在 `.local/mod-sources/`，优先选取 Minecraft **1.20.1** 对应分支。该目录已由根 `.gitignore` 的 `.local/` 规则忽略，源码不纳入 PinkkiCore Git，也不参与项目构建。

## 当前索引

2026-10-23 下载并检查。以下均为独立 Git 浅克隆（`--depth 1 --single-branch`），包含所选提交的完整工作树，未下载完整历史或其他分支。仓库根目录保留上游许可证。

| Mod | 本地子目录 | 上游仓库 | 分支 | 下载时提交 |
| --- | --- | --- | --- | --- |
| 通用机械 | `Mekanism/` | [mekanism/Mekanism](https://github.com/mekanism/Mekanism) | `1.20.x` | [96c736241e30](https://github.com/mekanism/Mekanism/commit/96c736241e308a9cf1c73f7dcfa9aa221ea559d3) |
| EnderIO | `EnderIO/` | [Team-EnderIO/EnderIO](https://github.com/Team-EnderIO/EnderIO) | `1.20.1` | [18ee1b17bf68](https://github.com/Team-EnderIO/EnderIO/commit/18ee1b17bf6879298e911fff751204b07f2e4228) |
| 神秘学 | `Occultism/` | [klikli-dev/occultism](https://github.com/klikli-dev/occultism) | `version/1.20.1` | [a02238484fed](https://github.com/klikli-dev/occultism/commit/a02238484fed53ad58452858e8d70e2b33c2eff7) |
| 铁魔法 | `IronsSpellbooks/` | [iron431/irons-spells-n-spellbooks](https://github.com/iron431/irons-spells-n-spellbooks) | `1.20.1-legacy` | [b928995887aa](https://github.com/iron431/irons-spells-n-spellbooks/commit/b928995887aa362467fa999a9c9fb86419f00f2d) |
| 永恒枪械工坊·零 | `TACZ/` | [MCModderAnchor/TACZ](https://github.com/MCModderAnchor/TACZ) | `1.20.1` | [b482eff8c94a](https://github.com/MCModderAnchor/TACZ/commit/b482eff8c94a733ac8d0910193fca3893954027c) |
| 车万女仆 | `TouhouLittleMaid/` | [TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid) | `1.20` | [8a3ac5e9eacf](https://github.com/TartaricAcid/TouhouLittleMaid/commit/8a3ac5e9eacf0c6d63adf8832949f3ce81484fe6) |
| 神化 | `Apotheosis/` | [Shadows-of-Fire/Apotheosis](https://github.com/Shadows-of-Fire/Apotheosis) | `1.20` | [785d26e1f755](https://github.com/Shadows-of-Fire/Apotheosis/commit/785d26e1f755bdcd97351b33f203cb9d9a33948a) |
| 应用能源2 | `Applied-Energistics-2/` | [AppliedEnergistics/Applied-Energistics-2](https://github.com/AppliedEnergistics/Applied-Energistics-2) | `forge/1.20.1` | [1c2f96e70ca6](https://github.com/AppliedEnergistics/Applied-Energistics-2/commit/1c2f96e70ca6b61e8120482923e9775d772fd9e7) |
| 精妙背包 | `SophisticatedBackpacks/` | [P3pp3rF1y/SophisticatedBackpacks](https://github.com/P3pp3rF1y/SophisticatedBackpacks) | `1.20.x` | [3f7e10d15d5e](https://github.com/P3pp3rF1y/SophisticatedBackpacks/commit/3f7e10d15d5e3f8b8a975f52b780cd2dd79d621f) |
| 匠魂3 | `TinkersConstruct/` | [SlimeKnights/TinkersConstruct](https://github.com/SlimeKnights/TinkersConstruct) | `1.20.1` | [f4e77d43f39b](https://github.com/SlimeKnights/TinkersConstruct/commit/f4e77d43f39ba1047bf93ae816b7e1beaab514c2) |
| JEI（Just Enough Items） | `JustEnoughItems/` | [mezz/JustEnoughItems](https://github.com/mezz/JustEnoughItems) | `1.20.1` | [f1b7b7bd0d8a](https://github.com/mezz/JustEnoughItems/commit/f1b7b7bd0d8a0f55d90d1426cb84cb1a267d2a36) |
| JER（Just Enough Resources） | `JustEnoughResources/` | [way2muchnoise/JustEnoughResources](https://github.com/way2muchnoise/JustEnoughResources) | `1.20.1` | [62cbeaf4375e](https://github.com/way2muchnoise/JustEnoughResources/commit/62cbeaf4375e11701e2533f9f4fffc4040570351) |

以上分支的构建配置均已核对为 1.20.1。铁魔法使用 `1.20.1-legacy`，其配置版本为 `1.20.1-3.4.0.11`；另一个 `1.20.1-dragon` 分支配置版本为 `1.20.1-3.1.7.1`。应用能源 2 选用 Forge 分支。这里的源码快照不代表对应发行包已经构建或运行验证。

机器可读清单位于 `.local/mod-sources/sources.json`，记录 URL、分支、完整提交号、版本配置位置和许可证文件；每个仓库的下载日志位于同目录的 `<目录名>.clone.log`。

## 查找实现

多数仓库的主要 Java 入口在 `<子目录>/src/main/java/`，资源在 `<子目录>/src/main/resources/`；部分项目另有 API、数据生成及其他 source set，可按仓库结构继续查找。

JEI 为多模块项目，优先查看 `JustEnoughItems/Forge/`、`ForgeApi/`、`Common/`、`CommonApi/`、`Gui/` 和 `Library/` 下的 `src/main/java/`，分别参考 Forge 适配、公共 API、界面和配方展示实现。

JER 同样采用多模块结构，Forge 适配与 API 位于 `JustEnoughResources/Forge/`、`ForgeApi/`，共享实现与 API 位于 `Common/`、`CommonApi/`，可从这些模块的 `src/main/java/` 查找资源分布与掉落展示实现。

从 PinkkiCore 根目录搜索；由于 `.local/` 被忽略，`rg` 需要显式加 `--no-ignore`：

```bash
# 跨仓库搜索 Java 实现
rg --no-ignore -n -g '*.java' 'class .*Screen|MenuType|Capability' .local/mod-sources

# 查找模型、语言、配方等资源
rg --files --no-ignore .local/mod-sources/TinkersConstruct/src/main/resources

# 查看当前分支和提交
git -C .local/mod-sources/Mekanism log -1 --oneline
```

## 补充下载与更新

新电脑可按表中仓库和分支逐一恢复，例如：

```bash
mkdir -p .local/mod-sources
git clone --depth 1 --single-branch --branch 1.20.x \
  https://github.com/mekanism/Mekanism.git .local/mod-sources/Mekanism
```

需要更新时先检查本地改动，再显式拉取所选分支。例如：

```bash
git -C .local/mod-sources/Mekanism status --short
git -C .local/mod-sources/Mekanism pull --ff-only
```

有本地改动时先保留改动；不使用强制重置覆盖。更新后同步本索引及本地 `sources.json` 的提交与版本证据。需要历史时再按需 `git fetch --deepen` 或 `--unshallow`。

参考实现及资源时遵守各上游许可证，具体复用前检查对应文件的授权；PinkkiCore 的 MIT 许可证不替代上游授权。

本次仅建立开发参考资料，不改变游戏行为，因此无需同步游戏内手册、玩家 Wiki 或创建功能适配待办。
