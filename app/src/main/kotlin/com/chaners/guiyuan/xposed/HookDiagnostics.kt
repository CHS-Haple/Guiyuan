package com.chaners.guiyuan.xposed

/** A single install-generation diagnostic gate, shared by the three Keyguard/AOD native sources. */
internal class HookDiagnostics(
    val onEvent: ((String) -> Unit)?,
    val isDetailedDiagnosticsEnabled: () -> Boolean,
)
