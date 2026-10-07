package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullAodTransitionSourceTest {
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
