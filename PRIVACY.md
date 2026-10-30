# Privacy Policy / 隐私政策

**Effective date / 生效日期：2026-10-31**

[English](#english) | [简体中文](#简体中文)

## English

### 1. About this policy

This policy explains how **Guiyuan**, an open-source Xiaomi HyperOS status-indicator module and companion application, handles information. It covers the app and its supported LSPosed/SystemUI integration as distributed from the [Guiyuan project](https://github.com/CHS-Haple/Guiyuan). The project's maintainers are responsible for the features described here, not for Android, LSPosed, Root managers, GitHub, browsers, or other independently operated services.

**Summary:** Guiyuan operates locally. The app does not declare Android's `INTERNET` permission, and the project does not operate an account system, analytics, advertising, telemetry, or automatic report-upload service. Local processing of system information and logs is still data handling, and choosing to export or submit data may disclose it to others.

### 2. Information used on your device

Guiyuan processes information needed to provide its features:

- **Preferences and customizations:** enabled features, Xposed-related options, appearance, layouts, colors and custom battery schemes, font-weight preferences, diagnostic level and locally stored maintenance state.
- **SystemUI status and environment:** battery and charging state, Wi-Fi and mobile signal/status, SIM/data and airplane-mode state, relevant status-icon and view properties, available font information, and supported Home/Keyguard/AOD/Control Center transitions. These are used to render or safely restore the indicator, not to create a developer-operated device profile.
- **Diagnostics when requested:** application/module version, Build and channel, device/model and software versions, SystemUI/LSPosed environment, runtime status, diagnostic events and relevant log excerpts. Diagnostic data can contain device-specific or potentially identifying details.

The project does not require you to create a Guiyuan account. Information processed by Android, the Xposed framework or other apps remains subject to their respective implementations and controls.

### 3. Permissions, Xposed and Root

- **Network:** the current application manifest does not request `INTERNET`. There is no built-in project-server synchronization or automatic diagnostic submission.
- **Xposed integration:** only the declared SystemUI scope and optional Settings scope are used for the corresponding features. The module works within those host processes when the framework has enabled it; those processes have their own privileges and behavior.
- **Root:** the companion app invokes Root only for user-requested diagnostic log access or for a specifically confirmed restart of eligible SystemUI/Settings processes. It does not keep a persistent Root service.
- **System communication:** the exported native font-information receiver is protected by Android's `STATUS_BAR` permission and checks the sender's SystemUI identity. This is for local interprocess communication, not data upload.

Root-assisted log reading may access shared LSPosed log files or system logcat output **before filtering** the entries shown or exported. Guiyuan filters for relevant module events and does not intentionally include unrelated applications' logs, but cannot guarantee that the underlying shared log source contains no unrelated information.

### 4. Storage, retention and deletion

- App preferences and diagnostic-history settings reside in app-managed local storage. The application declares `allowBackup=false` for Android's automatic app backup; this does not affect any files you deliberately export.
- **Clear log history** saves a local cutoff so earlier Guiyuan entries are excluded from subsequent diagnostic lists and newly generated reports. New events remain available. It **does not** erase shared LSPosed/source logs, system logcat history or previously exported reports.
- Exported configuration backups and reports are stored wherever you choose through Android's document picker. You control those files and any synchronization performed by your chosen storage provider.
- To prepare an Android share attachment, the app creates an app-managed report in `Download/Guiyuan`. On subsequent share creation, it prunes older app-owned reports according to its current limit (up to three recent managed reports and a 24-hour age threshold). **This is cleanup on a later operation, not guaranteed deletion exactly 24 hours after creation.** Other files in that folder are not intended to be removed.
- Clearing the app's storage or uninstalling Guiyuan removes app-managed private data through Android's normal mechanisms. It does not necessarily remove exported documents, shared framework logs or copies already sent elsewhere.

### 5. Exporting, sharing and third-party services

Guiyuan does **not automatically send** diagnostic reports, backups or system state to its maintainers. File export uses the Android document picker. Sharing uses Android's system share flow and only proceeds to a destination you choose. Please inspect a report or backup before sharing it.

If you open the project's GitHub pages, visit an external link or donation page, publish an Issue/PR, submit a vulnerability report, or send a report through another app, that website, storage provider or recipient may process the content and connection information under its own terms. Public GitHub contributions can remain visible in repository history or forks; the Guiyuan app cannot delete that history or third-party copies.

Open-source libraries used to render the interface and access Android/Xposed APIs are listed in [Third-Party Notices](THIRD_PARTY_NOTICES.md). Their inclusion does not imply that the app uploads data to their developers.

### 6. Your choices and security

You can disable a feature or its Xposed scope, change saved settings, clear Guiyuan-visible diagnostic history, delete exported files, clear app data, or uninstall the app. Disabling a module does not delete shared system logs. System/Root access should be granted only when you understand and need the corresponding operation.

The app applies local input, scope and sender checks, but no software or privileged integration can promise absolute security. Avoid publicly posting complete diagnostic reports, identifiers, private configuration data, credentials or security exploit details.

### 7. Contact and policy changes

Questions about this policy can be raised through the [Guiyuan repository](https://github.com/CHS-Haple/Guiyuan/issues) without including sensitive information. Report security or privacy vulnerabilities through [GitHub's private vulnerability reporting](https://github.com/CHS-Haple/Guiyuan/security/advisories/new) as described in [SECURITY.md](SECURITY.md).

Material changes to this policy will be recorded in the repository with an updated effective date. Reviewing the published policy is recommended when updating the app. This policy does not claim control over independently operated platforms or guarantee particular deletion periods for their data.

---

## 简体中文

### 一、适用范围

本政策说明 **归元（Guiyuan）** 如何处理与功能有关的信息。归元是一款面向 Xiaomi HyperOS 的开源状态栏图标模块及配套应用。本政策适用于 [归元项目](https://github.com/CHS-Haple/Guiyuan) 提供的应用，以及其受支持的 LSPosed/SystemUI 集成功能。Android 系统、LSPosed 框架、Root 管理器、GitHub、浏览器等独立服务不由归元维护者运营。

**简要说明：**归元以本地运行为主。当前应用不申请 Android `INTERNET` 联网权限，项目不提供账号系统、广告、统计分析、遥测或自动上传诊断报告的服务。但本地读取系统状态和日志仍属于信息处理；主动导出、分享或反馈的信息可能被接收方看到。

### 二、在本机处理的信息

为实现相关功能，归元可能在设备本地处理：

- **设置与自定义内容：**功能开关、Xposed 相关设置、界面外观与布局、颜色与自定义电池方案、字重、诊断等级及必要的本地维护状态。
- **系统界面状态：**电量与充电、Wi-Fi 和蜂窝网络状态、SIM 卡和数据网络、飞行模式、状态图标与视图属性、可用字体信息，以及受支持的主状态栏、锁屏、息屏显示与控制中心切换状态。这些信息用于绘制图标和安全恢复原生显示，并非用于建立开发者侧的设备画像。
- **用户请求的诊断信息：**归元版本、构建号和渠道、设备型号及系统版本、SystemUI/LSPosed 环境、运行状态、诊断事件和相关日志片段。这些内容可能包含能够间接识别设备或使用环境的信息。

使用归元无需注册项目账号。Android、Xposed 框架及其他应用自身处理的信息，仍由各自的实现和设置管理。

### 三、权限、Xposed 与 Root

- **联网：**当前应用清单不声明 `INTERNET` 权限，没有项目服务器同步功能，也不会自动提交诊断报告。
- **Xposed：**仅在框架授权的系统界面作用域及可选的系统设置作用域中启用相应功能。模块会在被授权的宿主进程内运行；宿主本身具有独立权限和行为。
- **Root：**仅在用户主动读取诊断日志，或确认重启指定的 SystemUI/设置进程时按需调用 Root；不常驻运行 Root 服务。
- **本机进程通信：**用于获取系统字体信息的接收器受 Android `STATUS_BAR` 权限约束，并验证系统界面发送方身份；该通信不用于联网传输。

使用 Root 读取日志时，底层可能先访问 **LSPosed 共享日志或系统 logcat**，再筛选归元相关条目供界面展示或报告导出。归元不会有意把无关应用日志加入诊断报告，但不能保证共享日志源本身完全不含其他应用的信息。

### 四、本地存储、保留与清理

- 配置和诊断历史设置保存在应用管理的本地存储中。应用通过 `allowBackup=false` 关闭 Android 的自动应用备份；用户主动导出的文件不受此设置影响。
- **“清空日志”**会在本机记录历史截止时间，使此前的归元日志不再出现在后续诊断列表及新生成的报告中；之后的新日志正常记录。该功能**不会删除 LSPosed 共享原始日志、系统日志，也不会删除此前导出的报告**。
- 配置备份及通过系统文件选择器导出的报告，保存在用户指定的位置。文件的保管、删除及所选存储服务的同步行为由用户或该服务决定。
- 为通过 Android 系统分享报告，归元会在 `Download/Guiyuan` 创建应用管理的文本附件。**下次创建分享附件时**，应用会按现有规则清理较旧的自有报告（最多保留三份近期文件，并检查 24 小时的文件年龄阈值）。这不代表文件创建满 24 小时就会自动删除，也不意味着清理该目录中的其他文件。
- 清除应用数据或卸载归元，可由 Android 按其机制清理应用私有数据，但不一定删除已导出的文件、框架共享日志或已经发给第三方的副本。

### 五、导出、分享与第三方平台

归元**不会自动向项目维护者发送**诊断报告、配置备份或系统状态。导出使用 Android 系统文件选择器，分享通过系统分享界面交给用户选择的目标。分享前建议检查内容，避免公开设备标识、私人配置或不必要的日志。

当用户主动访问 GitHub、外部网页或赞助页面，提交 Issue/PR、安全反馈，或通过其他应用发送文件时，相关网站、存储服务或接收方可能按自己的规则处理上传内容与连接信息。公开的 GitHub 内容可能保留在仓库历史或第三方 Fork 中；归元应用无法代为删除这些记录或外部副本。

界面及系统集成所使用的开源组件见[第三方组件与许可](THIRD_PARTY_NOTICES.md)。使用第三方开源代码并不代表应用会向其开发者上传数据。

### 六、用户选择与信息安全

用户可以关闭功能及对应 Xposed 作用域、调整本地配置、清空归元可见的历史日志、删除导出文件、清除应用数据或卸载归元。关闭模块不会清理系统或框架保存的共享日志。请仅在了解相应用途时授予 Xposed 及 Root 权限。

归元针对本地输入、作用域及进程通信设置了必要的校验，但任何软件及特权集成都无法承诺绝对安全。请勿在公开渠道直接提供完整私人日志、凭据、敏感配置或漏洞利用细节。

### 七、联系与政策更新

对隐私政策有疑问，可通过[归元 GitHub 仓库](https://github.com/CHS-Haple/Guiyuan/issues)反馈非敏感问题。涉及安全或隐私漏洞时，请按照[安全政策](SECURITY.md)使用 [GitHub 私密漏洞报告](https://github.com/CHS-Haple/Guiyuan/security/advisories/new)，不要公开个人信息或敏感日志。

政策出现实质变化时，项目将在仓库中更新内容与生效日期。建议更新应用时查阅最新版本。本政策不代表归元能够控制其他独立平台的数据处理，也不保证外部平台遵循特定的数据删除期限。
