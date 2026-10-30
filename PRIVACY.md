# 隐私政策 / Privacy Policy

[简体中文](#简体中文) | [English](#english)

## 简体中文

归元是一款调整系统状态信息显示的 LSPosed 模块。运行时会在设备上处理 SystemUI 状态、用户设置和诊断信息。归元不会自动向项目维护者发送这些信息，也不申请 Android 的 `INTERNET` 权限。

### 本地设置

外观、语言、桌面图标显示和诊断等级等设置保存在本机。你可以通过 Android 系统文件选择器导出或恢复配置，并自行选择文件的位置。

### 诊断与 Root 权限

查看或刷新诊断日志、生成诊断报告时，归元可能使用 Root 权限读取 LSPosed 共享日志或系统日志，再筛选出与归元相关的记录用于显示和报告。因此，底层读取时可能接触其他应用的日志，诊断页面和报告则使用筛选后的记录。

当你确认重新加载 SystemUI 或结束设置应用进程时，归元也会使用 Root 权限。

诊断报告由你主动生成，可能包含归元版本、设备及系统信息、运行状态和相关日志。使用**“清空日志”**后，较早的记录不再出现在诊断页面和新生成的报告中，但 LSPosed 原始日志及此前导出的文件不会被删除。

### 导出与分享

你可以使用系统文件选择器保存报告，或通过 Android 分享界面选择接收应用。为分享报告，归元会在 `Download/Guiyuan` 中创建文本附件，并在后续准备分享时清理较早生成的此类附件。

报告中的部分设备信息可能具有识别性，分享前建议先查看内容。保存或分享后的副本，由相应的存储位置或接收应用管理。

### 项目反馈与第三方平台

如果你通过 GitHub 等平台提交问题、日志或其他资料，项目维护者可以查看你提供的内容；平台也会按照各自的隐私政策处理这些信息。

有关隐私或安全问题，可以通过项目仓库联系维护者。涉及敏感内容的安全报告，请使用 [SECURITY.md](SECURITY.md) 说明的私密渠道。

---

## English

Guiyuan is an LSPosed module that changes how system status information is displayed. It processes SystemUI state, your settings, and diagnostics on your device. Guiyuan does not automatically send this information to project maintainers or request Android's `INTERNET` permission.

### Settings stored on your device

Appearance, language, launcher icon visibility, and diagnostic preferences are stored locally. You can export or restore a settings backup using Android's file picker, choosing the file and location yourself.

### Diagnostics and Root access

When you view or refresh logs, or generate a diagnostic report, Guiyuan may use Root access to read shared LSPosed logs or system logs before filtering entries related to Guiyuan for display and reporting. The underlying read may therefore encounter other apps' logs; the Diagnostics view and reports use the filtered entries.

Root access is also used when you confirm actions to reload SystemUI or stop the Settings app.

You generate diagnostic reports when needed. They may include the Guiyuan version, device and system information, runtime state, and related logs. **Clear logs** hides earlier entries from the Diagnostics view and newly generated reports; it does not delete the original LSPosed logs or files you have previously exported.

### Exporting and sharing

You can save a report using Android's file picker or choose a receiving app through the system share sheet. To prepare a share, Guiyuan creates a text attachment in `Download/Guiyuan` and cleans up older such attachments when preparing a subsequent share.

Some device details in a report may be identifying, so review it before sharing. Saved or shared copies are handled by the destination or receiving app.

### Project feedback and third-party platforms

If you submit issues, logs, or other material through GitHub or another platform, project maintainers can see what you provide. The platform processes that information under its own privacy policy.

You can contact the maintainers through the project repository about privacy or security questions. For reports containing sensitive security details, use the private channel described in [SECURITY.md](SECURITY.md).
