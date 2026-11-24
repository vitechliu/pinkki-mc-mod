# 牛魔随机音效

把 WAV 原稿放在本目录。使用 `niumo_001.wav`、`niumo_002.wav`、`niumo_003.wav` 这样的名称，后续使用新编号即可，允许跳号。

在仓库根目录运行 `python3 scripts/sync-niumo-sounds.py`。脚本使用 Python 的 `soundfile>=0.13` 将 WAV 转为游戏用的单声道 Ogg Vorbis。请先在运行脚本的 Python 环境安装 `soundfile`（`python -m pip install "soundfile>=0.13"`）。脚本会在至少 1.75 秒的 WAV 开头检测起音，识别前导空白后裁掉，保留约 60 毫秒起音余量。接近 2 秒的片段会额外留意较轻的起音，再更新列表。牛魔台成功加工和玩家掷出喷溅型牛魔药水时，都从这份列表等概率抽选。列表为空时保持静音。

更新开发覆盖包时运行 `python3 scripts/hot-sync.py --resources-only`，再按 F3+T。正式包将游戏资源目录中的 OGG 和 `sounds.json` 一起打包。
