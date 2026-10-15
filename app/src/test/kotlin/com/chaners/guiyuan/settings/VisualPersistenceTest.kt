package com.chaners.guiyuan.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualPersistenceTest {
    @Test
    fun singleAndDualKeepIndependentLayoutsSizesAndColors() {
        val prefs = MemoryPreferences()
        val single = VisualCfg(
            combinedScale = 0.9f,
            outerWeightScale = 1.1f,
            wifiOffsetY = -3f,
            mobileTypeOffsetY = 6f,
            batteryColorPreset = BatteryColorPreset.RECOMMENDED,
            batteryColorModes = BatteryColorModes(charging = BatteryColorMode.CUSTOM),
            batteryColorOverrides = BatteryColorOverrides(charging = 0xFF123456.toInt()),
        )
        val dual = VisualCfg(
            layout = ContentLayout.BATTERY_CENTER,
            dualSignal = true,
            combinedScale = 1.2f,
            outerWeightScale = 0.8f,
            wifiOffsetY = 4f,
            mobileTypeOffsetY = -2f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
            batteryColorModes = BatteryColorModes(powerSave = BatteryColorMode.FOLLOW_SYSTEM),
        )
        prefs.edit().putVisualCfg(single).apply()
        prefs.edit().putVisualCfg(dual).apply()

        assertEquals(dual.normalized(), prefs.readVisualCfg())
        assertEquals(single.normalized(), prefs.readVisualCfg(dual = false))
        assertEquals(ContentLayout.NETWORK_CENTER, prefs.readLayout(dual = false))
        assertEquals(ContentLayout.BATTERY_CENTER, prefs.readLayout(dual = true))
        assertEquals(
            single.batteryColorOverrides.charging,
            prefs.readVisualCfg(dual = false).batteryColorOverrides.charging,
        )
        assertEquals(
            BatteryColorMode.FOLLOW_SYSTEM,
            prefs.readVisualCfg(dual = true).batteryColorModes.powerSave,
        )

        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertEquals(single.normalized(), prefs.readVisualCfg())
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        assertEquals(dual.normalized(), prefs.readVisualCfg())
    }

    @Test
    fun remoteMirrorPreservesBothBanksWhenSwitchingModes() {
        val local = MemoryPreferences()
        val remote = MemoryPreferences()
        val single = VisualCfg(combinedScale = 0.8f, batteryColorPreset = BatteryColorPreset.RECOMMENDED)
        val dual = VisualCfg(
            dualSignal = true,
            combinedScale = 1.2f,
            batteryColorPreset = BatteryColorPreset.IOS_STYLE,
        )
        local.edit().putVisualCfg(single).apply()
        local.edit().putVisualCfg(dual).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(single.normalized(), remote.readVisualCfg())
        assertEquals(dual.normalized(), remote.readVisualCfg(dual = true))

        local.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, true).apply()
        remote.edit().putVisualCfg(local.readVisualCfg()).apply()
        assertEquals(dual.normalized(), remote.readVisualCfg())
        assertEquals(single.normalized(), remote.readVisualCfg(dual = false))
    }

    @Test
    fun missingDualKeysCannotBorrowLegacySingleValues() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putFloat(COMBINED_SCALE_KEY, 0.75f)
            .putFloat(WIFI_VERTICAL_OFFSET_KEY, -3f)
            .putString(BATTERY_COLOR_PRESET_KEY, BatteryColorPreset.IOS_STYLE.persistedValue)
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, true)
            .apply()
        assertEquals(COMBINED_SCALE_DEFAULT, prefs.readVisualCfg().combinedScale, 0.0001f)
        assertEquals(0f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(-3f, prefs.readVisualCfg(dual = false).wifiOffsetY, 0f)
        assertEquals(BatteryColorPreset.HYPEROS, prefs.readVisualCfg().batteryColorPreset)
        assertEquals(0.75f, prefs.readVisualCfg(dual = false).combinedScale, 0.0001f)
        assertEquals(BatteryColorPreset.IOS_STYLE, prefs.readVisualCfg(dual = false).batteryColorPreset)
        assertTrue(prefs.dualSignalEnabled())
        prefs.edit().putBoolean(DUAL_SIGNAL_ENABLED_KEY, false).apply()
        assertFalse(prefs.dualSignalEnabled())
    }

    @Test
    fun oldBatteryCenteredOffsetIsInheritedUntilEachNewSliderIsSaved() {
        val prefs = MemoryPreferences()
        val layout = ContentLayout.BATTERY_CENTER
        prefs.edit()
            .putString(visualModeKey(false, CONTENT_LAYOUT_KEY), layout.persistedValue)
            .putFloat(visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY), topOffsetYRaw(5f))
            .apply()

        val initial = prefs.readVisualCfg()
        assertEquals(5f, initial.wifiOffsetY, 0f)
        assertEquals(5f, initial.mobileTypeOffsetY, 0f)
        assertEquals(topOffsetYRaw(5f), initial.topOffsetY, 0f)

        prefs.edit()
            .putFloat(visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY), -1f)
            .apply()
        assertEquals(-1f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(5f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)

        val result = prefs.readVisualCfg().copy(mobileTypeOffsetY = 3f)
        prefs.edit().putVisualCfg(result).apply()
        assertEquals(-1f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(3f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)
    }

    @Test
    fun legacyNetworkCenteredReadoutDoesNotAffectNewNetworkOffsets() {
        val prefs = MemoryPreferences()
        prefs.edit()
            .putFloat(visualProfileKey(ContentLayout.NETWORK_CENTER, BATTERY_TOP_VERTICAL_OFFSET_KEY), topOffsetYRaw(6f))
            .apply()
        assertEquals(0f, prefs.readVisualCfg().wifiOffsetY, 0f)
        assertEquals(0f, prefs.readVisualCfg().mobileTypeOffsetY, 0f)
        assertEquals(topOffsetYRaw(6f), prefs.readVisualCfg().topOffsetY, 0f)
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(
            key: String?,
            defValues: MutableSet<String>?,
        ): MutableSet<String>? = (values[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long =
            values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float =
            values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun edit(): SharedPreferences.Editor = Editor()

        private inner class Editor : SharedPreferences.Editor {
            private val changes = linkedMapOf<String, Any?>()
            private var clearFirst = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor =
                put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
                put(key, values?.toSet())
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
                put(key, value)
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
                put(key, value)
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
                put(key, value)
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
                put(key, value)
            override fun remove(key: String?): SharedPreferences.Editor = put(key, null)

            override fun clear(): SharedPreferences.Editor {
                clearFirst = true
                return this
            }

            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun commit(): Boolean {
                if (clearFirst) values.clear()
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
