package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardIconTransitionSourceTest {
    @Test
    fun pinnedAnimateIconContainerContractRequiresOneBoolean() {
        assertTrue(
            KeyguardIconTransitionSource.matchesAnimateIconContainerSignature(
                arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardIconTransitionSource.matchesAnimateIconContainerSignature(
                emptyArray(),
                Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardIconTransitionSource.matchesAnimateIconContainerSignature(
                arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
    }
}
