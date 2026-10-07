# Guiyuan

[![Build](https://github.com/CHS-Haple/Guiyuan/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/CHS-Haple/Guiyuan/actions/workflows/build.yml)
![Companion app: Android 13+](https://img.shields.io/badge/Companion%20app-Android%2013%2B-3DDC84?logo=android&logoColor=white)
![Modern Xposed API 102](https://img.shields.io/badge/Modern%20Xposed%20API-102-3F51B5)
[![License: GPL-3.0-only](https://img.shields.io/badge/License-GPL--3.0--only-blue.svg)](LICENSE)
![Status: pre-release](https://img.shields.io/badge/status-pre--release-orange)

**Guiyuan** is an LSPosed module for Xiaomi HyperOS that combines battery, mobile-network, and Wi-Fi information into one compact status-bar indicator.

[English](#english) | [简体中文](#简体中文)

---

## English

### Project status

> **Pre-release development.** The active pre-release line is **0.2.1**, a maintenance checkpoint over 0.2.0. The first planned formal release remains **1.0.0**; pre-1.0 versions are validated development checkpoints unless a release is explicitly published.

| Item | Current scope |
| --- | --- |
| Runtime platform | Xiaomi HyperOS |
| Verified SystemUI baseline | `17.03.260226.r` |
| Xposed interface | Modern Xposed API 102 |
| Companion app | Android 13 / API 33+ |
| Verified Guiyuan surfaces | Home status bar; opt-in lock screen / Keyguard; opt-in AOD |
| Other SystemUI surfaces | Native until separately supported and validated |

Compatibility is established against the actual target SystemUI. Other HyperOS builds or device variants may differ internally and are not assumed compatible until validated.

### Highlights

**Status-bar integration**
- Combines battery, mobile-network, and Wi-Fi information into one compact Home status indicator.
- Reacts to relevant battery, connectivity, SIM/data, airplane-mode, tint, and SystemUI state.
- Falls back to native SystemUI presentation when a replacement state cannot be established safely.

**Companion app**
- MIUIX-based Home, Features, and Settings navigation.
- Light/dark appearance, dynamic color, and standard or floating navigation options.
- English and Simplified Chinese with Android 13+ per-app language selection.
- Optional launcher-icon hiding while retaining a non-launcher app entry point.

**Diagnostics**
- General and Detailed diagnostic levels.
- Local diagnostic-report export and Android sharing.
- Explicit, user-confirmed SystemUI restart when Root access is available.
- No project-operated telemetry or resident Root service. See [PRIVACY.md](PRIVACY.md).

### Current scope

The Home status bar plus the opt-in lock-screen / Keyguard and AOD presentations are runtime-verified on the pinned target. Notification Shade remains native because this target does not expose the status-icon row there. Partial Control Center pulls use the bounded SystemUI-owned transition bridge from an eligible Home or Keyguard source; the fully expanded Control Center remains native, and AOD is not a Control Center transition source.

The project is still under active development, so wider device, system-version, and scene compatibility should not be inferred from the current verified target.

### Documentation

**Project / user-facing**
- [CHANGELOG.md](CHANGELOG.md) — unreleased net changes and future release history.
- [PRIVACY.md](PRIVACY.md) — local data, diagnostics, Root, export, and sharing.
- [SECURITY.md](SECURITY.md) — private security-reporting policy.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — direct dependencies and license notices.
- [LICENSE](LICENSE) — GNU General Public License v3.0 only (`GPL-3.0-only`).

**Development / contribution**
- [CONTRIBUTING.md](CONTRIBUTING.md) — engineering, validation, ownership, CI, and contribution rules.
- [docs/README.md](docs/README.md) — documentation map and authority guide.
- [docs/development/CURRENT.md](docs/development/CURRENT.md) — current development recovery point.
- [docs/development/ROADMAP.md](docs/development/ROADMAP.md) — future direction and 1.0.0 qualification.
- [docs/development/DECISIONS.md](docs/development/DECISIONS.md) — durable engineering decisions and rationale.
- [docs/architecture](docs/architecture) — current architecture policy and scene/layout boundaries.
- [docs/reference/systemui-contracts.md](docs/reference/systemui-contracts.md) — reusable target-SystemUI integration evidence.

### Support

If Guiyuan is useful to you and you would like to support ongoing development and maintenance, WeChat Pay and Alipay are available. See [Support Guiyuan](docs/SUPPORT.md). Support is entirely voluntary and does not affect feature access, issue priority, or the project license.

### License

Guiyuan is free software licensed under version 3 of the GNU General Public License as published by the Free Software Foundation (`GPL-3.0-only`). See [LICENSE](LICENSE).

---

## 简体中文

### 项目状态

> **预发布开发阶段。** 当前预发布版本线为 **0.2.1**，属于 0.2.0 基础上的维护检查点。计划首个正式发布版本仍为 **1.0.0**；1.0 之前的版本属于经验证的开发检查点，除非明确发布 Release。

| 项目 | 当前范围 |
| --- | --- |
| 运行平台 | Xiaomi HyperOS |
| 已验证 SystemUI 基线 | `17.03.260226.r` |
| Xposed 接口 | Modern Xposed API 102 |
| 配套应用 | Android 13 / API 33+ |
| 已验证归元场景 | 主状态栏 Home；可选锁屏 / Keyguard；可选 AOD |
| 其他 SystemUI 场景 | 在分别完成支持与验证前保持原生 |

兼容性以目标 SystemUI 的实际结构和运行表现为准。其他 HyperOS 版本或不同机型的内部实现可能不同，在完成验证前不会默认视为兼容。

### 功能概览

**状态栏集成**
- 将电池、移动网络和 Wi-Fi 信息整合为一个紧凑的 Home 状态图标。
- 跟随电池、连接、SIM/数据、飞行模式、Tint 与相关 SystemUI 状态变化。
- 无法安全建立替换状态时，保留或恢复系统原生显示。

**配套应用**
- 基于 MIUIX 的 Home、Features、Settings 导航。
- 支持亮色/深色、动态取色，以及标准或悬浮导航。
- 支持英文、简体中文和 Android 13+ 应用级语言选择。
- 可隐藏桌面图标，同时保留非桌面入口。

**诊断**
- General / Detailed 两档诊断等级。
- 本地生成诊断报告，并通过 Android 系统导出或分享。
- 具备 Root 权限时，可由用户明确确认后重启 SystemUI。
- 不包含项目运营的遥测服务，也不使用常驻 Root 服务。详见 [PRIVACY.md](PRIVACY.md)。

### 当前范围

主状态栏 Home，以及可选的锁屏 / Keyguard 与 AOD 归元显示已在当前固定目标上完成运行时验证。通知栏在该目标上不提供状态图标行，因此保持系统原生；控制中心部分下拉通过由 SystemUI 主导、以 Home 或 Keyguard 为合格来源的有界过渡桥衔接，完全展开的控制中心保持系统原生，AOD 不作为控制中心过渡来源。

项目仍处于持续开发阶段，因此不应仅根据当前验证目标推定其他机型、系统版本或场景已经兼容。

### 文档

**项目 / 用户**
- [CHANGELOG.md](CHANGELOG.md) — 当前未发布净变化与未来发布历史。
- [PRIVACY.md](PRIVACY.md) — 本地数据、诊断、Root、导出与分享说明。
- [SECURITY.md](SECURITY.md) — 安全问题私密报告规则。
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — 直接依赖与许可证说明。
- [LICENSE](LICENSE) — GNU 通用公共许可证 v3.0（仅第 3 版）（`GPL-3.0-only`）。

**开发 / 贡献**
- [CONTRIBUTING.md](CONTRIBUTING.md) — 工程、验证、所有权、CI 与贡献规范。
- [docs/README.md](docs/README.md) — 文档导航与权威关系说明。
- [docs/development/CURRENT.md](docs/development/CURRENT.md) — 当前开发恢复点。
- [docs/development/ROADMAP.md](docs/development/ROADMAP.md) — 后续方向与 1.0.0 准入条件。
- [docs/development/DECISIONS.md](docs/development/DECISIONS.md) — 长期有效的工程决策与理由。
- [docs/architecture](docs/architecture) — 当前架构策略与场景/布局边界。
- [docs/reference/systemui-contracts.md](docs/reference/systemui-contracts.md) — 可复用的目标 SystemUI 集成证据。

### 支持项目

如果归元对你有所帮助，并且你愿意支持项目的持续开发与维护，可通过微信支付或支付宝自愿支持，详见 [支持归元](docs/SUPPORT.md)。支持完全自愿，不影响功能获取、问题处理优先级或项目的开源许可。

### 许可证

归元是自由软件，按照自由软件基金会发布的 GNU 通用公共许可证第 3 版授权（`GPL-3.0-only`）。详见 [LICENSE](LICENSE)。

---

## Disclaimer / 免责声明

Guiyuan is an independent community project and is not affiliated with, endorsed by, or maintained by Xiaomi, HyperOS, LSPosed, or the MIUIX project.

归元是独立的社区项目，与 Xiaomi、HyperOS、LSPosed 或 MIUIX 项目不存在官方隶属、背书或维护关系。
