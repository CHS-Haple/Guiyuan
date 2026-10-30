# Privacy Policy / 隐私政策

[简体中文](#简体中文) | [English](#english)

## English

Guiyuan adjusts how status information appears in supported system interfaces. It uses SystemUI state, your saved settings, and diagnostic information on your device. The app does not provide automatic data uploads or telemetry operated by the project, and it does not request Android's `INTERNET` permission.

### Settings and backups

Appearance, language, launcher icon visibility, and diagnostic preferences are saved on your device. You can use Android's file picker to choose where to export a settings backup or which file to restore.

### Diagnostics and Root access

When you view or refresh diagnostic logs, or generate a report, Guiyuan may use Root access to read shared LSPosed logs or system logs and then filter for entries related to Guiyuan. Because filtering follows the underlying read, entries from other apps may be encountered along the way. The displayed log and report use the selected Guiyuan-related entries.

Root access also supports confirmed maintenance actions for selected SystemUI or Settings scopes. Guiyuan does not run a persistent Root service.

Reports are created on your device when requested. They may contain the app version, device and system details, runtime state, and related logs. Review a report before sharing it, as some details may identify your device or environment.

### Log history and files

**Clear logs** sets a local cutoff for older entries in the Diagnostics view and new reports. It does not delete the original shared LSPosed logs or files you have already exported. New entries continue to appear.

For exports, Android lets you choose the destination. To share a report, Guiyuan prepares a text attachment in `Download/Guiyuan` and opens Android's share sheet. It cleans up older app-managed share attachments when preparing another one; this does not remove separately exported files or copies saved by a receiving app.

### Feedback and other platforms

If you send an issue, report, or contribution through GitHub or another platform, project maintainers can read the information you submit. The platform handles that information under its own privacy terms. For sensitive security issues, please use the private process described in [SECURITY.md](SECURITY.md).

---

## 简体中文

归元用于调整受支持系统界面中的状态信息显示，并在本机处理 SystemUI 状态、用户设置和诊断信息。应用不提供由项目运营的自动数据上传或遥测服务，也不申请 Android 的 `INTERNET` 权限。

### 本地设置与备份

外观、语言、桌面图标显示和诊断等级等设置保存在本机。备份或恢复设置时，你可以通过 Android 系统文件选择器决定保存位置或选择要导入的文件。

### 诊断与 Root 权限

查看、刷新诊断日志或生成报告时，归元可能使用 Root 权限读取 LSPosed 共享日志或系统日志，再筛选出与归元相关的记录。由于筛选发生在底层读取之后，读取过程可能接触其他应用的日志；诊断页面和报告使用的是筛选后的归元相关记录。

经你确认后，Root 权限还可用于所选 SystemUI 或设置作用域的维护操作。归元不运行常驻 Root 服务。

诊断报告在你发起操作时于本机生成，可能包含应用版本、设备与系统信息、运行状态和相关日志。部分信息可能识别设备或运行环境，分享前建议先查看报告内容。

### 历史日志与文件

**“清空日志”**会为诊断页面和新报告设置本地历史截止点，不会删除 LSPosed 共享的原始日志或此前已导出的文件。新产生的记录仍会正常显示。

导出报告时，可以通过 Android 系统界面选择保存位置。分享报告时，归元会在 `Download/Guiyuan` 中准备文本附件，再打开系统分享界面。下次准备分享时，应用会清理较早生成的受管理附件，但不会删除你单独导出的文件或接收方保存的副本。

### 反馈与第三方平台

如果你通过 GitHub 等平台主动提交问题、报告或贡献内容，项目维护者可以查看你提交的信息。平台会按照各自的隐私政策处理这些内容。涉及敏感信息的安全问题，请使用 [SECURITY.md](SECURITY.md) 中的私密报告渠道。
