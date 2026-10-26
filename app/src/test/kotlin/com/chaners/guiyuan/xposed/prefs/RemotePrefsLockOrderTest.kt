package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DIAG_LEVEL_KEY
import com.chaners.guiyuan.settings.DUAL_SIGNAL_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePrefsLockOrderTest {
    @After
    fun cleanUp() {
        FeaturePrefsOwner.unbind()
        VisualPrefsOwner.unbind()
        DiagPrefsOwner.unbind()
    }

    @Test
    fun featureListenerCanFinishWhileRebinding() =
        testListenerLock(FEATURE_ENABLED_KEY) { FeaturePrefsOwner.bind(it) { _, _ -> } }

    @Test
    fun visualListenerCanFinishWhileRebinding() =
        testListenerLock(DUAL_SIGNAL_ENABLED_KEY) { VisualPrefsOwner.bind(it) { } }

    @Test
    fun diagnosticsListenerCanFinishWhileRebinding() =
        testListenerLock(DIAG_LEVEL_KEY) {
            DiagPrefsOwner.bind(it, forceDetailed = false) { }
        }

    @Test
    fun staleVisualReadCannotReplaceNewConfiguration() {
        val old = LockedPrefs()
        val next = LockedPrefs()
        VisualPrefsOwner.bind(old) { }
        testStaleRead(old, DUAL_SIGNAL_ENABLED_KEY, { old.dual = true }) {
            VisualPrefsOwner.bind(next) { }
        }
        assertFalse(VisualPrefsOwner.current().dualSignal)
    }

    @Test
    fun staleFeatureReadCannotReplaceNewConfiguration() {
        val old = LockedPrefs()
        val next = LockedPrefs()
        FeaturePrefsOwner.bind(old) { _, _ -> }
        testStaleRead(old, FEATURE_ENABLED_KEY, { old.featureEnabled = false }) {
            FeaturePrefsOwner.bind(next) { _, _ -> }
        }
        assertTrue(FeaturePrefsOwner.current().enabled)
    }

    @Test
    fun featureChangedBeforeListenerRegistrationIsRead() {
        val prefs = LockedPrefs().apply {
            onRegister = { featureEnabled = false }
        }
        val delivered = mutableListOf<Boolean>()
        val bound = FeaturePrefsOwner.bind(prefs) { cfg, _ -> delivered += cfg.enabled }
        assertFalse(bound.enabled)
        assertFalse(FeaturePrefsOwner.current().enabled)
        assertEquals(false, delivered.last())
    }

    @Test
    fun visualChangedBeforeListenerRegistrationIsRead() {
        val prefs = LockedPrefs().apply {
            onRegister = { dual = true }
        }
        val delivered = mutableListOf<Boolean>()
        val bound = VisualPrefsOwner.bind(prefs) { cfg -> delivered += cfg.dualSignal }
        assertTrue(bound.dualSignal)
        assertTrue(VisualPrefsOwner.current().dualSignal)
        assertEquals(true, delivered.last())
    }

    @Test
    fun failedFeatureDeliveryCanRetryTheSameValue() {
        val prefs = LockedPrefs()
        val failure = IllegalStateException("feature-receiver")
        var attempts = 0
        FeaturePrefsOwner.bind(prefs) { cfg, _ ->
            if (!cfg.enabled) {
                attempts++
                if (attempts == 1) throw failure
            }
        }
        prefs.featureEnabled = false
        assertSame(
            failure,
            runCatching { prefs.notifyListeners(FEATURE_ENABLED_KEY) }.exceptionOrNull(),
        )
        assertTrue(FeaturePrefsOwner.current().enabled)
        prefs.notifyListeners(FEATURE_ENABLED_KEY)
        assertEquals(2, attempts)
        assertFalse(FeaturePrefsOwner.current().enabled)
    }

    @Test
    fun failedVisualDeliveryCanRetryTheSameValue() {
        val prefs = LockedPrefs()
        val failure = IllegalArgumentException("visual-receiver")
        var attempts = 0
        VisualPrefsOwner.bind(prefs) { cfg ->
            if (cfg.dualSignal) {
                attempts++
                if (attempts == 1) throw failure
            }
        }
        prefs.dual = true
        assertSame(
            failure,
            runCatching { prefs.notifyListeners(DUAL_SIGNAL_ENABLED_KEY) }.exceptionOrNull(),
        )
        assertFalse(VisualPrefsOwner.current().dualSignal)
        prefs.notifyListeners(DUAL_SIGNAL_ENABLED_KEY)
        assertEquals(2, attempts)
        assertTrue(VisualPrefsOwner.current().dualSignal)
    }

    @Test
    fun failedInitialFeatureDeliveryRetiresItsListener() {
        val prefs = LockedPrefs()
        val error = IllegalStateException("feature-initial")
        assertSame(
            error,
            runCatching { FeaturePrefsOwner.bind(prefs) { _, _ -> throw error } }.exceptionOrNull(),
        )
        assertEquals(0, prefs.listenerCount)
        assertFalse(FeaturePrefsOwner.current().enabled)
    }

    @Test
    fun failedInitialVisualDeliveryRetiresItsListener() {
        val prefs = LockedPrefs().apply { dual = true }
        val error = IllegalStateException("visual-initial")
        assertSame(
            error,
            runCatching { VisualPrefsOwner.bind(prefs) { throw error } }.exceptionOrNull(),
        )
        assertEquals(0, prefs.listenerCount)
        assertFalse(VisualPrefsOwner.current().dualSignal)
    }

    @Test
    fun failedOldFeatureDeliveryDoesNotRetireReentrantNewBinding() {
        val old = LockedPrefs()
        val next = LockedPrefs().apply { featureEnabled = false }
        val error = IllegalStateException("feature-old")
        assertSame(
            error,
            runCatching {
                FeaturePrefsOwner.bind(old) { _, _ ->
                    FeaturePrefsOwner.bind(next) { _, _ -> }
                    throw error
                }
            }.exceptionOrNull(),
        )
        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertFalse(FeaturePrefsOwner.current().enabled)
    }

    @Test
    fun failedOldVisualDeliveryDoesNotRetireReentrantNewBinding() {
        val old = LockedPrefs()
        val next = LockedPrefs().apply { dual = true }
        val error = IllegalStateException("visual-old")
        assertSame(
            error,
            runCatching {
                VisualPrefsOwner.bind(old) {
                    VisualPrefsOwner.bind(next) { }
                    throw error
                }
            }.exceptionOrNull(),
        )
        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertTrue(VisualPrefsOwner.current().dualSignal)
    }

    @Test
    fun failedOldDiagnosticsDeliveryDoesNotRetireReentrantNewBinding() {
        val old = LockedPrefs()
        val next = LockedPrefs().apply { detailed = true }
        val error = IllegalStateException("diagnostics-old")
        var delivered = 0
        assertSame(
            error,
            runCatching {
                DiagPrefsOwner.bind(old, forceDetailed = false) {
                    DiagPrefsOwner.bind(next, forceDetailed = false) { delivered++ }
                    throw error
                }
            }.exceptionOrNull(),
        )
        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertTrue(DiagPrefsOwner.isBound)
        assertEquals(1, delivered)
        next.detailed = false
        next.notifyListeners(DIAG_LEVEL_KEY)
        assertEquals(2, delivered)
    }

    @Test
    fun diagnosticsChangedBeforeListenerRegistrationIsRead() {
        val prefs = LockedPrefs().apply {
            onRegister = { detailed = true }
        }
        val delivered = mutableListOf<Boolean>()
        val bound = DiagPrefsOwner.bind(prefs, forceDetailed = false) { delivered += it }
        assertTrue(bound.detailed)
        assertEquals(true, delivered.last())
    }

    private fun testStaleRead(
        old: LockedPrefs,
        key: String,
        setOld: () -> Unit,
        rebind: () -> Unit,
    ) {
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        old.pauseRead(key, entered, resume)
        setOld()
        val error = AtomicReference<Throwable?>(null)
        val callback = worker(error) { old.notifyListeners(key) }
        try {
            assertTrue("old read started", entered.await(3, TimeUnit.SECONDS))
            rebind()
        } finally {
            resume.countDown()
        }
        callback.join(3_000)
        assertFalse("callback finished", callback.isAlive)
        assertNull(error.get())
    }

    private fun testListenerLock(key: String, bind: (SharedPreferences) -> Unit) {
        val old = LockedPrefs()
        val next = LockedPrefs()
        bind(old)
        val held = CountDownLatch(1)
        val proceed = CountDownLatch(1)
        val unregister = CountDownLatch(1)
        old.beforeUnregister = { unregister.countDown() }
        val error = AtomicReference<Throwable?>(null)
        val callback = worker(error) { old.notifyHoldingLock(key, held, proceed) }
        val rebind = try {
            assertTrue("listener lock held", held.await(3, TimeUnit.SECONDS))
            worker(error) { bind(next) }.also {
                assertTrue("unregister entered", unregister.await(3, TimeUnit.SECONDS))
            }
        } finally {
            proceed.countDown()
        }
        callback.join(3_000)
        rebind.join(3_000)
        assertFalse("callback finished", callback.isAlive)
        assertFalse("rebind finished", rebind.isAlive)
        assertNull(error.get())
        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
    }

    private fun worker(error: AtomicReference<Throwable?>, work: () -> Unit) =
        thread(isDaemon = true) {
            try {
                work()
            } catch (e: Throwable) {
                error.compareAndSet(null, e)
            }
        }

    private class LockedPrefs : SharedPreferences {
        private val lock = Any()
        private val listeners = linkedSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
        @Volatile var dual = false
        @Volatile var featureEnabled = true
        @Volatile var detailed = false
        @Volatile var onRegister: (() -> Unit)? = null
        @Volatile var beforeUnregister: (() -> Unit)? = null
        @Volatile private var pause: Pause? = null

        private data class Pause(val key: String, val entered: CountDownLatch, val resume: CountDownLatch)

        val listenerCount: Int
            get() = synchronized(lock) { listeners.size }

        fun pauseRead(key: String, entered: CountDownLatch, resume: CountDownLatch) {
            pause = Pause(key, entered, resume)
        }

        fun notifyListeners(key: String) {
            val copy = synchronized(lock) { listeners.toList() }
            copy.forEach { it.onSharedPreferenceChanged(this, key) }
        }

        fun notifyHoldingLock(key: String, held: CountDownLatch, proceed: CountDownLatch) {
            synchronized(lock) {
                held.countDown()
                check(proceed.await(3, TimeUnit.SECONDS)) { "callback not released" }
                listeners.forEach { it.onSharedPreferenceChanged(this, key) }
            }
        }

        private fun awaitRead(key: String?) {
            val pending = pause ?: return
            if (pending.key != key) return
            pause = null
            pending.entered.countDown()
            check(pending.resume.await(3, TimeUnit.SECONDS)) { "read not resumed" }
        }

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            onRegister?.invoke()
            if (listener != null) synchronized(lock) { listeners.add(listener) }
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            beforeUnregister?.invoke()
            if (listener != null) synchronized(lock) { listeners.remove(listener) }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            awaitRead(key)
            return when (key) {
                DUAL_SIGNAL_ENABLED_KEY -> dual
                FEATURE_ENABLED_KEY -> featureEnabled
                else -> defValue
            }
        }

        override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any>()
        override fun getString(key: String?, defValue: String?): String? =
            if (key == DIAG_LEVEL_KEY) {
                if (detailed) "Detailed" else "General"
            } else {
                defValue
            }
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            defValues
        override fun getInt(key: String?, defValue: Int): Int = defValue
        override fun getLong(key: String?, defValue: Long): Long = defValue
        override fun getFloat(key: String?, defValue: Float): Float = defValue
        override fun contains(key: String?): Boolean = false
        override fun edit(): SharedPreferences.Editor =
            throw UnsupportedOperationException("Read only")
    }
}
