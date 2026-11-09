# Changelog · 更新日志

This file documents **formal Guiyuan releases starting with 1.0.0**. As the first stable release, the 1.0.0 entry introduces the product rather than comparing it with an earlier stable release. Earlier development, internal-test and build history remains in Git commits, pull requests and CI records, not in this public changelog.

Formal releases appear newest first, with **equivalent Simplified Chinese and English notes**. Subsequent releases use **Features**, **Improvements** and **Fixes** where applicable, adding security or compatibility notes only when needed and omitting empty categories. Build-by-Build history, abandoned experiments and unpublished candidates are not release notes.

本文件仅记录归元自 **1.0.0** 起正式发布的版本。1.0.0 为首次正式版，以下内容介绍应用能力，并非相对旧正式版的变更。此前的开发、内测与构建记录保留在 Git 提交、PR 和 CI 历史中，不列入公开版本日志。

每个正式版本按日期倒序记录，**简体中文与 English 均须完整对应**。后续版本按实际内容选用 **新功能 / Features**、**体验优化 / Improvements**、**问题修复 / Fixes**；仅在需要时增加安全或兼容性说明，省略无内容的分类。不按 Build 逐条记录，不收录失败实验，也不将未发布候选写作已发布版本。

## [1.1.0] - 2026-11-09

### 简体中文

本版本汇总自 **1.0.0 正式版（2026-11-02）** 之后、截至 **1.1.0（B1822）** 的已合入改动。以下按功能与问题归类，不将中间测试包、未合入实验或未经证实的兼容性结果当作新功能。

#### 新功能与布局

- **信号强度居中：** 增加以移动信号点为中心的布局，下方显示网络制式；与电量数字居中等既有内容布局协调，支持经原生卡槽识别后的单卡/双卡呈现。
- **布局设置独立记忆：** 各内容布局独立保留其信号点样式、尺寸、上下偏移等视觉配置；单排与双排排列互不覆盖，并兼容历史单/双卡配置。
- **双侧分布：** 双卡信号点新增双侧分布，保留弧形与横向布局选择。首次选择双卡分布时优先使用双侧样式；底部有图标与无图标时分别计算光学间距，减少信号点相挤或留白不均。
- **电量环开口与避让：** 增加按实际信号点坐标、尺寸、圆角与电量环线宽计算的随位置开口模式，并保留固定开口选择；弧形与横向分布分别计算有效避让。
- **底部状态信息：** 双侧信号点之间可放置由原生状态驱动的 Wi-Fi/移动网络活动图标，或将原生充电闪电移至底部；两者同时满足条件时充电闪电优先，不绘制图标时不保留无意义空槽。
- **网络活动选项：** 完善上/下行图标显示、大小、上下偏移和预览状态；新增“隐藏空闲时网络活动指示器”选项（默认关闭），并统一使用双三角形设置图标。网络活动以 OEM 实际资源和可见性为准，不根据连通类型自行推测流量。
- **信号过渡几何：** 调整双侧分布向系统原生移动信号/控制中心呈现的收拢、展开和形状变化；仅在确认为聚合双卡时使用对应的双点中间位置，避免将独立双卡误当聚合状态。

#### 原生状态与问题修复

- **部分机型的电量刷新：** 将电量百分比与充电状态的观察点对齐到原生电量控件实际注册的回调入口，继续复用已有单一状态写入通路，不新增平行广播轮询；不同机型仍以实机兼容性为准。
- **部分双卡机型的信号缺失：** 当 Home 原生移动图标已存在但缺少 Binder 关联记录时，限定在当前已验证的 OEM Home 图标根节点读取卡槽身份，避免错误丢失已有信号；有明确 Binder 记录时仍以其为准。
- **聚合双卡的活动图标：** 区分“默认上网 SIM”与“实际承载原生活动图标的可见根节点”。针对 SIM2 上网、SIM1 负责显示的聚合场景，改为跟随真正绘制资源的原生根节点。
- **VPN、Wi-Fi 与蜂窝切换：** 优化网络活动图标对 OEM 实际资源、根节点可见性及子图标可见性的跟随；避免仅凭 VPN/默认传输切换或残留资源 ID 推断图标应当消失、出现，减少底部空白占位。未经过实机覆盖的 OEM 时序不作完全同步承诺。
- **飞行模式与热重载：** 修正跨代状态合并时移动网络恢复等待标记可能被旧快照覆盖的问题，避免飞行模式关闭后的恢复阶段提前显示旧信号或网络制式。
- **深浅色自动反色：** 调整为优先采用当前场景原生电量控件已应用的前景颜色；邻近状态图标颜色仅用于尚未收到原生值的初始化阶段，不再持续覆盖当前场景颜色。充电语义色与文字/信号前景色继续独立处理。
- **热重载数据与异常边界：** 对跨代传递的版本标记、颜色等字段实行更严格的实际类型检查；完善原生 Hook、会话与清理中的异常传播/退役边界，避免错误输入被解释成有效状态。

#### MIUIX、Glass 与界面交互

- **上游 Glass 全链路对齐：** 更新经验证的 MIUIX Glass 材质、叠层、顶栏按钮、菜单形变与导航实现；保留归元已有且必要的宿主、手势和生命周期适配，不额外设计独立动画。
- **锚定菜单与返回：** 调整设置菜单、诊断筛选和电量配色菜单的原生锚点、根级弹窗宿主、级联子菜单及 Back/退场流程；修复外部手势被取消时仍误关闭菜单的问题。
- **电量配色 Sheet：** 修复二级颜色编辑器退出期间过早释放父级手势/弹层锁，及子页面尚未真正卸载就清理请求导致的闪烁；等待原生内容结束生命周期后再完成清理。
- **顶栏渐进模糊：** 在已适配的 OS4 页面引入上游渐进模糊，并新增默认开启的设置项，在 OS3/OS4 *界面样式* 下按各自上游机制控制；同步适配预览沙盒顶栏及其小栏/底部扩展范围。
- **导航栏、图标与挖孔：** 对齐导航胶囊拖动/选择动画、图标尺寸与触摸区域；更新 Glass/Material Symbols 图标，并恢复 MIUIX 原生屏幕挖孔处理。未有实际调用方的可选尾随导航动作不计作已上线功能。
- **预览沙盒：** 调整顶部标题、渐进模糊、小栏与页面滚动的联动；修正点击“回到顶部”后 Glass 阴影可能残留的问题，同时保持原生标题折叠行为，不引入自行设计的补间动画。
- **关于页与动态背景：** 修复关于页滚动后 Logo/前景覆盖列表的问题；背景特效改用 MIUIX 上游效果节点与动画生命周期，由归元轻量适配页面可见性、滚动透明度与主题变化。

#### 设置、诊断与配置可靠性

- **首次使用引导：** 未连接、未授权或仍在检测的权限项目显示真实文字状态，不再一律出现误导性的红色感叹号；Root 仍非普通设置的强制条件。
- **Release 详细诊断：** 正式版与 Canary 均允许按需开启详细级别，默认仍为普通诊断；将高开销的深层视图/渲染探针与日常详细记录分离，并改进当前 SystemUI 会话和 PID 的状态确认，避免将未生效误报为已生效。
- **日志采集与展示：** 优先从实际 SystemUI 进程中选择诊断事件，避免较新的 Settings 进程日志掩盖信号问题线索；改进日志卡片、展开详情、摘要和状态提示的组件组织。
- **设置备份与旧配置迁移：** 整理文件导入/导出与恢复事务，修正历史双卡配置、信号点排列和按布局记忆之间的读取优先级；保留兼容字段、既有备份格式及非法数值校验，避免旧配置覆盖新布局的有效值。
- **设置与依赖信息：** 改善外观预览、配色方案页面及中英文搜索的组件组织和匹配开销；“第三方依赖”按运行时、编译期、构建/测试等实际用途分类，展示与真实构建一致的 MIUIX/libxposed 版本。
- **政策及反馈：** 更新中英双语隐私政策、使用条款、第三方许可说明和 Issue 模板，明确权限、Root、诊断数据、风险与反馈范围；不新增应用自身的网络权限或遥测。

#### 性能、测试与构建

- **减少重复工作：** 降低原生网络活动值不变时的状态快照/集合复制；简化诊断历史会话扫描、设置搜索预处理及备份的重复读取。
- **内部质量整理：** 对原生呈现会话、控制中心退场、AOD 边界、绘制几何、配置迁移、日志、配色和界面组件做按职责拆分，收紧异常边界，补充对应回归测试；未把所有静态分析告警宣称已清零。
- **上游依赖：** MIUIX 对齐已验收的 `45645854` 源码及 `0.9.4-45645854-SNAPSHOT` 工件；Gradle Wrapper 升至 **9.9.0**，Android Gradle Plugin 使用 **9.5.1**，`actions/setup-java` 固定为 **6.2.0**。libxposed API/Service 仍按应用实际依赖保持 **102.0.0**。
- **发布与签名保护：** 改进精确 Git SHA、Build 身份、官方 Maven/源码一致性、Xposed 元数据及隔离签名验证；停止已退役 IT 渠道的常规构建/签名入口。正式版与 Canary 使用受保护的签名流程，开发/测试工具链不作为应用运行时功能宣传。

#### 兼容性与验证范围

- 正式支持范围仍是 **Xiaomi HyperOS 4** 与兼容 **Modern Xposed API 102** 的 LSPosed 环境；OS3/OS4 仅表示可选择的应用界面风格，**不表示支持在 HyperOS 3 系统运行**。
- **B1822** 已在维护者目标设备上安装并反馈无异常，且对应源码通过完整构建和签名验证；这不意味着所有 HyperOS 4 设备、GPU、双卡状态、屏幕方向、第三方主题或异常时序都已逐一验证。
- 尚未合入的上游实验、临时诊断代码及仍在跟进的非阻断 Codacy 问题不计入已完成更新。

### English

This entry covers changes **merged since the stable 1.0.0 release (2026-11-02)** through **1.1.0 (B1822)**. Items are grouped by behavior rather than interim test builds; unmerged experiments and unverified compatibility claims are excluded.

#### Features and layouts

- **Centered cellular signal:** adds a content layout with cellular signal dots in the center and network type underneath, with native-identity-aware single-/dual-SIM presentation alongside existing battery-centered layouts.
- **Per-layout visual memory:** retains the applicable signal-dot arrangement, size and vertical offset separately for content layouts and for single-/dual-row choices, including legacy configuration migration.
- **Two-sided dual-SIM dots:** adds the two-sided arrangement while retaining curved/horizontal choices. Newly selected dual-SIM layouts prefer two-sided placement, with optical spacing computed differently when a bottom icon is actually drawable versus absent.
- **Battery-ring opening:** introduces an opening that follows actual dot coordinates, sizes, rounded edges and ring stroke, with a fixed-opening alternative; curved and horizontal layouts use their respective geometry.
- **Shared bottom information slot:** places OEM-backed Wi-Fi/cellular activity or a relocated native charging bolt between two-sided dots. Charging takes priority, and no bottom gap remains when no qualifying icon is drawable.
- **Activity indicator controls:** improves native in/out indication, size, vertical offset and Preview Sandbox states; adds an optional **Hide network activity indicator when idle** switch (off by default) and unified two-triangle setting icon. Activity is derived from OEM resources and visibility, not guessed from connectivity type.
- **Signal-to-native transitions:** adjusts staged contraction, expansion and shape changes between two-sided dots and native mobile/Control Center presentation. The paired intermediate position applies only to verified aggregated dual-SIM layouts.

#### Native-state and bug fixes

- **Battery refresh on some hosts:** observes battery percentage and charging through the native meter's registered callback entry, retaining one state writer rather than adding a second broadcast/polling source. Individual device behavior still requires qualification.
- **Missing Home signal on some dual-SIM hosts:** when an eligible native mobile root is present but its Binder mapping receipt is absent, uses the verified current OEM Home root to recover the SIM identity; an available Binder receipt remains authoritative.
- **Aggregated SIM activity:** separates the default-data SIM from the native root that actually presents the mobile activity glyph, including the SIM2-data/SIM1-visible-root case.
- **VPN and transport transitions:** follows original resource and View visibility during VPN, Wi-Fi and cellular handoffs; avoids treating a stale resource ID or a change of default transport alone as visibility authority and avoids blank bottom reservations. Unqualified OEM frame timing is not promised.
- **Airplane mode and Hot Reload:** retains the pending mobile recovery gate correctly when new and transferred states merge, preventing stale signal/network-type presentation during recovery.
- **Automatic light/dark tint:** favors the native foreground applied to the current scene's battery view. Neighboring icon tint remains a provisional bootstrap only and cannot continuously override a known native value; charging semantic color stays separate from text/signal foreground.
- **Hot Reload safety:** validates transferred version and tint field types more strictly and tightens exceptional hook/session/retirement cleanup without inventing parallel state owners.

#### MIUIX, Glass and interactions

- **Source-matched Glass integration:** refreshes upstream-aligned Glass materials, overlays, top-bar actions, popup transforms and navigation while retaining Guiyuan's necessary host/gesture/lifecycle adapters.
- **Anchored and cascading menus:** adjusts native popup anchoring, root hosting, submenu/Back exit and the battery-scheme and diagnostics menus; canceled outside gestures no longer count as confirmed dismissal.
- **Nested battery-color sheets:** retains the parent overlay lock until the child has genuinely completed its exit and disposal, addressing flicker caused by premature dismissal/cleanup.
- **Progressive top-bar blur:** adopts the upstream OS4 progressive material and adds an on-by-default option that controls the appropriate implementation in each OS3/OS4 **visual style**. The Preview Sandbox top bar and its measured dock/band extension are aligned accordingly.
- **Navigation, icons and cutouts:** aligns capsule drag/selection motion, icon sizing and touch geometry; refreshes Glass/Material Symbols assets and restores upstream display-cutout handling. A trailing navigation action with no production caller is **not** advertised as a shipped feature.
- **Preview Sandbox:** improves top-bar, blur, dock and scrolling interaction; reconciles lingering Glass shadow when using scroll-to-top while retaining native title-collapse behavior.
- **About and animated background:** prevents the scrolling About hero from covering list content; moves background shader/preset/animation ownership to the MIUIX effect node, with lightweight Guiyuan gating for page visibility, scroll transparency and theme changes.

#### Settings, diagnostics and configuration

- **Onboarding status:** shows plain-text status for unavailable, ungranted or checking permissions instead of a misleading red warning icon. Root remains optional for ordinary settings.
- **Detailed diagnostics in Release:** enables opt-in Detailed diagnostics in Release and Canary while retaining General by default. Expensive deep visual/render probes remain independently disabled, and effective status is checked against the live SystemUI session/PID rather than reported optimistically.
- **Log selection and display:** prioritizes actual SystemUI diagnostic events instead of letting newer Settings-process logs obscure signal evidence; improves the organization of log cards, expanded details, summaries and state hints.
- **Backup, restore and migration:** separates archive file I/O from restore transactions, corrects precedence among historical dual-SIM, dot-style and per-layout preference keys, and retains legacy archive fields and input validation rather than overwriting newer valid choices.
- **Settings and third-party catalog:** reorganizes appearance previews and battery color pages while reducing bilingual search and other repeated work. Third-party dependencies are classified as runtime, compile-time or build/test, with display identities grounded in actual MIUIX/libxposed build inputs.
- **Policies and feedback:** updates bilingual Privacy/Terms/attribution and issue templates to clarify permissions, optional Root, local diagnostics, risks and reporting. No new app INTERNET permission or telemetry is introduced.

#### Performance, test coverage and build tooling

- **Lower redundant work:** avoids repeated snapshot/map copies for unchanged native network activity, redundant diagnostic session rescans, repeated search preprocessing and backup reads.
- **Maintainability and tests:** separates presentation/session ownership, Control Center retirement, AOD boundaries, geometry, migration, logs and UI responsibilities, with focused regressions. The remaining static-analysis backlog is **not** represented as fully resolved.
- **Dependencies:** updates to accepted MIUIX revision `45645854` and published `0.9.4-45645854-SNAPSHOT`; Gradle Wrapper **9.9.0**, Android Gradle Plugin **9.5.1**, and SHA-pinned `actions/setup-java` **6.2.0**. Actual libxposed API/Service dependencies remain **102.0.0**.
- **Release security:** strengthens exact-Git-SHA/Build identity, source-versus-Maven checks, Xposed metadata and isolated signing. The retired IT channel no longer feeds ordinary build/signing entrypoints.

#### Compatibility and validation limits

- Supported runtime remains **Xiaomi HyperOS 4** with an LSPosed environment compatible with **Modern Xposed API 102**. OS3/OS4 refers only to selectable **UI styles**, not runtime support for HyperOS 3.
- The maintainer installed and accepted **B1822**, with source-matched build and signing checks passed. This is not blanket certification for every HyperOS 4 device, GPU, dual-SIM state, orientation, third-party theme or exceptional OEM timing.
- Unmerged upstream experiments, temporary diagnostics and unresolved non-release-blocking Codacy findings are not listed as completed functionality.

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
