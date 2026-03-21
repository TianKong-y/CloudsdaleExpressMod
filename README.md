# CloudsdaleExpress Fabric Mod

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8-green?style=for-the-badge">
  <img src="https://img.shields.io/badge/Fabric-Client%20%26%20Server-orange?style=for-the-badge">
  <img src="https://img.shields.io/badge/Version-2.0.0-blue?style=for-the-badge">
</p>

CloudsdaleExpress Fabric Mod provides handshake and compatibility support for the CloudsdaleExpress minecart system on Fabric 1.21.8.

The accompanying Paper Plugin can be found at [CloudsdaleExpress](https://github.com/TianKong-y/CloudsdaleExpress)

Compared with a server-only setup, this mod adds:
1. Handshake-based capability detection between client and server.
2. Better compatibility for enhanced minecart behavior when used with the Paper plugin.

---

## Core Features

* Server and client entrypoints for CloudsdaleExpress network initialization.
* Handshake channel support to identify mod-capable players.
* Works together with CloudsdaleExpress Paper Plugin for enhanced behavior.
* Lightweight structure suitable for dedicated servers and clients.

---

## Usage Guide

### What This Mod Does

* Registers CloudsdaleExpress networking on Fabric side.
* Participates in handshake flow so the server can apply enhanced movement rules.

### Typical Deployment

* Install this mod on Fabric client and/or Fabric server where compatibility is needed.
* Use together with the Paper plugin in mixed environments that rely on handshake behavior.

---

## Installation and Version Support

### Fabric

* Minecraft: 1.21.8
* Fabric Loader: 0.17.0 or later
* Fabric API: 0.136.1+1.21.8 or compatible
* Java: 21
* Install method: Place the mod jar into `mods/`.

---

## Build

If you want to build from source:

```bash
git clone https://github.com/TianKong-y/cloudsdaleexpress-fabric-mod.git
cd fabric-mod
./gradlew build
```

Output jar location:

* `build/libs/`

---

## Open Source License

This project is licensed under **GNU General Public License v3.0 (GPL-3.0)**.

You may use, modify, and redistribute this project under GPL-3.0 terms. Any distributed derivative work must also remain open source under GPL-3.0.

---

## Contact

* Author: TianKong_y
* GitHub: https://github.com/TianKong-y
* bilibili：[TianKong_y](https://space.bilibili.com/288309681)
* QQ：[技术交流/反馈群](https://qm.qq.com/q/m6XfOuCtVe)
