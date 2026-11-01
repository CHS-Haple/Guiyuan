# Security Policy

Guiyuan 1.0.0 is the first stable release line. The officially supported OS family is Xiaomi HyperOS 4 with a compatible LSPosed / Modern Xposed API 102 environment. The specifically device-tested baseline is Xiaomi 15 Pro on HyperOS `OS4.0.0.19.XOBCNXM` / SystemUI `17.03.260226.r`; other HyperOS 4 builds have not all been independently tested. Reports may concern the latest published `main` release or the active `dev` line. Unsupported platforms can be reported, but are not automatically within the supported security-maintenance scope.

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

The current supported stable version is 1.0.0. New fixes are evaluated against the supported HyperOS 4 / API 102 target and released when validated. Older development checkpoints and unrelated HyperOS or framework implementations do not carry a guaranteed security-fix SLA. This policy may be revised in a later stable release.
