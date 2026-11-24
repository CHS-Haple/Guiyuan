package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FullAodTransitionSourceTest {
    private class NativeHookMethods {
        @Suppress("UNUSED_PARAMETER")
        private fun animate(enabled: Boolean) {}

        @Suppress("UNUSED_PARAMETER")
        private fun animate(level: Int) {}
    }

    @Test
    fun strictDeclaredHookSelectionDoesNotGuessOverloads() {
        val contract = "fixture-hook-contract-"
        val method = SysUiPresentationRuntime.requireUniqueHookMethod(
            NativeHookMethods::class.java,
            "animate",
            contract,
        ) { candidate ->
            candidate.parameterCount == 1 &&
                candidate.parameterTypes[0] == Boolean::class.javaPrimitiveType &&
                candidate.returnType == Void.TYPE
        }
        assertTrue(method.isAccessible)
        assertEquals(Boolean::class.javaPrimitiveType, method.parameterTypes.single())

        val missing = assertThrows(IllegalStateException::class.java) {
            SysUiPresentationRuntime.requireUniqueHookMethod(
                NativeHookMethods::class.java,
                "notPresent",
                contract,
            ) { true }
        }
        assertEquals("fixture-hook-contract-missing", missing.message)

        val ambiguous = assertThrows(IllegalStateException::class.java) {
            SysUiPresentationRuntime.requireUniqueHookMethod(
                NativeHookMethods::class.java,
                "animate",
                contract,
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
