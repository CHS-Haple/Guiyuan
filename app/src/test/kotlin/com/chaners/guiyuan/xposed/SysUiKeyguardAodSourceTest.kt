package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiKeyguardAodSourceTest {
    @Test
    fun pinnedToggleAodContractRequiresSingleBooleanParameter() {
        assertTrue(
            SysUiKeyguardAodSource.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            SysUiKeyguardAodSource.matchesToggleAodSignature(
                parameterTypes = emptyArray(),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            SysUiKeyguardAodSource.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
    }

    @Test
    fun stableAodRequiresTargetAodWithNativeAnimationFinished() {
        assertTrue(SysUiKeyguardAodSource.isStableAod(true, false))
        assertFalse(SysUiKeyguardAodSource.isStableAod(true, true))
        assertFalse(SysUiKeyguardAodSource.isStableAod(false, false))
        assertFalse(SysUiKeyguardAodSource.isStableAod(false, true))
    }

    @Test
    fun anyNativeAodSignalBlocksKeyguardProjection() {
        assertFalse(SysUiKeyguardAodSource.blocksKeyguardProjection(false, false, false))
        assertTrue(SysUiKeyguardAodSource.blocksKeyguardProjection(true, false, false))
        assertTrue(SysUiKeyguardAodSource.blocksKeyguardProjection(false, true, false))
        assertFalse(SysUiKeyguardAodSource.blocksKeyguardProjection(false, false, true))
        assertFalse(SysUiKeyguardAodSource.blocksKeyguardProjection(false, false, null))
    }
}
