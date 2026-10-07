# Privacy

[English](#english) | [简体中文](#简体中文)

## English

Guiyuan is designed to operate locally on the device.

### Network and telemetry

The application does not declare the Android `INTERNET` permission and does not include project-operated analytics, telemetry, advertising, or remote-account services.

### Local preferences

Preferences such as appearance, language choice, launcher visibility, and diagnostics level are stored locally using Android platform storage.

### Root access

Root access is used only for explicit maintenance or diagnostic actions that require it, currently:

- restarting SystemUI after user confirmation;
- reading Guiyuan-related local runtime logs when the user opens or refreshes the Diagnostics log view, or generates a diagnostic report.

Guiyuan does not run a persistent Root service.

### Diagnostic reports

Diagnostic reports are generated locally and only when requested by the user.

A report may contain:

- Guiyuan version, build, package name, build channel, and diagnostics state;
- device model/codename, Android version, HyperOS information, and SystemUI version;
- Guiyuan structured runtime snapshot and module log entries.

Guiyuan does not intentionally collect unrelated third-party application logs for diagnostic reports.

Review diagnostic reports before sharing them publicly, because device and runtime details may be identifying in some contexts.

### Export and sharing

Export uses Android's system document picker.

When preparing a share attachment, Guiyuan creates an app-managed text report under `Download/Guiyuan` and automatically prunes older managed reports.

Guiyuan does not upload diagnostic reports to a project server. Sharing occurs only after the user chooses a destination through Android's system UI.

### Security reports

Do not post credentials, signing material, private logs, or exploit details in public issues. Use the private process described in [SECURITY.md](SECURITY.md).

---

## 简体中文

Guiyuan 以本地运行和最小化数据处理为设计原则。

### 网络与遥测

应用本身不声明 Android `INTERNET` 权限，也不包含由本项目运营的统计分析、遥测、广告或远程账号服务。

### 本地设置

外观、语言选择、桌面图标显示和诊断等级等设置通过 Android 平台存储保存在本机。

### Root 权限

仅在用户明确触发且功能确实需要时使用 Root，目前用于：

- 经用户确认后重启 SystemUI；
- 用户打开或刷新“诊断”日志视图，或主动生成诊断报告时，读取与 Guiyuan 相关的本地运行日志。

归元不使用常驻 Root 服务。

### 诊断报告

诊断报告只在用户主动请求时于本机生成。

报告可能包含：

- Guiyuan 版本、构建、包名、构建通道与诊断状态；
- 设备型号/代号、Android 版本、HyperOS 信息与 SystemUI 版本；
- Guiyuan 结构化运行快照与模块日志。

Guiyuan 不会为了生成诊断报告而主动收集无关第三方应用的日志。

由于设备和运行环境信息在某些场景下可能具有识别性，公开分享报告前仍建议先检查内容。

### 导出与分享

导出使用 Android 系统文件选择器。

准备分享附件时，Guiyuan 会在 `Download/Guiyuan` 下创建由应用管理的文本报告，并自动清理较旧的此类文件。

Guiyuan 不会将诊断报告上传至项目服务器。只有用户通过 Android 系统界面选择目标应用或位置后，报告才会被导出或分享。

### 安全问题

请勿在公开 Issue 中发布账号凭据、签名材料、私人日志或漏洞利用细节。安全问题请使用 [SECURITY.md](SECURITY.md) 中说明的私密报告流程。
