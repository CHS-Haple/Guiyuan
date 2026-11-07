package com.chaners.guiyuan.xposed

import com.android.systemui.Dependency
import com.android.systemui.statusbar.phone.StatusBarIconController as LegacyController
import com.android.systemui.statusbar.phone.ui.StatusBarIconController as ModernController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NativeControllerLookupTest {
    private class Manager(private val mController: Any?)

    @After
    fun clearDependencyFixture() {
        Dependency.lookup = null
    }

    private fun resolve(manager: Manager, loader: ClassLoader = javaClass.classLoader!!): Any? {
        val method =
            NativeParticipantAccess::class.java.getDeclaredMethod(
                "resolveStatusBarIconController",
                ClassLoader::class.java,
                Any::class.java,
            ).apply { isAccessible = true }
        return method.invoke(NativeParticipantAccess, loader, manager)
    }

    @Test
    fun missingDependencyClassFallsBackToManagerOrNull() {
        val isolatedLoader = object : ClassLoader(null) {}
        val fallback = Any()

        assertSame(fallback, resolve(Manager(fallback), isolatedLoader))
        assertNull(resolve(Manager(null), isolatedLoader))
    }

    @Test
    fun modernDependencyCandidateWinsBeforeManagerFallback() {
        val native = Any()
        val calls = mutableListOf<Class<*>>()
        Dependency.lookup = { type ->
            calls += type
            native
        }

        assertSame(native, resolve(Manager(Any())))
        assertEquals(listOf(ModernController::class.java), calls)
    }

    @Test
    fun absentModernCandidateFallsThroughToLegacyCandidate() {
        val native = Any()
        val calls = mutableListOf<Class<*>>()
        Dependency.lookup = { type ->
            calls += type
            if (type == LegacyController::class.java) native else null
        }

        assertSame(native, resolve(Manager(Any())))
        assertEquals(listOf(ModernController::class.java, LegacyController::class.java), calls)
    }

    @Test
    fun missingCandidatesUseManagerFieldWithoutInventingController() {
        val fallback = Any()
        Dependency.lookup = { null }

        assertSame(fallback, resolve(Manager(fallback)))
        assertNull(resolve(Manager(null)))
    }

    @Test
    fun reflectiveLookupFailureFallsThroughThenUsesNativeManager() {
        val fallback = Any()
        var attempts = 0
        Dependency.lookup = {
            attempts++
            throw IllegalStateException("fixture get(Class) unavailable")
        }

        assertSame(fallback, resolve(Manager(fallback)))
        assertEquals(2, attempts)
    }
}
