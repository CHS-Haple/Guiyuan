# Changelog · 更新日志

This file documents **formal Guiyuan releases starting with 1.0.0**. As the first stable release, the 1.0.0 entry introduces the product rather than comparing it with an earlier stable release. Earlier development, internal-test and build history remains in Git commits, pull requests and CI records, not in this public changelog.

Formal releases appear newest first, with **equivalent Simplified Chinese and English notes**. Subsequent releases use **Features**, **Improvements** and **Fixes** where applicable, adding security or compatibility notes only when needed and omitting empty categories. Build-by-Build history, abandoned experiments and unpublished candidates are not release notes.

本文件仅记录归元自 **1.0.0** 起正式发布的版本。1.0.0 为首次正式版，以下内容介绍应用能力，并非相对旧正式版的变更。此前的开发、内测与构建记录保留在 Git 提交、PR 和 CI 历史中，不列入公开版本日志。

每个正式版本按日期倒序记录，**简体中文与 English 均须完整对应**。后续版本按实际内容选用 **新功能 / Features**、**体验优化 / Improvements**、**问题修复 / Fixes**；仅在需要时增加安全或兼容性说明，省略无内容的分类。不按 Build 逐条记录，不收录失败实验，也不将未发布候选写作已发布版本。

## [1.1.0] - 2026-11-09

### English

#### Features

- **More flexible indicator layouts:** adds centered and two-sided signal-dot arrangements with independently retained single- and dual-SIM layout preferences; extends charging and network-activity placement choices while keeping native-state priority.
- **Configurable top-bar blur:** introduces an enabled-by-default progressive blur option shared by OS3/OS4 **visual styles** and extends the corresponding Preview Sandbox top-bar presentation.

#### Improvements

- **MIUIX/HyperOS 4 appearance:** refreshes source-matched Glass materials, navigation behavior, popup/menu presentation, display-cutout handling and iconography against the verified upstream snapshot.
- **Settings and diagnostics:** improves the battery color editor's nested dismissal, Preview Sandbox scroll-to-top behavior, and diagnostic log presentation.

#### Fixes

- **Network presentation:** reconciles activity-indicator visibility with observed native changes, including VPN handoffs, rather than treating an icon refresh as a loss of network state.
- **Runtime handoffs:** tightens Hot Reload transfer typing and owner cleanup while retaining native fallback when a host contract cannot be established.

Compatibility remains scoped to Xiaomi HyperOS 4 with a compatible Modern Xposed API 102 environment. The OS3 option is a visual style, **not** HyperOS 3 runtime support.

### 简体中文

#### 新功能

- **更灵活的图标布局：** 新增信号点居中与双侧分布，并分别记忆单卡、双卡布局设置；扩展充电闪电与网络活动指示器的放置方式，同时遵循原生状态优先级。
- **可配置的顶栏渐进模糊：** 新增默认开启的顶栏渐进模糊选项，适用于 OS3/OS4 **界面样式**，并同步完善预览沙盒的顶栏显示。

#### 体验优化

- **MIUIX/HyperOS 4 外观：** 依据已核验的上游版本，调整 Glass 材质、导航交互、菜单与弹窗表现、屏幕挖孔适配及图标资源。
- **设置与诊断：** 改进电量颜色编辑器的多层退出处理、预览沙盒回到顶部表现，以及诊断日志展示。

#### 问题修复

- **网络状态显示：** 优化网络活动指示器对原生可见性变化的跟随，包括 VPN 切换过程，避免把图标刷新误判为网络状态丢失。
- **运行状态切换：** 收紧热重载数据交接类型与资源释放处理；无法验证宿主契约时仍保留原生回退。

兼容范围仍为配备兼容 Modern Xposed API 102 环境的小米 HyperOS 4。OS3 选项仅代表界面样式，**不表示支持 HyperOS 3 系统运行**。

## [1.0.0] - 2026-11-02

### English

#### Features

- **Unified status indicator:** combines battery, cellular and Wi-Fi information in one compact icon, with battery-centered and network-centered layouts and configurable colors, size, position and font weight.
- **SIM and network states:** supports single SIM, conditional dual-SIM and optional dual-row signals when native subscription identities are verified, including Wi-Fi, hotspot, airplane mode and no-SIM conditions.
- **Native-aware scenes:** presents the combined indicator on verified Home, Keyguard and AOD surfaces. Control Center handoffs follow native SystemUI presentation and transitions; fully expanded Control Center remains SystemUI-owned.
- **Settings and live preview:** offers MIUIX-style settings, feature search, a preview sandbox, battery color schemes and customizable floating navigation.
- **Configuration and maintenance:** supports settings backup/restore, on-demand diagnostics and log export, plus API 102 Hot Reload in compatible framework environments.
- **First-run guide:** covers language selection, scope and permission guidance, Terms of Use and Privacy Policy acknowledgment. Root is optional for ordinary settings and requested only for relevant diagnostic or system-maintenance actions.

#### Improvements

- Supports light and dark themes, theme-based colors and selectable OS3/OS4 **visual styles**, with source-qualified MIUIX glass integration for HyperOS 4.
- Preserves native SystemUI presentation where host icons, SIM identities or layout contracts cannot be established reliably, rather than forcing an unsafe replacement.

#### Compatibility

- **Officially supports Xiaomi HyperOS 4**, with an LSPosed environment compatible with Modern Xposed API 102. The OS3 visual style does **not** imply HyperOS 3 runtime support.
- The device-tested baseline is **Xiaomi 15 Pro**, HyperOS `4.0.0.19.XOBCNXM`, SystemUI `17.03.260226.r`. Other HyperOS 4 devices, system builds, third-party fonts and framework combinations are not all individually certified.

### 简体中文

#### 新功能

- **一体化状态栏显示**：将电量、移动网络和 Wi-Fi 信息组合为紧凑的图标，提供电量居中和网络居中等布局，以及颜色、尺寸、位置、字重等外观设置。
- **多卡与网络状态**：在能够确认系统数据和卡槽身份时支持单卡、双卡及可选双排信号样式，并处理 Wi-Fi、热点、飞行模式、无 SIM 卡等状态。
- **多场景适配**：在已验证的桌面状态栏、锁屏与息屏场景显示归元图标；控制中心展开时遵循系统原生图标及过渡逻辑，完整展开的控制中心由 SystemUI 管理。
- **设置与实时预览**：提供 MIUIX 风格的设置、功能搜索、预览沙盒、电池配色方案，以及可自定义的浮动导航。
- **配置与维护**：支持配置备份与恢复、按需诊断和日志导出，并在兼容的框架环境中提供 API 102 热重载。
- **首次使用引导**：提供语言选择、作用域及权限说明、使用条款和隐私政策确认；日常设置不强制要求 Root，特定诊断与系统维护操作按需授权。

#### 体验优化

- 界面支持深浅色、主题配色，以及可选的 OS3/OS4 **视觉风格**；HyperOS 4 相关玻璃控件采用项目已验证的 MIUIX 适配。
- 对无法可靠识别的原生图标、卡槽或宿主布局，尽可能保持 SystemUI 原生显示，而不是强制接管。

#### 兼容性说明

- **正式支持 Xiaomi HyperOS 4**，需要兼容 Modern Xposed API 102 的 LSPosed 环境；应用内的 OS3 视觉风格不代表支持 HyperOS 3。
- 已实机验证的基线为 **Xiaomi 15 Pro**、HyperOS `4.0.0.19.XOBCNXM`、SystemUI `17.03.260226.r`。其他 HyperOS 4 机型、系统版本、第三方字体和框架组合未全部独立验证。
