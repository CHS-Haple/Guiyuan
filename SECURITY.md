# Security Policy / 安全政策

Guiyuan's stable release line began at 1.0.0; the currently supported stable version is the latest signed stable release published on GitHub Releases. The officially supported OS family is Xiaomi HyperOS 4 with a compatible LSPosed / Modern Xposed API 102 environment. The specifically device-tested baseline is Xiaomi 15 Pro on HyperOS `4.0.0.19.XOBCNXM` / SystemUI `17.03.260226.r`; other HyperOS 4 builds have not all been independently tested. Reports may concern the latest published `main` release or the active `dev` line. Unsupported platforms can be reported, but are not automatically within the supported security-maintenance scope.

## Reporting a vulnerability

Please report security-sensitive issues through GitHub's private vulnerability reporting flow:

**Security -> Report a vulnerability**

Do not publish credentials, signing material, exploit details, private diagnostic data, or other sensitive information in a public issue.

A useful report includes:

- affected Guiyuan version/build/channel;
- Android, HyperOS, and SystemUI version;
- affected component or workflow;
- reproducible steps;
- security impact;
- the smallest relevant log or diagnostic excerpt.

## Security-relevant areas

Examples include:

- unintended Root or shell-command behavior;
- exported-component or intent handling that exposes privileged actions;
- diagnostic/report data disclosure;
- signing or release-pipeline weaknesses;
- unsafe SystemUI hook behavior with a security impact;
- accidental inclusion of credentials or private material in distributed artifacts.

Ordinary visual defects, compatibility problems, and SystemUI crashes without a security impact should use the normal bug-report form.

## Disclosure

Please allow maintainers time to reproduce and address a confirmed vulnerability before public disclosure.

The currently supported stable version is the latest signed stable release listed on [GitHub Releases](https://github.com/CHS-Haple/Guiyuan/releases). New fixes are evaluated against the supported HyperOS 4 / API 102 target and released when validated. Older development checkpoints and unrelated HyperOS or framework implementations do not carry a guaranteed security-fix SLA. This policy may be revised in a later stable release.

---

## 简体中文

归元（Guiyuan）目前支持 **Xiaomi HyperOS 4** 和兼容 Modern Xposed API 102 的 LSPosed 环境。已实机验证的基线为 Xiaomi 15 Pro、HyperOS `4.0.0.19.XOBCNXM`、SystemUI `17.03.260226.r`。其他设备和版本尚未全部验证。可以报告最新正式版或开发版中的安全问题。

### 如何报告安全漏洞

请通过 [GitHub 私密漏洞报告](https://github.com/CHS-Haple/Guiyuan/security/advisories/new)提交安全问题，不要在公开 Issue 中发布账户凭据、签名材料、漏洞利用细节或私人诊断数据。

尽可能注明受影响的版本与构建、设备与系统版本、涉及的功能、复现方法、安全影响以及必要的脱敏日志。信息暂时不完整也可以先提交。

### 哪些问题属于安全问题

例如：Root 或命令执行异常、特权操作意外暴露、诊断数据泄露、签名及发布流程风险、具有安全影响的 SystemUI Hook 行为，或分发产物意外包含私密材料。

仅涉及外观、兼容性或不产生安全风险的崩溃，请使用普通的 [问题反馈模板](https://github.com/CHS-Haple/Guiyuan/issues/new/choose)。

### 披露与维护

请给予维护者合理时间复现并修复已确认的安全漏洞，再公开细节。当前正式版以 [GitHub Releases](https://github.com/CHS-Haple/Guiyuan/releases) 最新已发布的签名稳定版为准；1.0.0 为首个正式版本。修复将按所支持的 HyperOS 4 / API 102 环境评估。旧开发构建、其他系统及框架不承诺固定修复时限，本政策可能随正式版更新。
