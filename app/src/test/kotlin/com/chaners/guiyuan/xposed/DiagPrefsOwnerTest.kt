package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.guiyuan.settings.DiagLevel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagPrefsOwnerTest {
    @After
    fun tearDown() {
        DiagPrefsOwner.unbind()
    }

    @Test
    fun bindOwnsListenerAndPropagatesDetailedChanges() {
        val preferences = FakePreferences()
        val observed = mutableListOf<Boolean>()

        val result =
            DiagPrefsOwner.bind(
                preferences = preferences,
                forceDetailed = false,
                onDetailedChanged = observed::add,
            )

        assertFalse(result.detailedEnabled)
        assertTrue(DiagPrefsOwner.isBound)
        assertEquals(1, preferences.listenerCount)
        assertEquals(listOf(false), observed)

        preferences.setDiagnosticsLevel(DiagLevel.Detailed.name)

        assertEquals(listOf(false, true), observed)
    }

    @Test
    fun unbindUnregistersAndRejectsStaleCallbacks() {
        val preferences = FakePreferences()
        val observed = mutableListOf<Boolean>()

        DiagPrefsOwner.bind(
            preferences = preferences,
            forceDetailed = false,
            onDetailedChanged = observed::add,
        )
        val staleListener = preferences.lastRegisteredListener

        DiagPrefsOwner.unbind()

        assertFalse(DiagPrefsOwner.isBound)
        assertEquals(0, preferences.listenerCount)

        preferences.setRawDiagnosticsLevel(DiagLevel.Detailed.name)
        staleListener?.onSharedPreferenceChanged(
            preferences,
            DIAGNOSTICS_LEVEL_KEY,
        )

        assertEquals(listOf(false), observed)
    }

    @Test
    fun developmentProbeForcesDetailedState() {
        val preferences = FakePreferences()
        val observed = mutableListOf<Boolean>()

        val result =
            DiagPrefsOwner.bind(
                preferences = preferences,
                forceDetailed = true,
                onDetailedChanged = observed::add,
            )

        assertTrue(result.detailedEnabled)
        assertEquals(listOf(true), observed)

        preferences.setDiagnosticsLevel(DiagLevel.General.name)

        assertEquals(listOf(true, true), observed)
    }

    private class FakePreferences : SharedPreferences {
        private var diagnosticsLevel: String = DiagLevel.General.name
        private val listeners =
            linkedSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        var lastRegisteredListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
            private set

        val listenerCount: Int
            get() = listeners.size

        fun setDiagnosticsLevel(level: String) {
            diagnosticsLevel = level
            listeners.toList().forEach { listener ->
                listener.onSharedPreferenceChanged(
                    this,
                    DIAGNOSTICS_LEVEL_KEY,
                )
            }
        }

        fun setRawDiagnosticsLevel(level: String) {
            diagnosticsLevel = level
        }

        override fun getString(
            key: String?,
            defValue: String?,
        ): String? =
            if (key == DIAGNOSTICS_LEVEL_KEY) {
                diagnosticsLevel
            } else {
                defValue
            }

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) {
                listeners += listener
                lastRegisteredListener = listener
            }
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) {
                listeners -= listener
            }
        }

        override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any?>()

        override fun getStringSet(
            key: String?,
            defValues: MutableSet<String>?,
        ): MutableSet<String>? = defValues

        override fun getInt(
            key: String?,
            defValue: Int,
        ): Int = defValue

        override fun getLong(
            key: String?,
            defValue: Long,
        ): Long = defValue

        override fun getFloat(
            key: String?,
            defValue: Float,
        ): Float = defValue

        override fun getBoolean(
            key: String?,
            defValue: Boolean,
        ): Boolean = defValue

        override fun contains(key: String?): Boolean = key == DIAGNOSTICS_LEVEL_KEY

        override fun edit(): SharedPreferences.Editor =
            throw UnsupportedOperationException("not needed by this test")
    }
}
