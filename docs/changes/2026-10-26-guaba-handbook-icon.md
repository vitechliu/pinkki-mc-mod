# 2026-10-26：瓜巴手册名称与图标

- 变更 ID：2026-10-26-guaba-handbook-icon
- 需求与完成行为：中文名称改为“瓜巴手册”，英文为 Guaba Handbook；使用带瓜巴封面图案的专属书本纹理，替换原版书图标。物品 ID、合成配方与阅读功能保持原有契约。
- 源码位置：1.20.1 语言文件、`assets/pinkkicore/models/item/guide_book.json`、`textures/item/guide_book.png`；Wiki 图片、标题、链接文字与配方图标映射。
- 验收场景：物品名称、背包及手册配方产物显示专属封面，Wiki 对应图片一致；右键阅读标题显示“瓜巴手册”。
- 1.20.1 验证：见本条后续记录；不启动客户端，游戏内实际显示待人工验收。
- 移植注意：其他目标需单独接入名称和纹理资源，不能据此宣称已适配。

| 目标 | 状态 | 说明 / 验证证据 |
| --- | --- | --- |
| 1.21.1 NeoForge | 待适配/验证 | 后续按需补齐 |
| 1.19.1 Forge | 待适配/验证 | 后续按需补齐 |

验证结果：1.20.1 构建、发行包校验、Wiki 构建与 4 项手册内容检查通过。发行检查核对中英文名称、模型引用、32×32 纹理及 Wiki 图片字节一致。图标使用 imagegen 制作并缩至游戏纹理，采用透明背景；未启动客户端。日志 `.local/handbook-icon-build.log`、`handbook-icon-wiki.log`、`handbook-icon-tests.log`。完整部署回执 `.local/hmcl/deployments/20261026-215835-036363/receipt.json` 为 installed，clean、compat 安装 SHA256 均为 `e0afe364d3f0c7126a1b93e41bca4eeaa71c0a47d131bcce777ae1738043da27`；热同步已完成，记录 `.local/handbook-icon-hot-sync.log`，需玩家 F3+T、/reload。
