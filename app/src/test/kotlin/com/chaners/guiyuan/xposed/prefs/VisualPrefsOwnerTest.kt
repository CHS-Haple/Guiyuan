package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DUAL_SIGNAL_ENABLED_KEY
import com.chaners.guiyuan.settings.NativeFontAxis
import com.chaners.guiyuan.settings.VisualCfg
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class VisualPrefsOwnerTest {
    @After
    fun tearDown() {
        VisualPrefsOwner.unbind()
    }

    @Test
    fun previousBindingCannotOverwriteCurrentConfiguration() {
        val old = FakePreferences(dual = false)
        val next = FakePreferences(dual = true)
        val observed = mutableListOf<VisualCfg>()

        VisualPrefsOwner.bind(old, observed::add)
        val stale = old.lastListener
        VisualPrefsOwner.bind(next, observed::add)

        assertEquals(0, old.listenerCount)
        assertEquals(1, next.listenerCount)
        assertEquals(listOf(false, true), observed.map { it.dualSignal })

        old.setDual(true)
        stale?.onSharedPreferenceChanged(old, DUAL_SIGNAL_ENABLED_KEY)

        assertEquals(true, VisualPrefsOwner.current().dualSignal)
        assertEquals(listOf(false, true), observed.map { it.dualSignal })
    }

    @Test
    fun unbindRejectsLateCallbacks() {
        val prefs = FakePreferences(dual = true)
        val observed = mutableListOf<VisualCfg>()
        VisualPrefsOwner.bind(prefs, observed::add)
        val stale = prefs.lastListener

        VisualPrefsOwner.unbind()
        prefs.setDual(false)
        stale?.onSharedPreferenceChanged(prefs, DUAL_SIGNAL_ENABLED_KEY)

        assertEquals(0, prefs.listenerCount)
        assertEquals(VisualCfg(), VisualPrefsOwner.current())
        assertEquals(listOf(true), observed.map { it.dualSignal })
    }

    @Test
    fun unchangedAndUnrelatedNotificationsDoNotEmit() {
        val prefs = FakePreferences(dual = false)
        val observed = mutableListOf<VisualCfg>()
        VisualPrefsOwner.bind(prefs, observed::add)

        prefs.notify("unrelated")
        prefs.notify(DUAL_SIGNAL_ENABLED_KEY)
        assertEquals(listOf(false), observed.map { it.dualSignal })

        prefs.setDual(true)
        prefs.notify(DUAL_SIGNAL_ENABLED_KEY)
        assertEquals(listOf(false, true), observed.map { it.dualSignal })
    }

    @Test
    fun fontAxisRequestOnlyNotifiesTheBoundGeneration() {
        val old = FakePreferences(dual = false)
        val next = FakePreferences(dual = false)
        val seen = mutableListOf<VisualCfg>()
        var requests = 0
        VisualPrefsOwner.bind(old, onAxisRequest = { requests++ }, onChanged = seen::add)

        old.requestAxis(100L)
        old.notify(NativeFontAxis.REQUEST_KEY)
        assertEquals(1, requests)
        assertEquals(1, seen.size)

        val stale = old.lastListener
        VisualPrefsOwner.bind(next, onAxisRequest = { requests++ }, onChanged = seen::add)
        old.requestAxis(101L)
        stale?.onSharedPreferenceChanged(old, NativeFontAxis.REQUEST_KEY)
        assertEquals(1, requests)

        next.requestAxis(102L)
        assertEquals(2, requests)
        assertEquals(2, seen.size)
    }

    private class FakePreferences(private var dual: Boolean) : SharedPreferences {
        private val listeners =
            linkedSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        var lastListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
        private var requestId = 0L

        fun requestAxis(id: Long) {
            requestId = id
            notify(NativeFontAxis.REQUEST_KEY)
        }
            private set

        val listenerCount: Int
            get() = listeners.size

        fun setDual(value: Boolean) {
            dual = value
            notify(DUAL_SIGNAL_ENABLED_KEY)
        }

        fun notify(key: String?) {
            listeners.toList().forEach { it.onSharedPreferenceChanged(this, key) }
        }

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) {
                listeners += listener
                lastListener = listener
            }
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) listeners -= listener
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            if (key == DUAL_SIGNAL_ENABLED_KEY) dual else defValue

        override fun getAll(): MutableMap<String, *> =
            mutableMapOf<String, Any>(DUAL_SIGNAL_ENABLED_KEY to dual)

        override fun contains(key: String?): Boolean = key == DUAL_SIGNAL_ENABLED_KEY
        override fun getString(key: String?, defValue: String?): String? = defValue
        override fun getStringSet(
            key: String?,
            defValues: MutableSet<String>?,
        ): MutableSet<String>? = defValues

        override fun getInt(key: String?, defValue: Int): Int = defValue
        override fun getLong(key: String?, defValue: Long): Long =
            if (key == NativeFontAxis.REQUEST_KEY) requestId else defValue
        override fun getFloat(key: String?, defValue: Float): Float = defValue
        override fun edit(): SharedPreferences.Editor =
            throw UnsupportedOperationException("Not required")
    }
}
