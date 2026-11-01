# 隐私政策 / Privacy Policy

[使用条款 / Terms of Use](TERMS.md)

**最后更新 / Last updated: 2026-11-01**

## 简体中文

本隐私政策适用于归元（Guiyuan）应用及模块，说明其在设备本地运行、诊断、文件导出与分享过程中涉及的信息及处理方式。本政策仅适用于归元本身；Android、模块框架、Root 管理器及第三方应用的独立数据处理行为，应以各自的说明为准。

### 第一条 本地设置和运行数据

归元在设备本地处理 SystemUI 的电量、网络等状态以呈现界面，不会自动向项目维护者上传这些运行信息。外观、语言、桌面图标与诊断偏好等设置也保存在本机。

应用不提供用户账户，不申请 Android 的 INTERNET 权限，并且关闭了 Android 自动应用备份。

### 第二条 权限与系统访问

归元通过兼容的模块框架获取 SystemUI 作用域，用于调整状态信息显示。授权和实际生效情况由当前框架服务决定，您可以在对应的模块管理器中启用、调整或停用作用域。Root 不是首次启动的强制要求，仅用于部分诊断与进程维护操作。

进入首次引导的权限页面时，归元会检测 Root 状态，并可能触发 Root 管理器的授权提示。读取诊断日志、生成报告及执行进程维护仍需您主动发起或确认；阅读本政策或完成首次引导，不会自动执行上述操作。

### 第三条 诊断日志与报告

诊断功能当前可通过 Root 读取 LSPosed 模块日志路径的相关记录；无法使用该来源时，可能读取系统日志并筛选归元的记录。读取系统日志时底层可能接触其他应用的日志；诊断页面及报告仅使用筛选后的记录。

您主动生成的报告可能包含版本、设备型号、系统环境、运行状态及相关日志。归元不会自动提交报告；导出或分享前，请检查是否含有设备标识或其他敏感信息。

### 第四条 保存、备份与清理

外观、功能及诊断偏好等设置保存在应用私有区域，通常保留至您修改设置、清除应用数据或卸载应用。配置备份及恢复需由您通过 Android 文件选择器主动发起，并选择文件或存储位置。清除应用数据或卸载应用通常会移除应用私有数据，但不会删除您自行保存或已分享的副本。

使用“清空日志”后，归元仅以清空时间作为界限，在诊断页面和后续报告中排除此前记录；模块框架或系统保存的原始日志不会因此被删除。通过分享功能生成的报告附件可能保存在 Download/Guiyuan。归元会在下次准备分享时尝试清理超过 24 小时的同类附件，并将其管理的附件数量限制在三份以内；清理可能因系统限制或文件操作失败而未完成，且不会在期限届满时自动执行。通过文件选择器另存、已分享或由第三方保留的副本，不属于上述清理范围。

### 第五条 分享与第三方平台

归元不会自动向项目维护者发送设置、系统状态或诊断报告。只有当您主动导出文件、使用系统分享功能，或者向 GitHub 等平台提交反馈时，相关信息才会进入您选择的位置或服务。保存后的文件和第三方平台上的内容由相应接收方管理。

Android、模块框架、设备厂商及其他应用的独立数据处理行为不属于归元控制范围；本政策不替代它们各自的隐私说明。

### 第六条 您的选择与安全提示

您可以在归元中修改或恢复设置，在对应的模块管理器中调整或停用 SystemUI 作用域，并自行决定是否授予可选 Root 权限、生成诊断报告或分享文件。撤销相关权限可能导致对应功能不可用。您也可以通过 Android 系统设置清除归元的应用数据或卸载应用。

导出或分享诊断日志、配置文件前，请检查其中是否包含个人信息、设备标识或其他敏感内容。归元无法控制其他软件已接收或保存的数据，也不保证第三方存储环境的绝对安全。

在适用法律规定的范围内，您可以就相关个人信息提出查阅、复制、更正、删除或其他权利请求。归元不设用户账户，也没有集中保存用户资料的服务端系统；维护者通常无法直接访问或删除仅保存在您设备上的信息。对于已提交至第三方平台的信息，您可能需要向相应接收方提出请求。

### 第七条 政策更新与联系

归元是由项目 GitHub 仓库所列维护者维护的开源项目。隐私咨询、数据处理疑问及相关权利请求可先通过该仓库的 [Issues](https://github.com/CHS-Haple/Guiyuan/issues) 提出；Issues 为公开渠道，请勿提交个人信息、私人日志或其他敏感材料。如需非公开核验，请先在公开 Issues 中提出不包含个人信息的联系请求，由维护者确认是否有合适的非公开沟通方式。在确认安全渠道前，请勿发送敏感资料。安全漏洞请按 SECURITY.md 的说明使用 GitHub 私密漏洞报告渠道，该渠道不作为一般隐私咨询的私信入口。若处理目的、方式、信息种类或其他重要告知事项发生变化，项目将更新本政策并在应用内提示；依法需要重新取得同意的，将另行请求相应确认。

---

## English

This Privacy Policy applies to the Guiyuan app and module. It explains information handled during local operation, diagnostics, file export, and sharing. It applies only to Guiyuan; Android, module frameworks, Root managers, and third-party apps may have their own data-handling practices.

### Article 1 — Local preferences and runtime data

Guiyuan processes SystemUI information, including battery and connectivity states, locally to render its interface. It does not automatically upload this runtime information to maintainers. Appearance, language, launcher icon, and diagnostic preferences are also stored locally.

The app has no user-account system, does not request Android's INTERNET permission, and disables Android automatic app backup.

### Article 2 — Permissions and system access

Guiyuan uses the SystemUI scope granted by a compatible module framework to adjust status information. Authorization and effective scope depend on the current framework service. You can enable, change, or disable the scope in the corresponding module manager. Root is not mandatory during initial setup and is used for certain diagnostics and process maintenance operations.

Opening the first-run Permissions page checks Root status and may trigger an authorization prompt from the Root manager. Reading diagnostic logs, generating reports, and maintaining processes still require your action or confirmation; reading this policy or completing setup does not automatically perform those actions.

### Article 3 — Diagnostic logs and reports

Diagnostics can currently use Root to read relevant entries from LSPosed module log paths. If that source is unavailable, Guiyuan may read system logs and filter entries related to the module. Reading system logs may encounter entries from other apps; the diagnostics view and reports use only filtered records.

Reports you choose to generate may contain version, device model, system environment, runtime state, and relevant logs. Guiyuan does not submit reports automatically. Review them for device identifiers or other sensitive details before exporting or sharing.

### Article 4 — Storage, backup, and deletion

Appearance, feature, and diagnostic preferences are stored in the app-private area and generally remain until you change them, clear app data, or uninstall Guiyuan. Configuration backup and restore require your action through the Android document picker and a file or location you select. Clearing app data or uninstalling normally removes app-private data, but not copies you saved or shared elsewhere.

Clearing logs records a time boundary and excludes earlier records from the diagnostics view and subsequent reports; it does not delete original logs retained by the module framework or Android. Report attachments created through sharing may remain in Download/Guiyuan. Before the next share, Guiyuan attempts to remove managed attachments older than 24 hours and keep no more than three; cleanup may fail because of system restrictions or file-operation errors, and does not run automatically when the age limit is reached. Copies saved separately, already shared, or retained by third parties are outside this cleanup process.

### Article 5 — Sharing and third-party platforms

Guiyuan does not automatically send preferences, system state, or diagnostic reports to maintainers. Information reaches a destination or service you choose only when you export a file, use system sharing, or submit feedback through a platform such as GitHub. Saved copies and third-party submissions are managed by their respective recipients.

Independent data processing by Android, module frameworks, device manufacturers, and other apps is outside Guiyuan's control. This policy does not replace their privacy notices.

### Article 6 — Your choices and security considerations

You can change or restore Guiyuan settings, modify or disable the SystemUI scope in the relevant module manager, and decide whether to grant optional Root access, generate diagnostic reports, or share files. Revoking a permission may make related features unavailable. You can also clear Guiyuan app data or uninstall the app through Android system settings.

Before exporting or sharing diagnostic logs or configuration files, review them for personal information, device identifiers, or other sensitive content. Guiyuan cannot control data already received or retained by other software, or guarantee the absolute security of third-party storage.

Where provided by applicable law, you may request access to, copies of, correction of, or deletion of relevant personal information, or exercise other applicable rights. Guiyuan has no user accounts or centralized server-side store of user profiles; maintainers generally cannot directly access or delete information stored only on your device. For information submitted to third-party platforms, you may need to contact the relevant recipient.

### Article 7 — Updates and contact

Guiyuan is an open-source project maintained by the maintainers listed in its GitHub repository. For privacy inquiries, data-handling questions, or relevant rights requests, first use the repository [Issues](https://github.com/CHS-Haple/Guiyuan/issues). Issues are public: do not submit personal information, private logs, or other sensitive material. For requests requiring private verification, first open an Issue without personal information so maintainers can establish whether a suitable non-public contact method is available. Do not send sensitive material until a secure route has been confirmed. Report security vulnerabilities privately as described in SECURITY.md; that channel is not a general private inbox for privacy inquiries. Changes to processing purposes, methods, categories of information, or other material disclosures will be reflected in this policy and notified in the app; where the law requires fresh consent, a separate confirmation will be requested.
