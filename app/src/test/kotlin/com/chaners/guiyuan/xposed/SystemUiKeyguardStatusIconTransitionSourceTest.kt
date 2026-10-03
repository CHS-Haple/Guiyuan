package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardStatusIconTransitionSourceTest {
    @Test
    fun pinnedAnimateIconContainerContractRequiresOneBoolean() {
        assertTrue(
            SystemUiKeyguardStatusIconTransitionSource.matchesAnimateIconContainerSignature(
                arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
        assertFalse(
            SystemUiKeyguardStatusIconTransitionSource.matchesAnimateIconContainerSignature(
                emptyArray(),
                Void.TYPE,
            ),
        )
        assertFalse(
            SystemUiKeyguardStatusIconTransitionSource.matchesAnimateIconContainerSignature(
                arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                Void.TYPE,
            ),
        )
    }
}
