package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiKeyguardAodSourceTest {
    @Test
    fun pinnedToggleAodContractRequiresSingleBooleanParameter() {
        assertTrue(
            SysUiKeyguardAodEvidence.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
        assertTrue(
            SysUiKeyguardAodEvidence.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Boolean::class.javaObjectType),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            SysUiKeyguardAodEvidence.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                returnType = Boolean::class.javaPrimitiveType!!,
            ),
        )
        assertFalse(
            SysUiKeyguardAodEvidence.matchesToggleAodSignature(
                parameterTypes = emptyArray(),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            SysUiKeyguardAodEvidence.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
    }

    @Test
    fun stableAodRequiresTargetAodWithNativeAnimationFinished() {
        assertTrue(SysUiKeyguardAodPolicy.isStableAod(true, false))
        assertFalse(SysUiKeyguardAodPolicy.isStableAod(true, true))
        assertFalse(SysUiKeyguardAodPolicy.isStableAod(false, false))
        assertFalse(SysUiKeyguardAodPolicy.isStableAod(false, true))
    }

    @Test
    fun nativeAodTransitionBlocksKeyguardProjection() {
        assertFalse(SysUiKeyguardAodPolicy.blocksKeyguardProjection(false, false))
        assertTrue(SysUiKeyguardAodPolicy.blocksKeyguardProjection(true, false))
        assertTrue(SysUiKeyguardAodPolicy.blocksKeyguardProjection(false, true))
        assertTrue(SysUiKeyguardAodPolicy.blocksKeyguardProjection(true, true))
    }
}
