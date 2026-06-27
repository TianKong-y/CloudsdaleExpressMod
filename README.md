# CloudsdaleExpress Fabric Mod

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8~26.2-green?style=for-the-badge">
  <img src="https://img.shields.io/badge/Fabric-客户端%20%26%20服务端-orange?style=for-the-badge">
  <img src="https://img.shields.io/badge/许可证-GPL--3.0-blue?style=for-the-badge">
</p>

CloudsdaleExpress Fabric Mod 为 Fabric 提供高速矿车物理与客户端/服务端能力检测支持。

配套的 Paper 插件请参阅 [CloudsdaleExpress](https://github.com/TianKong-y/CloudsdaleExpress)

与仅服务端方案相比，本 Mod 额外提供：
1. 基于握手的客户端/服务端能力检测。
2. 与 Paper 插件配合使用时，为增强矿车行为提供更好的兼容性。

---

## 核心功能

* 在平滑石英轨道上将矿车速度上限提升至 `1.75`。
* 矿车离开高速轨道后平滑减速。
* 支持普通矿车、运输矿车、漏斗矿车、动力矿车、TNT 矿车和命令方块矿车。
* 与 Fabric 服务端通过原生 Fabric Payload 进行握手。
* 当 Fabric 通道不可用时，回退到 Paper 插件命令握手。
* 支持独立服务器与单人集成服务器运行。

---

## 使用指南

### 本 Mod 的功能

* 在 Fabric 端注册 CloudsdaleExpress 网络通道。
* 参与握手流程，使服务端能够应用增强的运动规则。
* 在平滑石英轨道上应用高速矿车物理。

### 典型部署方式

* 在需要兼容性的 Fabric 客户端和/或 Fabric 服务端安装本 Mod。
* 在依赖握手行为的混合环境中，与 Paper 插件配合使用。

---

## 安装与版本支持

### Fabric

| Minecraft | Java | Fabric API | 映射 / 工具链 |
|---|---:|---|---|
| 1.21.8 | 21 | 0.136.1+1.21.8 | Yarn / Loom 1.15 |
| 1.21.9 | 21 | 0.134.1+1.21.9 | Yarn / Loom 1.15 |
| 1.21.10 | 21 | 0.138.4+1.21.10 | Yarn / Loom 1.15 |
| 1.21.11 | 21 | 0.141.4+1.21.11 | Yarn / Loom 1.15 |
| 26.1 | 25 | 0.145.1+26.1 | Unobfuscated / Loom 1.17 |
| 26.2 | 25 | 0.153.0+26.2 | Unobfuscated / Loom 1.17 |

* 每个发布 jar 仅对应一个 Minecraft 版本。
* 将对应版本的 jar、Fabric Loader 和 Fabric API 放入 `mods/` 即可。

---

## 构建

如需从源码构建：

构建单个版本（默认为 1.21.8）：

```powershell
.\gradlew.bat build -Ptarget_version=1.21.8
```

构建并验证全部六个版本：

```powershell
$env:JAVA21_HOME = 'C:\path\to\jdk-21'
$env:JAVA25_HOME = 'C:\path\to\jdk-25'
.\scripts\Build-AllVersions.ps1 -Prefetch
```

输出 jar 位置：

* `dist/`

`Prefetch-Minecraft.ps1` 优先使用 BMCLAPI 下载，对失败的分段回退到 Mojang 源，并使用版本元数据中的 SHA-1 校验每个下载的游戏 jar。Gradle Wrapper 使用华为云 Gradle 镜像。

---

## 单人游戏回归测试

冒烟测试会创建一个包含全部六种矿车变体的世界，通过 Quick Play 以真实单人世界加载，等待 Fabric 握手完成，并检查日志中是否出现实体/Mixin 崩溃：

```powershell
.\scripts\Smoke-Test-Singleplayer.ps1 -Version 1.21.8
```

---

## 开源许可

本项目基于 **GNU 通用公共许可证 v3.0 (GPL-3.0)** 开源。

你可以在 GPL-3.0 条款下使用、修改和重新分发本项目。任何分发的衍生作品也必须在 GPL-3.0 下保持开源。

---

## 联系方式

* 作者：TianKong_y
* GitHub：https://github.com/TianKong-y
* bilibili：[TianKong_y](https://space.bilibili.com/288309681)
* QQ：[技术交流/反馈群](https://qm.qq.com/q/m6XfOuCtVe)
