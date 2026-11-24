package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException

// Reflection wrappers do not turn VM-fatal errors into recoverable failures.
internal object SysUiFatal {
    fun rethrow(error: Throwable) {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
    }
}
