# Security Policy

Guiyuan is currently in pre-release development. Security reports may target the current `main` baseline or the active `dev` line.

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

A broader supported-version policy will be defined with the first formal release.
