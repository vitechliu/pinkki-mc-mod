#!/usr/bin/env python3
"""准备 HMCL 项目测试目录、同步发行 mod、归档诊断日志；绝不启动游戏。"""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parent.parent
LOCAL = ROOT / ".local/hmcl"
GAME_ROOT = LOCAL / ".minecraft"
TARGETS = {"1.20.1": "forge", "1.21.1": "neoforge", "1.19.1": "forge"}


def properties():
    return dict(line.split("=", 1) for line in (ROOT / "mod.properties").read_text().splitlines()
                if "=" in line and not line.startswith("#"))


def stamp():
    return datetime.now().astimezone().strftime("%Y%m%d-%H%M%S-%f")


def artifact(mc):
    props = properties()
    return ROOT / "versions" / mc / "build/libs" / f"{props['mod_id']}-{TARGETS[mc]}-{mc}-{props['mod_version']}.jar"


def safe_file(path, parent):
    if (path.is_symlink() or not path.resolve().is_relative_to(parent.resolve())
            or not path.resolve().is_relative_to(ROOT.resolve())):
        raise ValueError(f"不处理符号链接或目录外文件：{path}")
    return path


def instance(mc, profile):
    path = GAME_ROOT / "versions" / f"PinkkiCore-{mc}-{profile}"
    safe_file(path, GAME_ROOT)
    descriptor = safe_file(path / f"{path.name}.json", path)
    if not descriptor.is_file():
        raise ValueError(f"尚未安装此实例。请先在 HMCL 中创建 {path.name}，启用各实例独立。预期文件：{descriptor}")
    return path


def is_our_mod(jar):
    try:
        with zipfile.ZipFile(jar) as archive:
            for name in ["META-INF/mods.toml", "META-INF/neoforge.mods.toml"]:
                if name in archive.namelist():
                    data = tomllib.loads(archive.read(name).decode())
                    if any(mod.get("modId") == properties()["mod_id"] for mod in data.get("mods", [])):
                        return True
    except (zipfile.BadZipFile, UnicodeDecodeError, tomllib.TOMLDecodeError):
        pass
    return False


def sync(mc, profile):
    target = instance(mc, profile)
    mods = safe_file(target / "mods", target)
    mods.mkdir(exist_ok=True)
    subprocess.run([sys.executable, str(ROOT / "scripts/verify-artifact.py"), mc], check=True)
    source = artifact(mc)
    old = [safe_file(p, target) for p in mods.glob("*.jar") if is_our_mod(safe_file(p, target))]
    destination = safe_file(mods / source.name, target)
    if destination.exists() and destination not in old:
        raise ValueError(f"同名文件不属于当前 mod，拒绝覆盖：{destination}")
    if old:
        backup = safe_file(LOCAL / "backups" / f"{target.name}-{stamp()}", ROOT)
        backup.mkdir(parents=True)
        for jar in old:
            shutil.copy2(jar, backup / jar.name)
        print(f"原 PinkkiCore 已备份：{backup}")
    with tempfile.NamedTemporaryFile(dir=mods, suffix=".tmp", delete=False) as handle:
        temporary = Path(handle.name)
    try:
        shutil.copy2(source, temporary)
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)
    for jar in old:
        if jar != destination:
            jar.unlink()
    print(f"已同步：{destination}\n第三方 mod、配置和存档均保留。请手动启动 HMCL 测试。")


def report(mc, profile, note):
    target = instance(mc, profile)
    output = safe_file(LOCAL / "reports" / f"{target.name}-{stamp()}", ROOT)
    files = [target / "logs/latest.log", target / "logs/debug.log"]
    crash_dir = safe_file(target / "crash-reports", target)
    crashes = [safe_file(p, target) for p in crash_dir.glob("*.txt")]
    files += sorted(crashes, key=lambda p: p.stat().st_mtime, reverse=True)[:3]
    files += sorted(target.glob("hs_err_pid*.log"), key=lambda p: p.stat().st_mtime, reverse=True)[:3]
    output.mkdir(parents=True)
    copied = []
    for path in files:
        safe_file(path, target)
        if path.is_file():
            relative = path.relative_to(target)
            destination = output / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, destination)
            copied.append(str(relative))
    mods = []
    for jar in sorted(safe_file(target / "mods", target).glob("*.jar")):
        safe_file(jar, target)
        mods.append({"file": jar.name, "sha256": hashlib.sha256(jar.read_bytes()).hexdigest()})
    (output / "report.json").write_text(json.dumps({
        "created_at": datetime.now().astimezone().isoformat(), "minecraft": mc,
        "loader": TARGETS[mc], "instance": target.name, "note": note,
        "copied_logs": copied, "mods": mods,
        "notice": "目录名称代表约定目标；实际加载器和版本以本次游戏日志为准。未收集账号配置、存档或完整模组。",
    }, ensure_ascii=False, indent=2) + "\n")
    print(f"日志与 mod 清单已归档：{output}")
    if not copied:
        print("未发现游戏日志；若在启动游戏前失败，请将 HMCL 导出的启动失败日志保存到此目录。")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["init", "sync", "report"])
    parser.add_argument("minecraft", nargs="?", choices=list(TARGETS), default="1.20.1")
    parser.add_argument("profile", nargs="?", choices=["clean", "compat"], default="compat")
    parser.add_argument("--note", default="", help="复现步骤、预期与实际现象")
    args = parser.parse_args()
    if args.action == "init":
        for path in [GAME_ROOT, LOCAL / "reports", LOCAL / "backups", LOCAL / "launcher-logs"]:
            safe_file(path, ROOT)
            path.mkdir(parents=True, exist_ok=True)
        print(f"HMCL 游戏目录：{GAME_ROOT}\n请手动添加目录、安装实例并启用各实例独立。未启动 HMCL 或游戏。")
    elif args.action == "sync":
        sync(args.minecraft, args.profile)
    else:
        report(args.minecraft, args.profile, args.note)


if __name__ == "__main__":
    main()
