package com.chaners.guiyuan.xposed.network

import android.widget.ImageView
import com.chaners.guiyuan.xposed.network.SysUiMobileCollectorLocator.Role
import kotlin.coroutines.Continuation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiMobileCollectorLocatorTest {
    private val locator = SysUiMobileCollectorLocator
    private val loader = javaClass.classLoader!!

    @Test
    fun acceptsOnlyTheExpectedGeneratedCollectorContracts() {
        assertTrue(locator.isVerifiedCandidate(SignalCollector::class.java, Role.SIGNAL, loader))
        assertTrue(
            locator.isVerifiedCandidate(
                ActivityResourceCollector::class.java, Role.ACTIVITY_RESOURCE, loader,
            ),
        )
        assertTrue(
            locator.isVerifiedCandidate(
                ActivityVisibilityCollector::class.java, Role.ACTIVITY_VISIBILITY, loader,
            ),
        )
    }

    @Test
    fun rejectsMissingOrMistypedSignalFields() {
        assertFalse(locator.isVerifiedCandidate(MissingClassId::class.java, Role.SIGNAL, loader))
        assertFalse(locator.isVerifiedCandidate(WrongImageType::class.java, Role.SIGNAL, loader))
        assertFalse(locator.isVerifiedCandidate(WrongClassIdType::class.java, Role.SIGNAL, loader))
    }

    @Test
    fun rejectsMismatchedMethodSignaturesAndCrossRoleCollectors() {
        assertFalse(locator.isVerifiedCandidate(WrongSignalSignature::class.java, Role.SIGNAL, loader))
        assertFalse(locator.isVerifiedCandidate(ActivityResourceCollector::class.java, Role.SIGNAL, loader))
        assertFalse(
            locator.isVerifiedCandidate(
                ActivityVisibilityCollector::class.java, Role.ACTIVITY_RESOURCE, loader,
            ),
        )
    }

    @Test
    fun verifiedExistingNamesNeverInvokeDexKit() {
        val known = mapOf(
            Role.SIGNAL to SignalCollector::class.java.name,
            Role.ACTIVITY_RESOURCE to ActivityResourceCollector::class.java.name,
            Role.ACTIVITY_VISIBILITY to ActivityVisibilityCollector::class.java.name,
        )
        // The JVM unit-test classpath has no DexKit JNI runtime.
        assertTrue(locator.findMissing(loader, known).isEmpty())
    }

    private class SignalCollector {
        @JvmField var `$mobile`: ImageView? = null
        @JvmField var `$r8$classId`: Int = 0
        fun emit(value: Any, continuation: Continuation<*>): Any = value
    }

    private class ActivityResourceCollector {
        @JvmField var `$leftInOut`: ImageView? = null
        fun invokeSuspend(value: Any): Any = value
    }

    private class ActivityVisibilityCollector {
        @JvmField var `$satellite`: ImageView? = null
        @JvmField var `$r8$classId`: Int = 4
        fun emit(value: Any, continuation: Continuation<*>): Any = value
    }

    private class MissingClassId {
        @JvmField var `$mobile`: ImageView? = null
        fun emit(value: Any, continuation: Continuation<*>): Any = value
    }

    private class WrongImageType {
        @JvmField var `$mobile`: String = ""
        @JvmField var `$r8$classId`: Int = 0
        fun emit(value: Any, continuation: Continuation<*>): Any = value
    }

    private class WrongClassIdType {
        @JvmField var `$mobile`: ImageView? = null
        @JvmField var `$r8$classId`: String = "0"
        fun emit(value: Any, continuation: Continuation<*>): Any = value
    }

    private class WrongSignalSignature {
        @JvmField var `$mobile`: ImageView? = null
        @JvmField var `$r8$classId`: Int = 0
        fun emit(value: Any): Any = value
    }
}
