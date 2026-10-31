# 隐私政策 / Privacy Policy

[使用条款 / Terms of Use](TERMS.md)

**最后更新 / Last updated: 2026-10-31**

## 简体中文

本政策说明归元在运行、诊断和文件分享中如何使用信息，以及您可以如何管理这些信息。本政策仅适用于归元。Android、模块框架和其他应用的独立行为，请参阅各自的说明。

### 本地设置和运行数据

归元在设备本地处理 SystemUI 的电量、网络等状态以呈现界面，不会自动向项目维护者上传这些运行信息。外观、语言、桌面图标与诊断偏好等设置也保存在本机。

应用不提供用户账户，不申请 Android 的 INTERNET 权限，并且关闭了 Android 自动应用备份。

### 权限与系统访问

归元通过兼容的模块框架获取 SystemUI 作用域，用于调整状态信息显示。授权和实际生效情况由当前框架服务决定，您可以在对应的模块管理器中启用、调整或停用作用域。Root 不是首次启动的强制要求，仅用于部分诊断与进程维护操作。

进入首次引导权限页时，归元会检测 Root 状态，可能出现授权提示。日志读取、报告生成和进程维护仍需您主动操作或确认。阅读本政策或完成引导确认不会自动执行这些操作。

### 诊断日志与报告

诊断功能可通过 Root 读取 LSPosed 模块日志中的相关记录。此来源不可用时，可能读取系统日志并筛选归元的记录。读取期间可能接触其他应用的日志，但诊断页面及报告仅使用筛选后的记录。

您主动生成的报告可能包含版本、设备型号、系统环境、运行状态和相关日志。归元不会自动提交报告。导出或分享前，请检查是否含有设备标识等敏感信息。

### 保存、备份与清理

设置保存在应用私有区域。配置备份或恢复由您通过 Android 文件选择器发起。卸载应用通常会移除应用私有数据，但不会删除您另存或传出的副本。

使用“清空日志”后，归元会在诊断列表和后续报告中排除清空时间之前的记录，不会删除模块框架和系统日志原文件。分享报告时生成的附件可能保存在 Download/Guiyuan。下一次准备分享时，归元会尝试清理较早的同类附件。您另存或已发送的副本不受影响。

### 分享与第三方平台

归元不会自动向项目维护者发送设置、系统状态或诊断报告。只有当您主动导出文件、使用系统分享功能，或者向 GitHub 等平台提交反馈时，相关信息才会进入您选择的位置或服务。保存后的文件和第三方平台上的内容由相应接收方管理。

Android、模块框架、设备厂商和其他应用的数据处理不受归元控制。请参阅各自的隐私说明。

### 您的选择与安全提示

您可以修改或恢复归元设置，在模块管理器中停用作用域，并自主选择是否授予 Root、生成报告或分享文件。关闭某项权限可能使相关功能不可用。已发送的资料需在对应的接收方管理。

请避免公开发布包含个人信息的日志或配置。归元不能承诺删除其他程序保留的日志，也不能保证第三方接收环境绝对安全。

### 政策更新与联系

数据处理方式发生重要变化时，项目会更新应用说明或公开文档。隐私问题可通过项目仓库反馈。涉及敏感漏洞或私人资料时，请使用 SECURITY.md 中的私密报告渠道。

---

## English

This policy explains how Guiyuan handles information during normal operation, diagnostics, and file sharing, and what you can control. It covers Guiyuan only. Android, module frameworks, and other apps have their own practices.

### Local preferences and runtime data

Guiyuan processes SystemUI information, including battery and connectivity states, locally to render its interface. It does not automatically upload this runtime information to maintainers. Appearance, language, launcher icon, and diagnostic preferences are also stored locally.

The app has no user-account system, does not request Android's INTERNET permission, and disables Android automatic app backup.

### Permissions and system access

Guiyuan uses the SystemUI scope granted by a compatible module framework to adjust status information. Authorization and effective scope depend on the current framework service. You can enable, change, or disable the scope in the corresponding module manager. Root is not mandatory during initial setup and is used for certain diagnostics and process maintenance operations.

Opening the first-run Permissions page checks Root status and may show an authorization prompt. Reading logs, generating reports, and maintaining processes still require your action or confirmation. Reading this policy or completing the guide does not perform those actions.

### Diagnostic logs and reports

Diagnostics can use Root to read relevant entries from LSPosed module logs. If that source is unavailable, Guiyuan may read system logs and filter entries related to the module. Reading system logs may encounter entries from other apps. The diagnostics view and reports use only filtered records.

Reports you choose to generate may contain version, device model, system environment, runtime state, and relevant logs. Guiyuan does not submit reports automatically. Review them for device identifiers or other sensitive details before exporting or sharing.

### Storage, backup, and deletion

Preferences are stored in the app's private area. Backing up or restoring settings requires your action through the Android file picker and a destination you select. Uninstalling normally removes app-private data, but not copies you have saved or transferred elsewhere.

Clearing logs excludes earlier entries from the diagnostics view and later reports. It does not delete original module-framework or system log files. Report attachments prepared for sharing may remain in Download/Guiyuan. Guiyuan attempts to remove older attachments before a later share. Copies you previously saved or sent are unaffected.

### Sharing and third-party platforms

Guiyuan does not automatically send preferences, system state, or diagnostic reports to maintainers. Information reaches a destination or service you choose only when you export a file, use system sharing, or submit feedback through a platform such as GitHub. Saved copies and third-party submissions are managed by their respective recipients.

Independent data processing by Android, module frameworks, device manufacturers, and other apps is outside Guiyuan's control. This policy does not replace their privacy notices.

### Your choices and security considerations

You can change or restore settings in Guiyuan, disable its scope in the corresponding module manager, and decide whether to grant optional Root access, generate reports, or share files. Disabling access may prevent a related feature from working. Information already sent to other apps or platforms must be managed at its destination.

Avoid publishing logs or configuration files containing personal information. Guiyuan cannot guarantee deletion of logs held by other software or the absolute security of third-party destinations.

### Updates and contact

Material changes in data handling will be reflected in updated app disclosures or public documentation. Privacy questions can be raised through the project repository. Use the private reporting channel in SECURITY.md for sensitive vulnerabilities or private material.
