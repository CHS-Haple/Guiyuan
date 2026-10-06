package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardIconTransitionSourceTest {
    @Test
    fun pinnedAnimateIconContainerContractRequiresOneBoolean() {
        assertTrue(
            KeyguardIconTransitionSource.matchesSignature(
                arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardIconTransitionSource.matchesSignature(
                emptyArray(),
                Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardIconTransitionSource.matchesSignature(
                arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
    }
}
