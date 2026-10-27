package com.chaners.guiyuan.xposed

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RenderFatalTest {
    @Test
    fun directFatalRetainsThrowableIdentity() {
        val vm = OutOfMemoryError("renderer-fatal")
        val thread = ThreadDeath()
        assertSame(vm, runCatching { rethrowRenderFatal(vm) }.exceptionOrNull())
        assertSame(thread, runCatching { rethrowRenderFatal(thread) }.exceptionOrNull())
    }

    @Test
    fun reflectivelyWrappedFatalPropagatesItsCause() {
        val vm = OutOfMemoryError("native-vector-fatal")
        assertSame(
            vm,
            runCatching { rethrowRenderFatal(InvocationTargetException(vm)) }.exceptionOrNull(),
        )
    }

    @Test
    fun ordinaryResourceFailuresKeepOriginalFallback() {
        assertNull(
            runCatching { rethrowRenderFatal(IllegalArgumentException("resource")) }.exceptionOrNull(),
        )
        assertNull(
            runCatching {
                rethrowRenderFatal(InvocationTargetException(IllegalStateException("vector")))
            }.exceptionOrNull(),
        )
    }
}
