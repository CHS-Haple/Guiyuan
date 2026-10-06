package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardAodTransitionSourceTest {
    @Test
    fun pinnedAnimateFullAodContractRequiresTwoBooleans() {
        assertTrue(
            KeyguardAodTransitionSource.matchesAnimateFullAodSignature(
                parameterTypes =
                    arrayOf<Class<*>>(
                        Boolean::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!,
                    ),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardAodTransitionSource.matchesAnimateFullAodSignature(
                parameterTypes =
                    arrayOf<Class<*>>(
                        Boolean::class.javaPrimitiveType!!,
                    ),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardAodTransitionSource.matchesAnimateFullAodSignature(
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
