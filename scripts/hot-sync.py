#!/usr/bin/env python3
"""同步 1.20.1 HMCL 资源/数据覆盖包，不编译 Java、不替换 mod JAR、不启动游戏。"""
import argparse
import fcntl
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tempfile

spec = importlib.util.spec_from_file_location("deploy_local", Path(__file__).with_name("deploy-local.py"))
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)
hmcl = deploy.hmcl
PACK_NAME = "pinkkicore-dev-overlay"
MANIFEST = ".pinkkicore-hot-sync.json"
OWNER = "pinkkicore-hot-sync-v1"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(data):
    return (json.dumps(data, ensure_ascii=False, indent=2) + "\n").encode()


def checked(path, parent):
    # Reject links even if they resolve inside the project: never follow a managed tree alias.
    hmcl.safe_file(path, parent)
    for part in [path, *path.parents]:
        if part.is_symlink():
            raise ValueError(f"不处理符号链接：{part}")
        if part == hmcl.ROOT:
            break
    return path


def relative_file(name):
    path = PurePosixPath(name)
    if (not name or path.is_absolute() or ".." in path.parts or "\\" in name
            or path.as_posix() != name or name == MANIFEST):
        raise ValueError(f"受管清单包含非法文件路径：{name}")
    return name


def source_files(kind):
    props = dict(line.split("=", 1) for line in (hmcl.ROOT / "versions/1.20.1/gradle.properties")
                 .read_text().splitlines() if "=" in line and not line.startswith("#"))
    if props.get("pack_format") != "15":
        raise ValueError("目标资源格式已变化，请先复核 1.20.1 热同步配置")
    files = {"pack.mcmeta": json_bytes({"pack": {"pack_format": 15,
             "description": "PinkkiCore 开发覆盖（正式 JAR 验收前请停用）"}})}
    # Same precedence as Gradle: shared resources first, version resources override them.
    for root in [hmcl.ROOT / "common/src/main/resources", hmcl.ROOT / "versions/1.20.1/src/main/resources"]:
        base = checked(root / kind, hmcl.ROOT)
        if not base.exists():
            continue
        for path in sorted(base.rglob("*")):
            checked(path, base)
            if path.is_file():
                data = path.read_bytes()
                if path.suffix == ".json":
                    json.loads(data)  # Validate all JSON before mutating any selected pack.
                files[path.relative_to(root).as_posix()] = data
    return files


def pack_state(pack):
    checked(pack, hmcl.GAME_ROOT)
    if not pack.exists():
        return None
    marker = checked(pack / MANIFEST, pack)
    if not marker.is_file():
        raise ValueError(f"目录非本工具创建，拒绝接管：{pack}")
    state = json.loads(marker.read_text())
    if state.get("owner") != OWNER or state.get("minecraft") != "1.20.1" or not isinstance(state.get("files"), dict):
        raise ValueError(f"受管清单无效：{marker}")
    expected = state["files"]
    for name, expected_hash in expected.items():
        path = checked(pack / relative_file(name), pack)
        if not path.is_file() or sha(path.read_bytes()) != expected_hash:
            raise ValueError(f"受管文件已被手动修改或删除，拒绝覆盖/移除：{path}")
    for path in pack.rglob("*"):
        checked(path, pack)
        if path.is_file() and path != marker and path.relative_to(pack).as_posix() not in expected:
            raise ValueError(f"受管包中有额外文件，请先移出：{path}")
    return state


def world_path(target, name):
    if not name or name in (".", "..") or "/" in name or "\\" in name:
        raise ValueError("--world 必须是该实例 saves 下的单个世界文件夹名")
    world = checked(target / "saves" / name, target)
    if not checked(world / "level.dat", world).is_file():
        raise ValueError(f"测试世界不存在或缺少 level.dat：{world}；不会创建世界")
    return world


def existing_worlds(target):
    saves = checked(target / "saves", target)
    if not saves.exists():
        return []
    worlds = []
    for path in sorted(saves.iterdir()):
        # Linked worlds may belong to another installation; automatic discovery excludes them.
        if path.is_symlink() or not path.is_dir():
            continue
        checked(path, saves)
        if checked(path / "level.dat", path).is_file():
            worlds.append(path)
    return worlds


def plan(profiles, world=None, data_only=False, resources_only=False):
    if world is not None and len(profiles) != 1:
        raise ValueError("数据包同步必须通过 --profile 明确指定一个实例")
    if resources_only and (world is not None or data_only):
        raise ValueError("--resources-only 不能与 --world 或 --data-only 同用")
    packs = []
    for profile in profiles:
        target, _ = deploy.check_instance(profile)
        if not data_only:
            packs.append((profile, "assets", checked(target / "resourcepacks" / PACK_NAME, target)))
        if not resources_only:
            worlds = [world_path(target, world)] if world is not None else existing_worlds(target)
            for selected in worlds:
                packs.append((profile, "data", checked(selected / "datapacks" / PACK_NAME, target)))
    plans = [(profile, kind, pack, pack_state(pack)) for profile, kind, pack in packs]
    for _, kind, pack, previous in plans:
        if previous and previous.get("kind") != kind:
            raise ValueError(f"覆盖包种类不匹配：{pack}")
    return plans


def atomic_write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, suffix=".tmp", delete=False) as handle:
        temporary = Path(handle.name)
    try:
        temporary.write_bytes(data)
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def apply_pack(pack, kind, previous, files, run, index):
    # Back up only this owned pack, never an entire world or third-party resource pack.
    if previous:
        shutil.copytree(pack, run / "backups" / str(index))
    new_hashes = {name: sha(data) for name, data in files.items()}
    old = previous["files"] if previous else {}
    changed = [name for name in files if new_hashes[name] != old.get(name)]
    deleted = sorted(set(old) - set(files))
    for name in changed:
        atomic_write(checked(pack / name, pack), files[name])
    for name in deleted:
        checked(pack / name, pack).unlink()
    for path in sorted(pack.rglob("*"), key=lambda p: len(p.parts), reverse=True):
        if path.is_dir() and not any(path.iterdir()):
            path.rmdir()
    state = {"owner": OWNER, "minecraft": "1.20.1", "kind": kind,
             "synced_at": hmcl.stamp(), "files": new_hashes}
    atomic_write(pack / MANIFEST, json_bytes(state))
    # Read back hashes, also detecting accidental extra files before reporting success.
    pack_state(pack)
    return {"changed": changed, "deleted": deleted, "file_count": len(files)}


def run(action, profiles, world=None, data_only=False, resources_only=False):
    run_dir = checked(hmcl.LOCAL / "hot-sync" / hmcl.stamp(), hmcl.ROOT)
    run_dir.mkdir(parents=True)
    receipt = {"action": action, "minecraft": "1.20.1", "world": world,
               "world_scope": "none" if resources_only else ("selected" if world is not None else "all"),
               "status": "started", "packs": []}
    try:
        plans = plan(profiles, world, data_only, resources_only)
        sources = {kind: source_files(kind) for kind in sorted({p[1] for p in plans})} if action == "sync" else {}
        for index, (profile, kind, pack, previous) in enumerate(plans):
            details = {"profile": profile, "kind": kind, "path": str(pack.relative_to(hmcl.ROOT))}
            if kind == "data":
                details["world"] = pack.parent.parent.name
            if action == "sync":
                details.update(apply_pack(pack, kind, previous, sources[kind], run_dir, index))
            elif action == "remove" and previous:
                shutil.copytree(pack, run_dir / "backups" / str(index))
                shutil.rmtree(pack)
                details["removed"] = True
            else:
                details["present"] = previous is not None
                details["file_count"] = len(previous["files"]) if previous else 0
            receipt["packs"].append(details)
            print(f"{profile} / {kind}: {pack}")
        receipt["status"] = "ok"
    except Exception as error:
        receipt.update(status="failed", error=str(error))
        raise
    finally:
        atomic_write(run_dir / "receipt.json", json_bytes(receipt))
        print(f"热同步记录：{run_dir}")
    if action == "sync":
        if not data_only:
            print(f"首次请在游戏资源包界面启用 {PACK_NAME}，放到最高优先级；以后同步完成按 F3+T。")
        if any(p[1] == "data" for p in plans):
            print(f'已同步测试世界的数据包；请在当前世界执行 /reload；首次用 /datapack list 检查，若未启用则执行 /datapack enable "file/{PACK_NAME}" last。')
    elif action == "remove":
        print("已移除选定受管覆盖包。请手动检查资源包/数据包列表并重载；正式 JAR 验收前确认不再启用覆盖包。")
    print("未编译 Java、未替换 mod JAR、未启动游戏；本命令不自动在游戏内重载。")
    return receipt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", nargs="?", choices=["sync", "remove", "status", "list-worlds"], default="sync")
    parser.add_argument("--profile", choices=["both", "clean", "compat"], default="both")
    parser.add_argument("--world", help="仅处理此测试世界（需指定单个实例）；默认处理所有已有世界")
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--data-only", action="store_true", help="只处理数据包，默认所有已有测试世界")
    modes.add_argument("--resources-only", action="store_true", help="只处理客户端资源包")
    args = parser.parse_args()
    profiles = ["clean", "compat"] if args.profile == "both" else [args.profile]
    try:
        hmcl.LOCAL.mkdir(parents=True, exist_ok=True)
        with checked(hmcl.LOCAL / "deploy.lock", hmcl.ROOT).open("a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            if args.action == "list-worlds":
                if args.world is not None or args.data_only or args.resources_only:
                    raise ValueError("list-worlds 不接受世界或资源类型筛选参数")
                for profile in profiles:
                    target, _ = deploy.check_instance(profile)
                    worlds = [p.name for p in existing_worlds(target)]
                    print(f"{profile}: {json.dumps(worlds, ensure_ascii=False)}")
            else:
                run(args.action, profiles, args.world, args.data_only, args.resources_only)
    except (OSError, ValueError, subprocess.SubprocessError) as error:
        parser.exit(1, f"热同步失败：{error}\n")


if __name__ == "__main__":
    main()
