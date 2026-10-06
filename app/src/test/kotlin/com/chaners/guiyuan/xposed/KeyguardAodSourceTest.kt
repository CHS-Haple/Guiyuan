package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardAodSourceTest {
    @Test
    fun toggleAodContractNeedsOneBool() {
        assertTrue(
            KeyguardAodSource.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardAodSource.matchesToggleAodSignature(
                parameterTypes = emptyArray(),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            KeyguardAodSource.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
    }

    @Test
    fun stableAodNeedsFinishedAnimation() {
        assertTrue(KeyguardAodSource.isStableAod(true, false))
        assertFalse(KeyguardAodSource.isStableAod(true, true))
        assertFalse(KeyguardAodSource.isStableAod(false, false))
        assertFalse(KeyguardAodSource.isStableAod(false, true))
    }

    @Test
    fun nativeAodBlocksKeyguard() {
        assertFalse(KeyguardAodSource.blocksKeyguardProjection(false, false, false))
        assertTrue(KeyguardAodSource.blocksKeyguardProjection(true, false, false))
        assertTrue(KeyguardAodSource.blocksKeyguardProjection(false, true, false))
        assertFalse(KeyguardAodSource.blocksKeyguardProjection(false, false, true))
        assertFalse(KeyguardAodSource.blocksKeyguardProjection(false, false, null))
    }
}
