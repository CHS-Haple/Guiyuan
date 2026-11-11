package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FullAodTransitionSourceTest {
    private class NativeHookMethods {
        @Suppress("UNUSED_PARAMETER")
        private fun animate(enabled: Boolean) = Unit

        @Suppress("UNUSED_PARAMETER")
        private fun animate(level: Int) = Unit
    }

    @Test
    fun strictDeclaredHookSelectionDoesNotGuessOverloads() {
        val method = SysUiPresentationRuntime.requireUniqueHookMethod(
            NativeHookMethods::class.java,
            "animate",
            "fixture-hook-contract-",
        ) { candidate ->
            candidate.parameterTypes.contentEquals(
                arrayOf(Boolean::class.javaPrimitiveType!!),
            ) && candidate.returnType == Void.TYPE
        }
        assertTrue(method.isAccessible)
        assertEquals(Boolean::class.javaPrimitiveType, method.parameterTypes.single())

        val missing = assertThrows(IllegalStateException::class.java) {
            SysUiPresentationRuntime.requireUniqueHookMethod(
                NativeHookMethods::class.java,
                "notPresent",
                "fixture-hook-contract-",
            ) { true }
        }
        assertEquals("fixture-hook-contract-missing", missing.message)

        val ambiguous = assertThrows(IllegalStateException::class.java) {
            SysUiPresentationRuntime.requireUniqueHookMethod(
                NativeHookMethods::class.java,
                "animate",
                "fixture-hook-contract-",
            ) { true }
        }
        assertEquals("fixture-hook-contract-ambiguous", ambiguous.message)
    }

    @Test
    fun pinnedAnimateFullAodContractRequiresTwoBooleans() {
        assertTrue(
            FullAodTransitionSource.matchesSignature(
                parameterTypes =
                    arrayOf<Class<*>>(
                        Boolean::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!,
                    ),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            FullAodTransitionSource.matchesSignature(
                parameterTypes =
                    arrayOf<Class<*>>(
                        Boolean::class.javaPrimitiveType!!,
                    ),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            FullAodTransitionSource.matchesSignature(
                parameterTypes =
                    arrayOf<Class<*>>(
                        Boolean::class.javaPrimitiveType!!,
                        Int::class.javaPrimitiveType!!,
                    ),
                returnType = Void.TYPE,
            ),
        )
    }
}
