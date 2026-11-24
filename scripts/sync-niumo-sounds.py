#!/usr/bin/env python3
"""Convert the Niumo WAV bank to positional Ogg Vorbis and refresh sounds.json."""
import json
from pathlib import Path
import re
import tempfile

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "designs/niumo/sounds"
ASSETS = ROOT / "versions/1.20.1/src/main/resources/assets/pinkkicore"
STEM = re.compile(r"niumo_[0-9]{3,}")


def trim_lead(samples, sample_rate):
    duration = len(samples) / sample_rate
    if duration < 1.75:
        return samples
    import numpy as np

    frame = max(1, round(sample_rate / 100))
    windows = samples[:len(samples) // frame * frame].reshape(-1, frame)
    rms = np.sqrt(np.mean(windows ** 2, axis=1))
    db = 20 * np.log10(np.maximum(rms, 1e-7))
    background = np.percentile(db[:25], 35)
    speech_level = np.percentile(db, 95)
    # In clips around two seconds, a quiet consonant can precede the stronger onset.
    speech_margin, background_margin = (32, 7) if duration <= 2 else (26, 10)
    threshold = min(-26, max(-55, speech_level - speech_margin, background + background_margin))
    audible = db >= threshold
    # Require three active frames in a four-frame window to ignore isolated clicks.
    onsets = np.flatnonzero(np.convolve(audible.astype(int), np.ones(4, dtype=int), mode="valid") >= 3)
    if not len(onsets):
        return samples
    start = max(0, (int(onsets[0]) - 6) * frame)
    return samples[start:] if start >= 8 * frame else samples


def sync(source, assets):
    clips = sorted((path for path in source.iterdir() if path.suffix.lower() == ".wav"), key=lambda path: path.name)
    for path in clips:
        if path.suffix != ".wav" or not STEM.fullmatch(path.stem) or not path.is_file():
            raise ValueError(f"音效文件名应为 niumo_001.wav 这类小写名称：{path.name}")

    if clips:
        try:
            import soundfile
        except ModuleNotFoundError as error:
            raise ValueError("转换 WAV 需要 Python soundfile，请在运行脚本的环境安装 soundfile>=0.13") from error

    manifest = assets / "sounds.json"
    sounds = json.loads(manifest.read_text()) if manifest.exists() else {}
    sounds["niumo_random"] = {"sounds": [f"pinkkicore:niumo/{path.stem}" for path in clips]}
    target = assets / "sounds/niumo"

    # Decode and encode everything before replacing the live list or any of its clips.
    with tempfile.TemporaryDirectory(prefix=".niumo-audio-", dir=assets) as staging:
        staging = Path(staging)
        filenames = set()
        for path in clips:
            with soundfile.SoundFile(path) as audio:
                if audio.format not in {"WAV", "WAVEX", "RF64"} or not audio.frames:
                    raise ValueError(f"音效文件应为有声轨的 WAV：{path.name}")
                samples = audio.read(dtype="float32", always_2d=True).mean(axis=1)
                sample_rate = audio.samplerate
            samples = trim_lead(samples, sample_rate)
            filename = path.with_suffix(".ogg").name
            filenames.add(filename)
            soundfile.write(staging / filename, samples, sample_rate, format="OGG",
                            subtype="VORBIS", compression_level=0.85)

        (staging / manifest.name).write_text(json.dumps(sounds, indent=2, ensure_ascii=False) + "\n")
        if filenames:
            target.mkdir(parents=True, exist_ok=True)
            for filename in sorted(filenames):
                (staging / filename).replace(target / filename)
        (staging / manifest.name).replace(manifest)
        if target.is_dir():
            for path in target.glob("*.ogg"):
                if STEM.fullmatch(path.stem) and path.name not in filenames:
                    path.unlink()
    return len(clips)


if __name__ == "__main__":
    try:
        print(f"已同步 {sync(SOURCE, ASSETS)} 条牛魔音效")
    except (OSError, RuntimeError, ValueError) as error:
        raise SystemExit(str(error)) from error
