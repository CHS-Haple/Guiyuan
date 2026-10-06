package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardIconTransitionSourceTest {
    @Test
    fun iconTransitionNeedsOneBool() {
        assertTrue(
            KeyguardIconTransition.matchesAnimateIconContainerSignature(
                arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardIconTransition.matchesAnimateIconContainerSignature(
                emptyArray(),
                Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardIconTransition.matchesAnimateIconContainerSignature(
                arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
    }
}
