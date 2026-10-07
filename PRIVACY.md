# Privacy

[English](#english) | [简体中文](#简体中文)

## English

Guiyuan is designed to operate locally on the device.

### Network and telemetry

The application does not declare the Android `INTERNET` permission and does not include project-operated analytics, telemetry, advertising, or remote-account services.

### Local preferences

Application preferences such as appearance, language-related state, launcher visibility, and diagnostics level are stored locally through Android platform storage.

### Root access

Root access is requested only for explicit maintenance/diagnostic actions that require it, currently including:

- restarting SystemUI after user confirmation;
- reading Guiyuan-related local runtime logs when the user opens or refreshes the Diagnostics log view, or generates a diagnostic report.

There is no resident Root service.

### Diagnostic reports

Diagnostic reports are generated locally and only when requested by the user.

A report may contain:

- Guiyuan version, build, package, build channel, and diagnostics state;
- device model/codename, Android version, HyperOS information, and SystemUI version;
- Guiyuan runtime health and module log entries;

Guiyuan does not intentionally collect unrelated third-party application logs for diagnostic reports.

Users should still review a diagnostic report before posting it publicly because device and runtime information may be identifying in some contexts.

### Export and sharing

Export uses the Android system document flow selected by the user.

When preparing a share attachment, Guiyuan creates a managed text report under `Download/Guiyuan`. Managed share reports are bounded and older entries are pruned by the app.

Guiyuan does not upload diagnostic reports to a project server. Sharing occurs only through the Android destination/application selected by the user.

### Security reports

Do not post credentials, signing material, private logs, or exploit details in public issues. Use the private process described in [SECURITY.md](SECURITY.md).

---

## 简体中文

Guiyuan 以本地运行和最小化数据处理为设计原则。

### 网络与遥测

应用本身不声明 Android `INTERNET` 权限，也不包含由本项目运营的统计分析、遥测、广告或远程账号服务。

### 本地设置

外观、语言相关状态、桌面图标显示以及诊断等级等应用设置通过 Android 平台存储保存在本机。

### Root 权限

只有用户明确触发且确实需要时才使用 Root，目前主要用于：

- 经用户确认后重启 SystemUI；
- 用户打开或刷新“诊断”日志视图，或主动生成诊断报告时，读取与 Guiyuan 相关的本地运行日志。

项目不使用常驻 Root 服务。

### 诊断报告

诊断报告只在用户主动请求时于本机生成。

报告可能包含：

- Guiyuan 版本、构建、包名、构建通道与诊断状态；
- 设备型号/代号、Android 版本、HyperOS 信息与 SystemUI 版本；
- Guiyuan 运行健康状态与模块日志；
- Guiyuan 自身有边界的分享诊断记录。

Guiyuan 不会为了生成诊断报告而主动收集无关第三方应用的日志。

由于设备和运行环境信息在某些场景下可能具有识别性，公开提交报告前仍建议用户先自行检查内容。

### 导出与分享

导出使用用户选择的 Android 系统文档流程。

准备分享附件时，Guiyuan 会在 `Download/Guiyuan` 下创建受管理的文本报告；这类临时分享报告数量和保存时间均有边界，并由应用清理旧条目。

Guiyuan 不会将诊断报告上传至项目服务器。只有用户通过 Android 系统选择目标应用或位置后，报告才会被导出或分享。

### 安全问题

请勿在公开 Issue 中发布账号凭据、签名材料、私人日志或漏洞利用细节。安全问题请使用 [SECURITY.md](SECURITY.md) 中说明的私密报告流程。
