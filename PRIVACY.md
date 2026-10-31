# 隐私政策 / Privacy Policy

[使用条款 / Terms of Use](TERMS.md)

**最后更新 / Last updated: 2026-10-31**

## 简体中文

本政策说明归元在运行、诊断和文件分享过程中会访问哪些信息、如何使用这些信息，以及您可以如何管理。它仅适用于归元本身；Android、模块框架及其他应用的独立行为，应以各自的说明为准。

### 本地设置和运行数据

归元在设备本地处理 SystemUI 的电量、网络等状态以呈现界面，不会自动向项目维护者上传这些运行信息。外观、语言、桌面图标与诊断偏好等设置也保存在本机。

应用不提供用户账户，不申请 Android 的 INTERNET 权限，并且关闭了 Android 自动应用备份。

### 权限与系统访问

归元通过兼容的模块框架获取 SystemUI 作用域，用于调整状态信息显示。授权和实际生效情况由当前框架服务决定，您可以在对应的模块管理器中启用、调整或停用作用域。Root 不是首次启动的强制要求，仅用于部分诊断与进程维护操作。

进入首次引导的权限设置页面时，归元会检测 Root 状态，可能出现 Root 管理器授权提示。日志读取、报告生成和进程维护仍由您主动发起或确认；阅读或同意本政策本身不会触发这些操作。

### 诊断日志与报告

诊断功能当前可通过 Root 读取 LSPosed 模块日志路径的相关记录；无法使用该来源时，可能读取系统日志并筛选归元的记录。读取系统日志时底层可能接触其他应用的日志；诊断页面及报告仅使用筛选后的记录。

您主动生成的报告可能包含版本、设备型号、系统环境、运行状态及相关日志。归元不会自动提交报告；导出或分享前，请检查是否含有设备标识或其他敏感信息。

### 保存、备份与清理

设置保存在应用私有区域；配置备份或恢复由您通过 Android 文件选择器发起并选择位置。卸载应用通常会移除应用私有数据，但不能删除您已经另行保存或传出的副本。

使用“清空日志”后，归元会以清空时间作为界限，隐藏此前的诊断记录，并在后续报告中排除这些记录；模块框架和系统日志原文件不会被删除。分享报告时生成的文本附件可能保存在 Download/Guiyuan，下一次准备分享时会清理较早的同类附件。此前手动保存或发出的副本不受影响。

### 分享与第三方平台

归元不会自动向项目维护者发送设置、系统状态或诊断报告。只有当您主动导出文件、使用系统分享功能，或者向 GitHub 等平台提交反馈时，相关信息才会进入您选择的位置或服务。保存后的文件和第三方平台上的内容由相应接收方管理。

Android、模块框架、设备厂商及其他应用的独立数据处理行为不属于归元控制范围；本政策不替代它们各自的隐私说明。

### 您的选择与安全提示

您可以在归元中修改或恢复设置，在对应的模块管理器中停用作用域，并决定是否授予可选 Root 权限、生成诊断报告或分享文件。关闭某项权限可能使对应功能不可用；已经发送至其他应用或平台的资料，需要到相应位置管理。

请避免公开发布包含个人信息的日志或配置。归元不能承诺删除其他程序保留的日志，也不能保证第三方接收环境绝对安全。

### 政策更新与联系

数据处理方式如发生重要变化，项目会更新应用说明或公开文档。可通过项目仓库提交隐私问题；涉及敏感漏洞或私人资料时，请使用 SECURITY.md 中的私密报告渠道。

---

## English

This policy explains what information Guiyuan accesses during normal operation, diagnostics, and file sharing; how it uses that information; and what you can control. It covers Guiyuan itself. Android, module frameworks, and other apps have their own practices.

### Local preferences and runtime data

Guiyuan processes SystemUI information, including battery and connectivity states, locally to render its interface. It does not automatically upload this runtime information to maintainers. Appearance, language, launcher icon, and diagnostic preferences are also stored locally.

The app has no user-account system, does not request Android's INTERNET permission, and disables Android automatic app backup.

### Permissions and system access

Guiyuan uses the SystemUI scope granted by a compatible module framework to adjust status information. Authorization and effective scope depend on the current framework service. You can enable, change, or disable the scope in the corresponding module manager. Root is not mandatory during initial setup and is used for certain diagnostics and process maintenance operations.

Opening the Permissions step of first-run setup checks Root status and may display an authorization prompt. Log viewing, report generation, and process maintenance remain user-initiated. Reading or accepting this policy does not start these actions.

### Diagnostic logs and reports

Diagnostics can currently use Root to read relevant entries from LSPosed module log paths. If that source is unavailable, Guiyuan may read system logs and filter entries related to the module. Reading system logs may encounter entries from other apps; the diagnostics view and reports use only filtered records.

Reports you choose to generate may contain version, device model, system environment, runtime state, and relevant logs. Guiyuan does not submit reports automatically. Review them for device identifiers or other sensitive details before exporting or sharing.

### Storage, backup, and deletion

Preferences are stored in the app's private area. Backing up or restoring settings requires your action through the Android file picker and a destination you select. Uninstalling normally removes app-private data, but not copies you have saved or transferred elsewhere.

When you clear logs, Guiyuan records a time boundary so earlier entries are omitted from the diagnostics view and subsequent reports. Original module-framework and system log files are not deleted. Report attachments prepared for sharing may remain in Download/Guiyuan; older attachments of that kind are cleaned up before a later share. Copies you previously saved or sent are unaffected.

### Sharing and third-party platforms

Guiyuan does not automatically send preferences, system state, or diagnostic reports to maintainers. Information reaches a destination or service you choose only when you export a file, use system sharing, or submit feedback through a platform such as GitHub. Saved copies and third-party submissions are managed by their respective recipients.

Independent data processing by Android, module frameworks, device manufacturers, and other apps is outside Guiyuan's control. This policy does not replace their privacy notices.

### Your choices and security considerations

You can change or restore settings in Guiyuan, disable its scope in the corresponding module manager, and decide whether to grant optional Root access, generate reports, or share files. Disabling access may prevent a related feature from working. Information already sent to other apps or platforms must be managed at its destination.

Avoid publishing logs or configuration files containing personal information. Guiyuan cannot guarantee deletion of logs held by other software or the absolute security of third-party destinations.

### Updates and contact

Material changes in data handling will be reflected in updated app disclosures or public documentation. Privacy questions can be raised through the project repository; use the private reporting channel in SECURITY.md for sensitive vulnerabilities or private material.
