package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DIAG_LEVEL_KEY
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
    fun bindPropagatesDetail() {
        val prefs = FakePreferences()
        val observed = mutableListOf<Boolean>()

        val result =
            DiagPrefsOwner.bind(
                prefs = prefs,
                forceDetailed = false,
                onChanged = observed::add,
            )

        assertFalse(result.detailed)
        assertTrue(DiagPrefsOwner.isBound)
        assertEquals(1, prefs.listenerCount)
        assertEquals(listOf(false), observed)

        prefs.setLevel(DiagLevel.Detailed.name)

        assertEquals(listOf(false, true), observed)
    }

    @Test
    fun unbindRejectsStaleCallback() {
        val prefs = FakePreferences()
        val observed = mutableListOf<Boolean>()

        DiagPrefsOwner.bind(
            prefs = prefs,
            forceDetailed = false,
            onChanged = observed::add,
        )
        val staleListener = prefs.lastListener

        DiagPrefsOwner.unbind()

        assertFalse(DiagPrefsOwner.isBound)
        assertEquals(0, prefs.listenerCount)

        prefs.setRaw(DiagLevel.Detailed.name)
        staleListener?.onSharedPreferenceChanged(
            prefs,
            DIAG_LEVEL_KEY,
        )

        assertEquals(listOf(false), observed)
    }

    @Test
    fun devProbeForcesDetail() {
        val prefs = FakePreferences()
        val observed = mutableListOf<Boolean>()

        val result =
            DiagPrefsOwner.bind(
                prefs = prefs,
                forceDetailed = true,
                onChanged = observed::add,
            )

        assertTrue(result.detailed)
        assertEquals(listOf(true), observed)

        prefs.setLevel(DiagLevel.General.name)

        assertEquals(listOf(true, true), observed)
    }

    private class FakePreferences : SharedPreferences {
        private var level: String = DiagLevel.General.name
        private val listeners =
            linkedSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        var lastListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
            private set

        val listenerCount: Int
            get() = listeners.size

        fun setLevel(value: String) {
            level = value
            listeners.toList().forEach { listener ->
                listener.onSharedPreferenceChanged(
                    this,
                    DIAG_LEVEL_KEY,
                )
            }
        }

        fun setRaw(value: String) {
            level = value
        }

        override fun getString(
            key: String?,
            defValue: String?,
        ): String? =
            if (key == DIAG_LEVEL_KEY) {
                level
            } else {
                defValue
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

        override fun contains(key: String?): Boolean = key == DIAG_LEVEL_KEY

        override fun edit(): SharedPreferences.Editor =
            throw UnsupportedOperationException("not needed by this test")
    }
}
