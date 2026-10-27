package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException

// Preview and SystemUI share a renderer without loading the Xposed runtime owner.
internal fun rethrowRenderFatal(error: Throwable) {
    val cause = (error as? InvocationTargetException)?.targetException ?: error
    if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
}
