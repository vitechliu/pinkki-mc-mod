# 2026-10-29：修正 1.21.1 整合包依赖下限

- 变更 ID：2026-10-29-neoforge-modpack-dependency-floor。
- 问题：初次1.21.1适配直接以当时下载的较新发行版设置最低版本，将构建SDK版本同时写成NeoForge运行下限。用户在“天空开拓者”整合包遇到三个加载器依赖拒绝：NeoForge 21.1.249 < 21.1.264、GeckoLib 4.9.2 < 4.9.3、JEI 19.56.0.438 < 19.70.0.462。这些截图证明版本门槛过高，不证明出现了三个运行期API冲突。
- Git 参照：`d6b904154d8c64b1241a1b1f837cd0ec1a9d224b`，基于同一工作区尚未提交的1.21.1完整移植继续修正，保留其他改动。
- 修复：按截图实际版本重新编译所有主代码及客户端适配代码。NeoForge SDK及最低运行版本为21.1.249；SDK与运行下限改为独立属性。GeckoLib编译基线/范围为4.9.2 / `[4.9.2,5)`；JEI编译基线/可选范围为19.56.0.438 / `[19.56.0.438,20)`；Curios仍为9.5.1，与用户列表一致。未将任何可选模组改为必需，未修改其他模组或整合包配置。
- 源码位置：`versions/1.21.1/{gradle.properties,build.gradle,src/main/templates/META-INF/neoforge.mods.toml}`；`scripts/verify-artifact.py`根据构建属性核对发行元数据；`scripts/check-port-1.21.1.py`支持`--neo-version`和`--installation`，独立测试多个NeoForge运行版本。
- 验收场景：本次发行包在21.1.249+4.9.2+JEI19.56.0.438+Curios9.5.1下不再因截图中的版本要求拒绝加载；独立服务端执行玩法探针、正常就绪及停服；同包在原先21.1.264+4.9.3下回归。JEI GUI不由无头服务端证明，以旧API实际符号链接检查补充静态证据。
- 主版本验证：不适用；未修改1.20.1或共享Java。本次仅处理1.21.1，不自动构建/部署主版本或1.19.1。
- 手册/Wiki：同步兼容页的安装基线、玩家更新记录、README；手册仍由同一Wiki源生成。没有新增玩法或改动配方。

## 真实验证

| 检查 | 结果 / 日志（`.local/porting-1.21.1/`下） |
| --- | --- |
| `./scripts/mod.sh 1.21.1 build portProbeJar -PportProbe --console=plain` | 以21.1.249/4.9.2/JEI19.56.0.438实际编译通过；`baseline-build.log` |
| `python3 scripts/verify-artifact.py 1.21.1` | 元数据范围、完整类/资源、Java21、客户端隔离与手册一致性通过；`baseline-artifact.log` |
| `python3 scripts/smoke-server.py 1.21.1` | 基线开发服务端加载、就绪、正常停服通过；`baseline-smoke.log` |
| `python3 scripts/check-jei-linkage.py --minecraft 1.21.1 --api <JEI19.56.0.438发行JAR>` | 7个隔离插件类、41条真实API方法/字段链接通过；`baseline-jei-linkage.log` |
| `python3 -m unittest discover -s scripts/tests` | 40项通过；`baseline-python-tests.log` |
| `npm run wiki:build` | 通过；`baseline-wiki.log`，未发布Pages |

无头发行包测试只在项目`.local/porting-1.21.1/runtime/`的新世界中运行，绑定127.0.0.1:25589、`--nogui`。21.1.249独立安装位于`.local/package-test/1.21.1-21.1.249`，21.1.264复用旧独立安装，运行器记录每个输入JAR的SHA256和正常退出结果。无需修改或升级用户的整合包。

```bash
# 基线：JEI使用自身内嵌的mezz_config 0.5.6，不额外安装较新配置库
python3 scripts/check-port-1.21.1.py \
  --geckolib .local/porting-1.21.1/downloads/geckolib-neoforge-1.21.1-4.9.2.jar \
  --extra-mod '.local/porting-1.21.1/downloads/curios-neoforge-9.5.1+1.21.1.jar' \
  --extra-mod .local/porting-1.21.1/downloads/jei-1.21.1-neoforge-19.56.0.438.jar

# 同一JAR的较新依赖回归
python3 scripts/check-port-1.21.1.py --neo-version 21.1.264 \
  --installation .local/package-test/1.21.1 \
  --geckolib .local/porting-1.21.1/downloads/geckolib-neoforge-1.21.1-4.9.3.jar \
  --extra-mod '.local/porting-1.21.1/downloads/curios-neoforge-9.5.1+1.21.1.jar'
```

## 证明范围

“天空开拓者”的名称与模组列表来自用户；本次没有完整整合包、KubeJS脚本、配置或存档，没有下载/重建该整合包。仅针对列表中直接相关的依赖组合完成独立验证，不宣称全整合包兼容或客户端验收通过。没有启动Minecraft图形客户端，没有操作其真实游戏实例、部署、提交、推送或发布。Tetra/官方匠魂无对应宿主、Silent Gear既有可选配方错误等前轮边界仍见[初次适配记录](2026-10-28-port-1.21.1.md)。

| 目标 | 状态 | 说明 |
| --- | --- | --- |
| 1.21.1 NeoForge | 已验证 | 三项依赖下限修复，较低/较新依赖的适用无头证据；完整整合包及GUI未验 |
| 1.19.1 Forge | 不适用 | 未修改此目标 |

## 最终证据

发行JAR SHA256：`50ad0570c1b2ac421cf3a18b68c95a8548323554ba620fddcfc184a5b26b5380`。

- `skyexplorer-bundled-dependencies.log`：`.local/porting-1.21.1/runtime/probe-1793203288689899000/{console.log,result.json}`，探针通过、无ERROR、就绪/正常停服、进程退出0。

- `newer-runtime-regression.log`：`.local/porting-1.21.1/runtime/probe-1793203255337579000/{console.log,result.json}`，探针通过、无ERROR、就绪/正常停服、进程退出0。

汇总证据`.local/porting-1.21.1-compatibility-verification.log`。验证后重新记录1.21.1检查点，`status --targets 1.21.1 --check`核对；保留1.19.1检查点与源码。
