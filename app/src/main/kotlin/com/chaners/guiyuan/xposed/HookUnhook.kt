package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle

// A failed unhook must keep its handle visible to its lifecycle owner.
@Suppress("TooGenericExceptionCaught")
internal fun HookHandle.unhookFailed(): Boolean =
    try {
        unhook()
        false
    } catch (error: Throwable) {
        SysUiFatal.rethrow(error)
        true
    }
