# 隐私政策 / Privacy Policy

[使用条款 / Terms of Use](TERMS.md)

**最后更新 / Last updated: 2026-10-31**

## 简体中文

本《隐私政策》说明归元应用及模块如何在设备本地处理系统状态、设置和诊断资料，以及你对相关数据的控制方式。该说明不涵盖操作系统、框架或其他应用的独立行为。

### 本地设置和运行数据

归元在设备本地处理 SystemUI 的电量、网络等状态以呈现界面，不会自动向项目维护者上传这些运行信息。外观、语言、桌面图标与诊断偏好等设置也保存在本机。

应用不提供用户账户，不申请 Android 的 INTERNET 权限，并且关闭了 Android 自动应用备份。

### 权限与系统访问

归元依赖 LSPosed 授予的 SystemUI 作用域。你可以通过框架管理器启用、调整或停用作用域。Root 不是首次启动的强制要求，仅用于部分诊断与进程维护操作。

主动查看或刷新日志、生成报告或确认重启有关进程时，相关功能可能请求 Root；仅靠勾选本协议不会自动执行这些操作。

### 诊断日志与报告

归元可能通过 Root 读取 LSPosed 共享日志或系统日志，并筛选与归元相关的记录。底层读取可能接触其他应用的日志，诊断页面及报告使用的是筛选后的记录。

你主动生成的报告可能包含版本、设备型号、系统环境、运行状态及相关日志。归元不会自动提交报告；导出或分享前，请检查是否含有设备标识或其他敏感信息。

### 保存、备份与清理

设置保存在应用私有区域；配置备份或恢复由你通过 Android 文件选择器发起并选择位置。卸载应用通常会移除应用私有数据，但不能删除你已经另行保存或传出的副本。

清空诊断列表不会删除底层 LSPosed 原始日志或之前导出的文件。为分享创建的文本附件可能保存在 Download/Guiyuan；准备下一次分享时会清理较早的同类附件。

### 分享与第三方平台

归元不会自动把设置、系统状态或诊断报告传给项目维护者。你通过系统分享、文件保存或第三方平台主动提供的内容，则由所选目标及平台按其自身规则处理。

LSPosed、Android、设备厂商及其他应用的独立数据处理行为不属于归元控制范围；本政策不替代它们各自的隐私说明。

### 你的选择与安全提示

你可以修改或恢复设置、关闭作用域、拒绝可选 Root 授权，或选择不生成、不导出、不分享报告。这可能影响对应功能，但不妨碍你控制自己主动提供的资料。

请避免公开发布包含个人信息的日志或配置。归元不能承诺删除其他程序保留的日志，也不能保证第三方接收环境绝对安全。

### 政策更新与联系

数据处理方式如发生重要变化，项目会更新应用说明或公开文档。可通过项目仓库提交隐私问题；涉及敏感漏洞或私人资料时，请使用 SECURITY.md 中的私密报告渠道。

---

## English

This Privacy Policy explains how the Guiyuan app and module process system state, preferences, and diagnostics locally and how you control that information. It does not cover independent behavior of the operating system, framework, or other apps.

### Local preferences and runtime data

Guiyuan processes SystemUI information, including battery and connectivity states, locally to render its interface. It does not automatically upload this runtime information to maintainers. Appearance, language, launcher icon, and diagnostic preferences are also stored locally.

The app has no user-account system, does not request Android's INTERNET permission, and disables Android automatic app backup.

### Permissions and system access

Guiyuan depends on the SystemUI scope granted through LSPosed. You can enable, change, or disable that scope through the framework manager. Root is not mandatory for first-run setup; it is used for certain diagnostics and process maintenance operations.

When you choose to view or refresh logs, generate a report, or confirm a related process restart, the relevant feature may require Root. Accepting these documents alone does not trigger such actions.

### Diagnostic logs and reports

Guiyuan may use Root to read shared LSPosed logs or system logs and filter for related entries. The underlying read may encounter other apps' logs; the diagnostics view and reports use filtered entries.

Reports you choose to generate may contain version, device model, system environment, runtime state, and relevant logs. Guiyuan does not submit reports automatically. Review them for device identifiers or other sensitive details before exporting or sharing.

### Storage, backup, and deletion

Preferences are stored in the app's private area. Backing up or restoring settings requires your action through the Android file picker and a destination you select. Uninstalling normally removes app-private data, but not copies you have saved or transferred elsewhere.

Clearing the diagnostics list does not delete original LSPosed logs or previously exported files. Text attachments prepared for sharing may remain in Download/Guiyuan; older attachments of that kind are cleaned up when a later share is prepared.

### Sharing and third-party platforms

Guiyuan does not automatically send preferences, system state, or diagnostic reports to maintainers. Content you choose to provide through system sharing, file export, or third-party platforms is handled by the chosen recipient and platform under their own practices.

Independent data processing by LSPosed, Android, device manufacturers, and other apps is outside Guiyuan's control. This policy does not replace their privacy notices.

### Your choices and security considerations

You may change or restore settings, disable the module scope, decline optional Root authorization, or choose not to generate, export, or share reports. Related features may become unavailable, but you retain control over material you choose to provide.

Avoid publishing logs or configuration files containing personal information. Guiyuan cannot guarantee deletion of logs held by other software or the absolute security of third-party destinations.

### Updates and contact

Material changes in data handling will be reflected in updated app disclosures or public documentation. Privacy questions can be raised through the project repository; use the private reporting channel in SECURITY.md for sensitive vulnerabilities or private material.

