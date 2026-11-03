# Security Policy / 安全政策

Guiyuan 1.0.0 is the first stable release line. The officially supported OS family is Xiaomi HyperOS 4 with a compatible LSPosed / Modern Xposed API 102 environment. The specifically device-tested baseline is Xiaomi 15 Pro on HyperOS `OS4.0.0.19.XOBCNXM` / SystemUI `17.03.260226.r`; other HyperOS 4 builds have not all been independently tested. Reports may concern the latest published `main` release or the active `dev` line. Unsupported platforms can be reported, but are not automatically within the supported security-maintenance scope.

## Reporting a vulnerability / 安全漏洞反馈

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

## Security-relevant areas / 安全问题范围

Examples include:

- unintended Root or shell-command behavior;
- exported-component or intent handling that exposes privileged actions;
- diagnostic/report data disclosure;
- signing or release-pipeline weaknesses;
- unsafe SystemUI hook behavior with a security impact;
- accidental inclusion of credentials or private material in distributed artifacts.

Ordinary visual defects, compatibility problems, and SystemUI crashes without a security impact should use the normal bug-report form.

## Disclosure / 披露

Please allow maintainers time to reproduce and address a confirmed vulnerability before public disclosure.

The current supported stable version is 1.0.0. New fixes are evaluated against the supported HyperOS 4 / API 102 target and released when validated. Older development checkpoints and unrelated HyperOS or framework implementations do not carry a guaranteed security-fix SLA. This policy may be revised in a later stable release.

---

## 简体中文

归元 1.0.0 是首个正式版本，目前对外支持 **Xiaomi HyperOS 4** 及兼容 Modern Xposed API 102 的 LSPosed 环境。已验证的设备基线为 Xiaomi 15 Pro、HyperOS `OS4.0.0.19.XOBCNXM`、SystemUI `17.03.260226.r`；其他设备和系统版本不代表已验证。您可以报告正式版或开发版中的安全问题。

### 私密报告安全漏洞

请使用 [GitHub 私密漏洞报告](https://github.com/CHS-Haple/Guiyuan/security/advisories/new)，不要在公开 Issue 中提交密钥、签名材料、漏洞利用细节或包含敏感信息的日志。

报告时请尽量说明受影响版本、设备与系统环境、问题表现、安全影响和复现方法；必要时附上已脱敏的相关日志。信息不完整也可以先提交。

涉及 Root、系统权限、诊断数据泄露、签名与发布流程或 SystemUI Hook 安全影响的问题适合私密报告。普通界面异常、兼容性问题及不涉及安全风险的崩溃，请使用 [问题反馈模板](https://github.com/CHS-Haple/Guiyuan/issues/new/choose)。

### 披露与维护范围

请给予维护者合理时间复现和修复已确认的漏洞，再公开相关细节。安全修复将根据已支持的 HyperOS 4 / API 102 环境进行评估与发布；旧开发构建、其他系统或框架不承诺固定修复时限。本政策可能随正式版本更新。
