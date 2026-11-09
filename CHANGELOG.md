# Changelog · 更新日志

This file documents **formal Guiyuan releases starting with 1.0.0**. As the
first stable release, the 1.0.0 entry introduces the product rather than
comparing it with an earlier stable release. Earlier development, internal-test
and build history remains in Git commits, pull requests and CI records, not in
this public changelog.

Formal releases appear newest first, with **equivalent Simplified Chinese and
English notes**. Subsequent releases group user-facing changes under **Added,
Improved, Fixed, Removed, and Upstream dependency updates** when applicable;
omit empty categories and add concise compatibility notes where needed.
Build-by-Build history, abandoned experiments and unpublished candidates are not
release notes.

本文件仅记录归元自 **1.0.0** 起正式发布的版本。1.0.0
为首次正式版，以下内容介绍应用能力，并非相对旧正式版的变更。此前的开发、内测与构建记录保留在 Git 提交、PR 和 CI 历史中，不列入公开版本日志。

每个正式版本按日期倒序记录，**简体中文与 English 均须完整对应**。后续版本以用户感知为主，按实际内容选用 **新增 / Added**、**优化 /
Improved**、**修复 / Fixed**、**移除 / Removed**、**上游依赖更新 / Upstream dependency
updates**；省略空分类，必要时补充简明兼容性说明。不按 Build 逐条记录，不收录失败实验，也不将未发布候选写作已发布版本。

## [1.1.0] - 2026-11-09

### 简体中文

本次更新汇总 **1.0.0 → 1.1.0** 的正式版本变化。

#### 新增

- **信号强度居中布局：** 可将移动信号点放在图标中央、网络制式置于下方，支持单卡与双卡场景。
- **双卡双侧分布：** 新增两侧排列的信号点样式，保留原有弧形、横向分布；首次启用双卡布局时默认使用双侧分布。
- **底部状态图标：** 网络上/下行指示器和充电闪电可显示在信号点底部，支持大小与位置调整；同时存在时优先显示充电闪电。
- **网络活动显示选项：** 新增“空闲时隐藏网络活动指示器”开关，默认关闭，支持在预览沙盒中查看效果。
- **顶栏渐进模糊：** 新增默认开启的独立开关，适配 OS3/OS4 界面风格及预览沙盒顶栏。
- **电量环动态避让：** 新增随信号点位置和大小变化的开口方式，同时保留固定开口选项。
- **正式版详细诊断：** Release 与 Canary 均可按需开启详细日志，日常默认保持普通诊断级别。

#### 优化

- **布局与配置记忆：** 各布局、单排及双排信号点分别记忆样式、大小和偏移，切换后不再相互覆盖。
- **图标与过渡动画：** 优化双卡信号点间距、电量环避让，以及展开控制中心时的图标收拢与恢复效果。
- **HyperOS 4 界面：** 按 MIUIX 上游规范更新 Glass 材质、菜单、弹层、导航胶囊、图标和屏幕挖孔适配。
- **预览沙盒：** 优化顶栏、小栏、渐进模糊与滚动交互，改善返回顶部时的阴影状态。
- **关于页：** 优化动态背景与滚动效果，随页面可见状态和主题变化正确管理动画。
- **设置与诊断：** 优化中英文搜索、配色设置、日志列表与详情展示，以及配置备份和恢复体验。
- **使用说明：** 完善中英文隐私政策、使用条款、第三方依赖说明和问题反馈模板。

#### 修复

- 修复部分机型电量百分比及充电状态更新不及时的问题。
- 修复部分双卡场景信号点缺失，以及 SIM2 作为上网卡时网络活动指示器显示异常的问题。
- 改善 VPN 开关及 Wi-Fi/蜂窝网络切换时网络活动指示器的异常隐藏、显示与空白占位。
- 修复飞行模式退出及热重载过程中可能短暂显示旧网络状态的问题。
- 修复部分场景下状态栏图标深浅色反转延迟或颜色不一致的问题。
- 修复电量配色二级弹层退出时偶发闪烁，以及菜单返回、退场过程中的交互异常。
- 修复关于页滚动后顶部图形遮挡内容、返回顶部后阴影残留等界面问题。
- 修复旧版双卡配置恢复后覆盖当前布局设置的部分兼容问题。
- 修复首次使用引导中权限状态尚未确认时错误显示警告图标的问题。

#### 移除

- 移除不再使用的本地 Glass 图标替代资源，改用匹配当前 MIUIX 版本的官方图标。
- 停用历史 IT 内测构建渠道；正式版与 Canary 沿用现有发布方式。

#### 上游依赖更新

- **MIUIX：** `0.9.4-8972f941-SNAPSHOT` → `0.9.4-45645854-SNAPSHOT`，同步更新 Glass 组件和官方图标资源。
- **Gradle Wrapper：** `9.8.2` → `9.9.0`。
- **Android Gradle Plugin：** `9.5.0` → `9.5.1`。
- **GitHub Actions setup-java：** `6.1.0` → `6.2.0`。

**兼容性：** 继续面向 **Xiaomi HyperOS 4** 与兼容 **Modern Xposed API 102** 的 LSPosed 环境。OS3/OS4 是应用内界面风格选项，不代表支持在 HyperOS 3 系统运行。libxposed API/Service 版本保持 `102.0.0`。

### English

This release summarizes the user-facing changes from **1.0.0 to 1.1.0**.

#### Added

- **Centered cellular signal layout:** place cellular signal dots in the center
  with the network type below, supporting single- and dual-SIM presentations.
- **Two-sided dual-SIM layout:** adds a two-sided signal-dot arrangement
  alongside curved and horizontal styles, and uses it by default when first
  enabling a dual-SIM layout.
- **Bottom status icons:** place native network upload/download indicators or
  the charging bolt below the signal dots, with size and position controls;
  charging takes priority when both apply.
- **Network activity option:** optionally hide the activity indicator when idle
  (off by default), with Preview Sandbox support.
- **Progressive top-bar blur:** an on-by-default switch for the supported
  OS3/OS4 visual styles and Preview Sandbox top bar.
- **Adaptive battery-ring opening:** an opening that follows signal-dot
  placement and size, while retaining the fixed-opening alternative.
- **Detailed diagnostics in Release:** opt in to detailed logs in Release or
  Canary; regular diagnostics remain the default.

#### Improved

- **Layout preferences:** keep signal-dot style, size and offsets separately for
  layouts and single-/dual-row modes.
- **Visual layout and transitions:** refine dual-SIM spacing, ring clearance and
  signal-dot transitions when entering or leaving Control Center.
- **HyperOS 4 interface:** align Glass materials, menus, sheets, navigation
  capsules, icons and display cutouts with upstream MIUIX conventions.
- **Preview Sandbox:** improve top-bar, dock, progressive blur and scrolling
  behavior, including scroll-to-top shadow handling.
- **About screen:** refine the animated background and scrolling, respecting
  visibility and theme changes.
- **Settings and diagnostics:** improve bilingual search, battery color
  controls, log lists and details, backup and restore.
- **User information:** update bilingual privacy and terms documents,
  third-party notices and issue-report templates.

#### Fixed

- Battery percentage or charging state not refreshing promptly on some devices.
- Missing signal dots on some dual-SIM hosts, and incorrect network activity
  when SIM2 is the default data SIM.
- Abnormal activity-indicator visibility and empty space during VPN and
  Wi-Fi/cellular transitions.
- Stale network state briefly appearing after airplane-mode transitions or Hot
  Reload.
- Delayed or inconsistent light/dark icon tint on some screens.
- Occasional flashing when dismissing nested battery-color sheets, plus menu
  Back/exit interaction issues.
- About-screen header covering content after scrolling and lingering shadow
  after scroll-to-top.
- Legacy dual-SIM settings sometimes overriding newer per-layout preferences
  after restore.
- Premature warning icons for permissions not yet resolved in first-run setup.

#### Removed

- Unused local Glass icon substitutes, in favor of official icons matching the
  pinned MIUIX revision.
- Retired historical IT internal-test build channel; Release and Canary use the
  established distribution paths.

#### Upstream dependency updates

- **MIUIX:** `0.9.4-8972f941-SNAPSHOT` → `0.9.4-45645854-SNAPSHOT`, including refreshed Glass components and official icons.
- **Gradle Wrapper:** `9.8.2` → `9.9.0`.
- **Android Gradle Plugin:** `9.5.0` → `9.5.1`.
- **GitHub Actions setup-java:** `6.1.0` → `6.2.0`.

**Compatibility:** Guiyuan continues to target **Xiaomi HyperOS 4** with an LSPosed environment compatible with **Modern Xposed API 102**. OS3/OS4 are in-app visual styles, not HyperOS 3 runtime support. libxposed API/Service remains at `102.0.0`.

## [1.0.0] - 2026-11-02

### English

#### Features

- **Unified status indicator:** combines battery, cellular and Wi-Fi information
  in one compact icon, with battery-centered and network-centered layouts and
  configurable colors, size, position and font weight.
- **SIM and network states:** supports single SIM, conditional dual-SIM and
  optional dual-row signals when native subscription identities are verified,
  including Wi-Fi, hotspot, airplane mode and no-SIM conditions.
- **Native-aware scenes:** presents the combined indicator on verified Home,
  Keyguard and AOD surfaces. Control Center handoffs follow native SystemUI
  presentation and transitions; fully expanded Control Center remains
  SystemUI-owned.
- **Settings and live preview:** offers MIUIX-style settings, feature search, a
  preview sandbox, battery color schemes and customizable floating navigation.
- **Configuration and maintenance:** supports settings backup/restore, on-demand
  diagnostics and log export, plus API 102 Hot Reload in compatible framework
  environments.
- **First-run guide:** covers language selection, scope and permission guidance,
  Terms of Use and Privacy Policy acknowledgment. Root is optional for ordinary
  settings and requested only for relevant diagnostic or system-maintenance
  actions.

#### Improvements

- Supports light and dark themes, theme-based colors and selectable OS3/OS4
  **visual styles**, with source-qualified MIUIX glass integration for HyperOS
  4.
- Preserves native SystemUI presentation where host icons, SIM identities or
  layout contracts cannot be established reliably, rather than forcing an unsafe
  replacement.

#### Compatibility

- **Officially supports Xiaomi HyperOS 4**, with an LSPosed environment
  compatible with Modern Xposed API 102. The OS3 visual style does **not** imply
  HyperOS 3 runtime support.
- The device-tested baseline is **Xiaomi 15 Pro**, HyperOS `4.0.0.19.XOBCNXM`, SystemUI `17.03.260226.r`. Other HyperOS 4 devices, system builds, third-party fonts and framework combinations are not all individually certified.

### 简体中文

#### 新功能

- **一体化状态栏显示**：将电量、移动网络和 Wi-Fi 信息组合为紧凑的图标，提供电量居中和网络居中等布局，以及颜色、尺寸、位置、字重等外观设置。
- **多卡与网络状态**：在能够确认系统数据和卡槽身份时支持单卡、双卡及可选双排信号样式，并处理 Wi-Fi、热点、飞行模式、无 SIM 卡等状态。
- **多场景适配**：在已验证的桌面状态栏、锁屏与息屏场景显示归元图标；控制中心展开时遵循系统原生图标及过渡逻辑，完整展开的控制中心由 SystemUI
  管理。
- **设置与实时预览**：提供 MIUIX 风格的设置、功能搜索、预览沙盒、电池配色方案，以及可自定义的浮动导航。
- **配置与维护**：支持配置备份与恢复、按需诊断和日志导出，并在兼容的框架环境中提供 API 102 热重载。
- **首次使用引导**：提供语言选择、作用域及权限说明、使用条款和隐私政策确认；日常设置不强制要求 Root，特定诊断与系统维护操作按需授权。

#### 体验优化

- 界面支持深浅色、主题配色，以及可选的 OS3/OS4 **视觉风格**；HyperOS 4 相关玻璃控件采用项目已验证的 MIUIX 适配。
- 对无法可靠识别的原生图标、卡槽或宿主布局，尽可能保持 SystemUI 原生显示，而不是强制接管。

#### 兼容性说明

- **正式支持 Xiaomi HyperOS 4**，需要兼容 Modern Xposed API 102 的 LSPosed 环境；应用内的 OS3
  视觉风格不代表支持 HyperOS 3。
- 已实机验证的基线为 **Xiaomi 15 Pro**、HyperOS `4.0.0.19.XOBCNXM`、SystemUI `17.03.260226.r`。其他 HyperOS 4 机型、系统版本、第三方字体和框架组合未全部独立验证。
