# Privacy / 隐私说明

[English](#english) | [简体中文](#简体中文)

## English

Guiyuan adjusts how status information appears in supported system interfaces. Settings and diagnostics are processed on the device. Guiyuan does not request Android's `INTERNET` permission or include accounts, ads, or project-operated usage analytics.

### Settings and backups

Appearance, language, launcher icon visibility, and diagnostic preferences are saved on your device. You can back up or restore these settings using Android's file picker. The backup is written to, or read from, the location you choose.

### Diagnostics and Root

When you open or refresh Diagnostics, or generate a report, Guiyuan may use Root access to read its runtime entries from LSPosed module logs or filter relevant entries from system logs. Root is also used for supported SystemUI or Settings maintenance actions after your confirmation. These actions do not require a continuously running Root service.

A diagnostic report is generated locally when you request it. It may include the Guiyuan version and build, device and system versions, runtime state, and related log entries. Some of this information may identify your device or runtime environment. Review the report before sharing it.

**Clear logs** keeps earlier entries out of the Diagnostics view and newly generated reports, even after reopening the app. It does not remove the original shared LSPosed logs or reports you have already exported. New entries continue to appear normally.

### Exporting and sharing

For exports, Android lets you choose where to save the report. For sharing, Guiyuan prepares a text attachment in `Download/Guiyuan` and opens Android's share sheet for you to choose a destination. Older attachments created by Guiyuan are cleaned up when another share is prepared.

After you save or share a copy, its storage and handling depend on the destination you chose.

### Security reports

If you need to report a security issue, please use the private reporting process in [SECURITY.md](SECURITY.md). Check any diagnostic excerpts before including them in a public issue.

---

## 简体中文

归元用于调整受支持系统界面中的状态信息显示。设置与诊断信息由应用在本机处理。归元不申请 Android 的 `INTERNET` 权限，也不提供账号、广告或由项目运营的使用统计服务。

### 设置与备份

外观、语言、桌面图标显示和诊断等级等设置保存在本机。你可以通过 Android 系统文件选择器备份或恢复这些设置，文件的保存位置和读取来源由你选择。

### 诊断与 Root

打开或刷新诊断页面、生成诊断报告时，归元可能使用 Root 权限读取 LSPosed 模块日志中的运行记录，或从系统日志中筛选与归元相关的条目。经你确认后，部分 SystemUI 或设置维护操作也会使用 Root 权限。这些操作不需要常驻 Root 服务。

诊断报告在你发起操作时于本机生成，可能包含归元版本与构建信息、设备及系统版本、运行状态和相关日志。其中的部分信息可能具有设备识别性，分享前可以先查看报告内容。

使用**“清空日志”**后，此前的日志将不再出现在诊断页面及新生成的报告中，重新打开应用也不会恢复显示。此操作不会删除 LSPosed 共享的原始日志或此前已导出的报告，之后的新日志仍会正常显示。

### 导出与分享

导出报告时，你可以通过 Android 系统界面选择保存位置。分享报告时，归元会在 `Download/Guiyuan` 中准备文本附件，并打开系统分享界面供你选择目标应用。再次准备分享文件时，归元会清理较早生成的此类附件。

报告保存或分享后的副本，由你选择的存储位置或目标应用管理。

### 安全问题反馈

如需反馈安全问题，请使用 [SECURITY.md](SECURITY.md) 中的私密报告渠道。向公开 Issue 附加诊断内容前，请先检查是否包含敏感信息。
