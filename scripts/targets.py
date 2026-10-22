#!/usr/bin/env python3
"""选择构建目标，查看适配检查点；不构建、不部署、不启动游戏。"""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent
REGISTRY = ROOT / "config/minecraft-targets.json"
STATE = ROOT / "docs/porting/status.json"


def registry():
    data = json.loads(REGISTRY.read_text())
    versions = [row["minecraft"] for row in data["targets"]]
    if len(versions) != len(set(versions)) or data["primary"] not in versions:
        raise ValueError("目标注册表重复或缺少主版本")
    return data


def select(value):
    data = registry()
    names = [row["minecraft"] for row in data["targets"]] if value == "all" else value.split(",")
    names = [name.strip() for name in names]
    lookup = {row["minecraft"]: row for row in data["targets"]}
    if not names or len(names) != len(set(names)) or any(name not in lookup for name in names):
        raise ValueError(f"目标无效或重复：{value}；可选：{', '.join(lookup)}，或 all")
    return [lookup[name] for name in names]


def files_in(paths):
    files = []
    for relative in paths:
        path = ROOT / relative
        if path.is_dir():
            files.extend(p for p in path.rglob("*") if p.is_file())
        elif path.is_file():
            files.append(path)
    return sorted(set(files), key=lambda p: p.relative_to(ROOT).as_posix())


def fingerprint(paths):
    digest = hashlib.sha256()
    for path in files_in(paths):
        digest.update(path.relative_to(ROOT).as_posix().encode() + b"\0")
        content = path.read_bytes()
        if path.suffix in {".java", ".json", ".toml", ".mcmeta", ".properties", ".gradle", ".xml", ".txt"}:
            content = content.replace(b"\r\n", b"\n")
        digest.update(hashlib.sha256(content).digest())
    return digest.hexdigest()


def target_paths(version):
    base = f"versions/{version}"
    return [f"{base}/src/main", f"{base}/build.gradle", f"{base}/settings.gradle",
            f"{base}/gradle.properties", f"{base}/gradle/wrapper/gradle-wrapper.properties"]


def source_fingerprint():
    return fingerprint(["common/src/main", "mod.properties", "gradle/forge.gradle",
                        *target_paths(registry()["primary"])])


def checkpoint(version, evidence):
    if version == registry()["primary"]:
        raise ValueError("检查点用于次要目标；日常主版本验证记入变更记录")
    proof = (ROOT / evidence).resolve()
    if not proof.is_relative_to(ROOT.resolve()) or not proof.is_file() or proof.stat().st_size == 0:
        raise ValueError("需要项目内已存在且非空的验证记录或日志")
    data = json.loads(STATE.read_text()) if STATE.exists() else {"targets": {}}
    source_ref = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    data["targets"][version] = {
        "source_fingerprint": source_fingerprint(), "target_fingerprint": fingerprint(target_paths(version)),
        "source_ref": source_ref, "evidence": proof.relative_to(ROOT.resolve()).as_posix(),
        "recorded_at": datetime.now().astimezone().isoformat(),
    }
    STATE.parent.mkdir(parents=True, exist_ok=True)
    STATE.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print(f"已记录 {version} 检查点；证据内容是否足以支持结论由执行者核实。")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["matrix", "status", "checkpoint"])
    parser.add_argument("--targets", default=None, help="逗号分隔的 Minecraft 版本，或 all")
    parser.add_argument("--check", action="store_true", help="选定次要目标未验证当前源码时失败")
    parser.add_argument("--evidence", help="checkpoint 必填；已经完成的验证记录路径")
    args = parser.parse_args()
    defaults = "all" if args.action == "status" else registry()["primary"]
    selected = select(args.targets if args.targets is not None else defaults)
    if args.action == "matrix":
        print(json.dumps({"include": selected}, separators=(",", ":")))
        return
    if args.action == "checkpoint":
        if len(selected) != 1 or not args.evidence:
            parser.error("checkpoint 需要一个次要目标以及 --evidence；验证完成后才能执行")
        checkpoint(selected[0]["minecraft"], args.evidence)
        return
    data = json.loads(STATE.read_text()) if STATE.exists() else {"targets": {}}
    source = source_fingerprint()
    pending = []
    print(f"当前主版本源码指纹：{source}")
    for target in selected:
        version = target["minecraft"]
        if version == registry()["primary"]:
            print(f"{version}: 日常开发目标（本命令不替代构建或测试）")
            continue
        saved = data["targets"].get(version, {})
        current = (saved.get("source_fingerprint") == source
                   and saved.get("target_fingerprint") == fingerprint(target_paths(version)))
        print(f"{version}: {'检查点匹配' if current else '待适配或重新验证'}；源码参照={saved.get('source_ref', '未记录')}；证据={saved.get('evidence', '无')}")
        if not current:
            pending.append(version)
    if args.check and pending:
        raise SystemExit("次要目标尚未追上当前源码：" + ", ".join(pending))


if __name__ == "__main__":
    main()
