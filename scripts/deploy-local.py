#!/usr/bin/env python3
"""构建、校验并安装到项目 HMCL 实例；不启动启动器或游戏。Python 3.11+。"""
import argparse
import importlib.util
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import tomllib
from urllib.request import urlopen
import zipfile

spec = importlib.util.spec_from_file_location("hmcl_test", Path(__file__).with_name("hmcl-test.py"))
hmcl = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hmcl)


def digest(path, algorithm="sha256"):
    return hmcl.hashlib.new(algorithm, path.read_bytes()).hexdigest()


def numeric(version):
    if not re.fullmatch(r"\d+(\.\d+)*", version):
        raise ValueError(f"无法确认版本兼容性：{version}")
    return tuple(int(part) for part in version.split("."))


def mods_metadata(path):
    with zipfile.ZipFile(path) as archive:
        if "META-INF/mods.toml" not in archive.namelist():
            return {}
        return tomllib.loads(archive.read("META-INF/mods.toml").decode())


def check_instance(profile):
    target = hmcl.instance("1.20.1", profile)
    descriptor = json.loads((target / f"{target.name}.json").read_text())
    patches = {p["id"]: p.get("version") for p in descriptor.get("patches", [])}
    if (patches.get("game") != "1.20.1" or "forge" not in patches
            or not (numeric("47.1.0") <= numeric(patches["forge"]) < numeric("48"))
            or descriptor.get("javaVersion", {}).get("majorVersion") != 17):
        raise ValueError(f"{target.name} 必须为 Minecraft 1.20.1 / Forge [47.1.0,48) / Java 17")
    settings_path = hmcl.safe_file(target / ".hmcl/config/instance-game-settings.json", target)
    settings = json.loads(settings_path.read_text())
    overrides = set(settings.get("overrideProperties", []))
    # HMCL 3.16.3: overriding runningDirectory with an empty value selects the instance root.
    run_dir = settings.get("runningDirectory", "")
    if "runningDirectory" not in overrides or (run_dir and Path(run_dir).resolve() != target.resolve()):
        raise ValueError(f"请在 HMCL 为 {target.name} 启用各实例独立")
    if not {"javaType", "customJavaPath"}.issubset(overrides) or settings.get("javaType") != "CUSTOM":
        raise ValueError(f"请为 {target.name} 明确选择自定义 Java 17")
    java = Path(settings.get("customJavaPath", ""))
    result = subprocess.run([str(java), "-XshowSettings:properties", "-version"],
                            capture_output=True, text=True, check=True, timeout=20)
    info = result.stdout + result.stderr
    version = re.search(r"java.version\s*=\s*(\S+)", info)
    arch = re.search(r"os.arch\s*=\s*(\S+)", info)
    if not version or not version[1].startswith("17."):
        raise ValueError(f"{target.name} 实际 Java 不是 17")
    return target, {"instance": target.name, "minecraft": patches["game"], "forge": patches["forge"],
                    "java_version": version[1], "java_arch": arch[1] if arch else "unknown",
                    "isolated": True}


def ensure_stopped(targets):
    # Never print process command lines: launch arguments may contain account credentials.
    processes = subprocess.run(["ps", "-axo", "command="], capture_output=True, text=True, check=True)
    for command in processes.stdout.splitlines():
        if "--gameDir" in command and any(str(t) in command for t in targets):
            raise ValueError("目标游戏仍在运行，请先手动正常退出，再部署")


def dependency_plan(target, minimum):
    mods = hmcl.safe_file(target / "mods", target)
    found = []
    for jar in sorted(mods.glob("*.jar")):
        hmcl.safe_file(jar, target)
        data = mods_metadata(jar)
        for mod in data.get("mods", []):
            if mod.get("modId") == "geckolib":
                version = str(mod.get("version", ""))
                if not minimum <= numeric(version) < numeric("5"):
                    raise ValueError(f"{target.name} 的 {jar.name} 不满足 GeckoLib 要求，请手动处理")
                # A Fabric/NeoForge or wrong-Minecraft JAR must not satisfy the prerequisite.
                deps = data.get("dependencies", {}).get("geckolib", [])
                mc_ranges = [d.get("versionRange", "") for d in deps if d.get("modId") == "minecraft"]
                if not any(d.get("modId") == "forge" for d in deps) or not any(
                        value in ("[1.20.1,)", "[1.20.1]", "[1.20.1,1.21)", "[1.20.1,1.20.2)")
                        for value in mc_ranges):
                    raise ValueError(f"无法确认 {jar.name} 支持 Forge 1.20.1，请手动处理")
                found.append(jar)
    if len(found) > 1:
        raise ValueError(f"{target.name} 有多份 GeckoLib，请先去重")
    return found[0] if found else None


def runtime_jar(config):
    cache = hmcl.safe_file(hmcl.LOCAL / "runtime-cache", hmcl.ROOT)
    cache.mkdir(parents=True, exist_ok=True)
    destination = hmcl.safe_file(cache / config["filename"], cache)
    if destination.exists():
        if digest(destination, "sha512") != config["sha512"]:
            raise ValueError(f"前置缓存校验失败：{destination}")
        return destination
    # Reuse only the original Maven artifact, never a ForgeGradle mapped/deobfuscated JAR.
    candidates = (hmcl.ROOT / ".local/gradle/caches/modules-2/files-2.1/software.bernie.geckolib"
                  / "geckolib-forge-1.20.1" / config["version"]).glob(f"*/{config['filename']}")
    with tempfile.NamedTemporaryFile(dir=cache, suffix=".tmp", delete=False) as handle:
        temporary = Path(handle.name)
    try:
        source = next((p for p in candidates if digest(p, "sha512") == config["sha512"]), None)
        if source:
            shutil.copy2(source, temporary)
        else:
            with urlopen(config["url"], timeout=60) as response, temporary.open("wb") as output:
                shutil.copyfileobj(response, output)
        if digest(temporary, "sha512") != config["sha512"]:
            raise ValueError("下载的前置 SHA512 不匹配，拒绝安装")
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)
    return destination


def install(source, target):
    destination = hmcl.safe_file(target / "mods" / source.name, target)
    destination.parent.mkdir(exist_ok=True)
    if destination.exists():
        raise ValueError(f"前置目标文件已存在，拒绝覆盖：{destination}")
    with tempfile.NamedTemporaryFile(dir=destination.parent, suffix=".tmp", delete=False) as handle:
        temporary = Path(handle.name)
    try:
        shutil.copy2(source, temporary)
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)
    return destination


def curios_plan(target):
    found = []
    for jar in sorted((target / "mods").glob("*.jar")):
        hmcl.safe_file(jar, target)
        data = mods_metadata(jar)
        for mod in data.get("mods", []):
            if mod.get("modId") == "curios":
                version = str(mod.get("version", ""))
                if version == "${file.jarVersion}":
                    with zipfile.ZipFile(jar) as archive:
                        manifest = archive.read("META-INF/MANIFEST.MF").decode()
                    match = re.search(r"^Implementation-Version: (.+)$", manifest, re.M)
                    version = match[1].strip() if match else ""
                if not numeric("5.14.1") <= numeric(version.split("+")[0]) < numeric("6"):
                    raise ValueError(f"{target.name} 的 Curios 不满足 [5.14.1,6)")
                deps = data.get("dependencies", {}).get("curios", [])
                if not any(d.get("modId") == "forge" for d in deps) or not any(
                        d.get("modId") == "minecraft" and d.get("versionRange") in
                        ("[1.20,1.21)", "[1.20,1.20.1]", "[1.20.1]", "[1.20.1,1.21)", "[1.20.1,1.20.2)") for d in deps):
                    raise ValueError(f"无法确认 {jar.name} 支持 Forge 1.20.1")
                found.append(jar)
    if len(found) > 1:
        raise ValueError(f"{target.name} 有多份 Curios，请先去重")
    return found[0] if found else None


def deploy(profiles, with_curios=False):
    output = hmcl.safe_file(hmcl.LOCAL / "deployments" / hmcl.stamp(), hmcl.ROOT)
    output.mkdir(parents=True)
    receipt = {"status": "started", "minecraft": "1.20.1", "instances": [], "started_at": hmcl.stamp()}
    try:
        checked = [check_instance(p) for p in profiles]
        targets = [target for target, _ in checked]
        ensure_stopped(targets)
        configs = json.loads((hmcl.ROOT / "config/hmcl-runtime.json").read_text())["1.20.1"]
        props = dict(line.split("=", 1) for line in (hmcl.ROOT / "versions/1.20.1/gradle.properties")
                     .read_text().splitlines() if "=" in line and not line.startswith("#"))
        if props["geckolib_version_range"] != f"[{configs['geckolib']['version']},5)":
            raise ValueError("GeckoLib 构建需求已变化，请先更新 config/hmcl-runtime.json")
        dependencies = [dependency_plan(t, numeric(configs["geckolib"]["version"])) for t in targets]
        curios_dependencies = [curios_plan(t) for t in targets] if with_curios else []
        if with_curios:
            for target, existing in zip(targets, curios_dependencies):
                if existing is None and (target / "mods" / configs["curios"]["filename"]).exists():
                    raise ValueError(f"{target.name} 的 Curios 文件名冲突")
        source = hmcl.artifact("1.20.1")
        # Check destination conflicts for every selected instance before any installation.
        for target, existing in zip(targets, dependencies):
            destination = hmcl.safe_file(target / "mods" / source.name, target)
            if destination.exists() and not hmcl.is_our_mod(destination):
                raise ValueError(f"同名文件不属于 PinkkiCore：{destination}")
            dep_dest = hmcl.safe_file(target / "mods" / configs["geckolib"]["filename"], target)
            if existing is None and dep_dest.exists():
                raise ValueError(f"前置文件名冲突：{dep_dest}")
        print(f"检查通过，构建日志：{output / 'build.log'}", flush=True)
        with (output / "build.log").open("w") as log:
            subprocess.run([str(hmcl.ROOT / "scripts/mod.sh"), "1.20.1", "build"],
                           cwd=hmcl.ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
            subprocess.run([sys.executable, str(hmcl.ROOT / "scripts/verify-artifact.py"), "1.20.1"],
                           cwd=hmcl.ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
        dep_source = runtime_jar(configs["geckolib"]) if None in dependencies else None
        curios_source = runtime_jar(configs["curios"]) if with_curios and None in curios_dependencies else None
        ensure_stopped(targets)
        receipt["artifact_sha256"] = digest(source)
        for profile, (target, details), existing in zip(profiles, checked, dependencies):
            if with_curios:
                curios = curios_dependencies[targets.index(target)] or install(curios_source, target)
                details.update(curios=curios.name, curios_sha256=digest(curios))
            dependency = existing or install(dep_source, target)
            hmcl.sync("1.20.1", profile, verified=True)
            installed = target / "mods" / source.name
            if digest(installed) != receipt["artifact_sha256"]:
                raise ValueError(f"安装后校验失败：{installed}")
            details.update({"mod": installed.name, "sha256": digest(installed),
                            "geckolib": dependency.name, "geckolib_sha256": digest(dependency)})
            overlay = "pinkkicore-dev-overlay/.pinkkicore-hot-sync.json"
            prepared = [target / "resourcepacks" / overlay]
            prepared += list((target / "saves").glob(f"*/datapacks/{overlay}"))
            details["prepared_hot_sync_overlays"] = [str(p.relative_to(target)) for p in prepared if p.is_file()]
            if details["prepared_hot_sync_overlays"]:
                print(f"注意：{target.name} 留有开发覆盖包；如已启用，可能遮住发行 JAR 的资源。正式验收前请用 hot-sync.py remove 移除并手动重载。")
            receipt["instances"].append(details)
        receipt["status"] = "installed"
    except Exception as error:
        receipt.update(status="failed", error=str(error))
        raise
    finally:
        (output / "receipt.json").write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + "\n")
        print(f"部署记录：{output}", flush=True)
    print("已安装到选定实例；未启动游戏。请在 HMCL 手动启动验收。")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=["both", "clean", "compat"], default="both")
    parser.add_argument("--with-curios", action="store_true", help="为选定实例补齐可选 Curios 饰品前置，保留满足版本的现有文件")
    args = parser.parse_args()
    # Prevent two concurrent deploy commands from competing over backup/removal.
    import fcntl
    hmcl.LOCAL.mkdir(parents=True, exist_ok=True)
    lock_path = hmcl.safe_file(hmcl.LOCAL / "deploy.lock", hmcl.ROOT)
    with lock_path.open("w") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            deploy(["clean", "compat"] if args.profile == "both" else [args.profile], args.with_curios)
        except (OSError, ValueError, subprocess.CalledProcessError) as error:
            parser.exit(1, f"部署失败：{error}\n")


if __name__ == "__main__":
    main()
