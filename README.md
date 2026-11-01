<div align="center">

# 归元 · Guiyuan

**为 HyperOS 4 打造的一体化状态栏图标**  
*A unified battery, cellular and Wi-Fi indicator for HyperOS 4.*

[![Release](https://img.shields.io/github/v/release/CHS-Haple/Guiyuan?label=release)](https://github.com/CHS-Haple/Guiyuan/releases)
[![Build](https://github.com/CHS-Haple/Guiyuan/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/CHS-Haple/Guiyuan/actions/workflows/build.yml)
![HyperOS 4](https://img.shields.io/badge/HyperOS-4%20only-0078D4)
![Modern Xposed API](https://img.shields.io/badge/Modern%20Xposed%20API-102-5965BB)
[![GPL-3.0-only](https://img.shields.io/badge/license-GPL--3.0--only-238636)](LICENSE)

[简体中文](#简体中文) · [English](#english) · [Releases](https://github.com/CHS-Haple/Guiyuan/releases) · [Issues](https://github.com/CHS-Haple/Guiyuan/issues)

</div>

---

## 简体中文

归元是面向 **Xiaomi HyperOS 4** 的开源 LSPosed 模块，将电池电量、移动网络和 Wi-Fi 信息整合为紧凑的状态栏显示，提供统一的设置与预览体验。

### 功能

- **一体化状态栏**：融合电量、蜂窝与 Wi-Fi 状态，提供可调整的布局、颜色、字重与位置。
- **多场景衔接**：适配已验证的桌面状态栏、锁屏和息屏显示，控制中心展开时遵循 SystemUI 原生显示与过渡行为。
- **双卡与网络状态**：支持有条件的双排信号、热点、飞行模式和多种网络状态；无法确认宿主结构时优先保持原生显示。
- **配置与维护**：提供 MIUIX 设置界面、预览沙盒、配置备份与恢复、按需诊断及热重载。

### 支持范围

| 项目 | 要求与说明 |
| --- | --- |
| **对外支持的系统** | **Xiaomi HyperOS 4**；不支持将 HyperOS 3 或其他 Android 系统视为正式适配目标 |
| 模块框架 | 兼容 Modern Xposed API 102 的 LSPosed 环境；其他框架未经验证 |
| 已实机验证的基线 | Xiaomi 15 Pro，HyperOS `OS4.0.0.19.XOBCNXM`，SystemUI `17.03.260226.r` |
| 作用域 | SystemUI；如需在系统设置中启用快捷入口，按需授权设置作用域 |
| Root | 配套应用的常规设置不强制 Root；部分诊断与系统进程维护需要 Root |

**系统支持范围不等于全机型兼容承诺。** HyperOS 4 不同机型、ROM 与 SystemUI 内部结构可能存在差异；未经验证的宿主或场景会尽量保持系统原生行为。应用中的“OS3”选项仅代表可选的**界面视觉风格**，不表示支持运行在 HyperOS 3 上。

### 安装与使用

1. 从 [Releases](https://github.com/CHS-Haple/Guiyuan/releases) 获取正式签名 APK，正常安装或覆盖安装。
2. 在兼容的 LSPosed 管理器中启用归元，并为模块配置 **SystemUI** 作用域。
3. 按框架要求重新启动相关系统进程，打开归元完成首次引导、协议确认与必要设置。
4. 调整布局和外观。需要诊断、日志导出或重启 SystemUI 时，再按需授予 Root。

首次升级、更新系统或修改作用域前，建议备份归元配置。如遇 SystemUI 异常，请在框架管理器中停用模块并恢复系统原生状态。

### 反馈与安全

普通故障、兼容性问题和功能建议请使用 [Issues](https://github.com/CHS-Haple/Guiyuan/issues)。反馈前请核对设备、HyperOS 与 SystemUI 版本，并移除日志中的敏感信息。安全漏洞请使用 GitHub 的[私密漏洞报告入口](https://github.com/CHS-Haple/Guiyuan/security/advisories/new)，不要在公开 Issue 中披露。

[更新日志](CHANGELOG.md) · [使用条款](TERMS.md) · [隐私政策](PRIVACY.md) · [安全政策](SECURITY.md) · [支持项目](docs/SUPPORT.md)

---

## English

Guiyuan is an open-source LSPosed module for **Xiaomi HyperOS 4**, combining battery, cellular and Wi-Fi status into one compact indicator with a configurable companion app.

### Features

- **Unified status indicator:** battery, cellular and Wi-Fi presentation, with configurable layout, colors, weight and positioning.
- **Native-aware scenes:** verified Home, optional Keyguard and AOD integration, with SystemUI-owned Control Center handoffs and safe native fallback.
- **Connectivity and dual SIM:** optional dual-row presentation and network-state handling when trustworthy native information is available.
- **Configuration and diagnostics:** MIUIX settings, preview sandbox, backup/restore, on-demand diagnostics and Hot Reload.

### Compatibility

| Area | Policy |
| --- | --- |
| **Supported OS family** | **Xiaomi HyperOS 4 only** |
| Module framework | LSPosed with Modern Xposed API 102; alternative frameworks are not verified |
| Device-tested baseline | Xiaomi 15 Pro, HyperOS `OS4.0.0.19.XOBCNXM`, SystemUI `17.03.260226.r` |
| Module scope | SystemUI; Settings is optional for its integration shortcut |
| Root | Not mandatory for ordinary companion-app settings; some diagnostics and process-maintenance actions require Root |

**HyperOS 4 support does not mean every HyperOS 4 device or SystemUI build has been tested.** Unsupported or ambiguous hosts should retain native presentation. The in-app “OS3” option is a **visual style**, not an assertion of HyperOS 3 runtime compatibility.

### Installation

1. Download the signed APK from [Releases](https://github.com/CHS-Haple/Guiyuan/releases).
2. Enable Guiyuan in a compatible LSPosed manager and authorize the **SystemUI** scope.
3. Restart the relevant system process as required by your framework, then complete the first-run guide.
4. Configure the indicator in the companion app; grant Root only for optional maintenance operations.

Back up your settings before major upgrades. If SystemUI becomes unstable, disable the module in the framework manager to restore native behavior.

### Community and documentation

Report ordinary bugs or requests through [Issues](https://github.com/CHS-Haple/Guiyuan/issues). Keep private logs and security details out of public discussions; use [private vulnerability reporting](https://github.com/CHS-Haple/Guiyuan/security/advisories/new) for security concerns.

[Changelog](CHANGELOG.md) · [Terms](TERMS.md) · [Privacy](PRIVACY.md) · [Security](SECURITY.md) · [Contributing](CONTRIBUTING.md) · [Developer docs](docs/README.md) · [Third-party notices](THIRD_PARTY_NOTICES.md)

---

<sub>归元是独立开源项目，不隶属于 Xiaomi、LSPosed 或 MIUIX，也不代表这些项目的官方立场。 Guiyuan is an independent community project and is not affiliated with or endorsed by Xiaomi, LSPosed or MIUIX.</sub>

**License:** [GPL-3.0-only](LICENSE). Third-party dependencies and incorporated assets retain their respective notices and licenses.
