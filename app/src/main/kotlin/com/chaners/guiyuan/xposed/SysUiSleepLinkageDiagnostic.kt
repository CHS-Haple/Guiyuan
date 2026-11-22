package com.chaners.guiyuan.xposed

// Passive deep-diagnostics reader. Never installs native hooks or owns presentation state.
internal object SysUiSleepLinkageDiagnostic {
    // Native screen-off decision, sampled only by detailed AOD diagnostics.
    fun read(resolved: SysUiKeyguardHostResolver.ResolvedHost): String? {
        return SysUiPresentationRuntime.optionalNativeRead {
            fun field(owner: Any, name: String): Any? =
                SysUiKeyguardHostResolver.findField(owner.javaClass, name)?.get(owner)

            val dep = field(resolved.host, "mDep") ?: return null
            val lazy = field(dep, "dozeServiceHost") ?: return null
            val doze = lazy.javaClass.methods
                .firstOrNull { it.name == "get" && it.parameterCount == 0 }
                ?.invoke(lazy) ?: return null
            if (doze.javaClass.name !=
                "com.android.keyguard.injector.DozeServiceHostInjector"
            ) {
                return null
            }
            val cc = field(doze, "mControlCenter") ?: return null
            val expanded = cc.javaClass.methods
                .firstOrNull { it.name == "getExpanded" && it.parameterCount == 0 }
                ?.invoke(cc) ?: return null
            "ccExpanded=" + expanded +
                ":linkage=" + field(doze, "mScreenOffNeedLinkageAnim") +
                ":fullAod=" + field(doze, "mScreenOffNeedFullAodAnim") +
                ":screenFade=" + field(doze, "mNeedScreenFade") +
                ":dozeAfterOff=" + field(doze, "mShouldDozeAfterScreenOff") +
                ":sleepReason=" + field(doze, "mSleepReason") +
                ":interactive=" + field(doze, "mDeviceInteractive")
        }
    }
}
