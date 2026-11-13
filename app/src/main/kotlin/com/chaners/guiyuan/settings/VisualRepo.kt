package com.chaners.guiyuan.settings

import android.content.Context
import kotlinx.coroutines.flow.Flow

internal class VisualRepo(context: Context) {
    private val context = context.applicationContext
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateVisualPrefsIfNeeded(preferences)
        retireLegacyIdleActivityPreference(preferences)
    }

    val settings: Flow<VisualCfg> =
        observePreferenceChanges(preferences, ::isVisualPreferenceKey, ::current)

    fun current(): VisualCfg = preferences.readVisualCfg()

    private fun activeProfileKey(baseKey: String): String =
        visualProfileKey(preferences.readLayout(), baseKey)

    private fun writeProfileBoolean(key: String, enabled: Boolean) {
        preferences.edit().putBoolean(activeProfileKey(key), enabled).apply()
    }

    private fun writeProfileFloat(key: String, value: Float) {
        preferences.edit().putFloat(activeProfileKey(key), value).apply()
    }

    private fun writeProfileInt(key: String, value: Int) {
        preferences.edit().putInt(activeProfileKey(key), value).apply()
    }

    private fun writeProfileString(key: String, value: String) {
        preferences.edit().putString(activeProfileKey(key), value).apply()
    }

    fun setDualSignal(enabled: Boolean) {
        preferences.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, enabled)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .apply()
    }

    fun setDualRowDotArrangement(style: SignalDotArrangement) {
        writeProfileString(DUAL_ROW_DOT_ARRANGEMENT_KEY, style.persistedValue)
    }

    fun setBothSidesShowActivity(enabled: Boolean) {
        writeProfileBoolean(BOTH_SIDES_ACTIVITY_KEY, enabled)
    }

    fun setBothSidesHideIdleActivity(enabled: Boolean) {
        writeProfileBoolean(BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY, enabled)
    }

    fun setBothSidesBottomCharging(enabled: Boolean) {
        writeProfileBoolean(BOTH_SIDES_CHARGING_KEY, enabled)
    }

    fun setBothSidesActivityScale(scale: Float) {
        writeProfileFloat(BOTH_SIDES_ACTIVITY_SCALE_KEY, scale.finiteIn(
            BOTH_SIDES_BOTTOM_INFO_SCALE_MIN,
            BOTH_SIDES_BOTTOM_INFO_SCALE_MAX,
            BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT,
        ))
    }

    fun setBothSidesActivityOffsetY(offset: Float) {
        writeProfileFloat(BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY, offset.normalizedNetworkOffset())
    }

    fun setBothSidesChargingOffsetY(offset: Float) {
        writeProfileFloat(BOTH_SIDES_CHARGING_OFFSET_Y_KEY, offset.normalizedNetworkOffset())
    }

    fun setSingleRowDotArrangement(style: SignalDotArrangement) {
        require(style != SignalDotArrangement.BOTH_SIDES) {
            "Two-sided signal dots require dual-row mode"
        }
        writeProfileString(SINGLE_ROW_DOT_STYLE_KEY, style.persistedValue)
    }

    fun setFixedLowerOpening(enabled: Boolean) {
        writeProfileBoolean(LOWER_FIXED_OPENING_KEY, enabled)
    }

    fun setMobileTypeBottomOffsetY(offset: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY),
                offset.finiteIn(
                    NETWORK_TOP_OFFSET_MIN,
                    NETWORK_TOP_OFFSET_MAX,
                    mobileTypeBottomOffsetDefault(preferences.readLayout()),
                ),
            ).apply()
    }

    fun setLayout(layout: ContentLayout) {
        preferences
            .edit()
            .putString(CONTENT_LAYOUT_KEY, layout.persistedValue)
            .apply()
    }

    fun setMobileFollowsBatteryColor(enabled: Boolean) {
        writeProfileBoolean(MOBILE_FOLLOWS_BATTERY_COLOR_KEY, enabled)
    }

    fun setCenterFollowsBatteryColor(enabled: Boolean) {
        writeProfileBoolean(CENTER_FOLLOWS_BATTERY_COLOR_KEY, enabled)
    }

    fun setTopReadout(enabled: Boolean) {
        val layout = preferences.readLayout()
        val dual = preferences.dualSignalEnabled()
        val readoutKey = visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY)
        val offsetKey = visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY)
        val editor = preferences.edit().putBoolean(readoutKey, enabled)
        if (
            layout == ContentLayout.BATTERY_CENTER &&
            enabled &&
            !preferences.profileBoolean(layout, BATTERY_TOP_READOUT_ENABLED_KEY, false) &&
            !preferences.contains(offsetKey) &&
            !preferences.contains(visualModeKey(dual, BATTERY_TOP_VERTICAL_OFFSET_KEY))
        ) {
            editor.putFloat(offsetKey, topOffsetYRaw(5f))
        }
        editor.apply()
    }

    fun setTopTextFollowsBatteryColor(enabled: Boolean) {
        writeProfileBoolean(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY, enabled)
    }

    fun setChargingIcon(enabled: Boolean) {
        writeProfileBoolean(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY, enabled)
    }

    fun setTopChargingIconFollowsBatteryColor(enabled: Boolean) {
        writeProfileBoolean(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY, enabled)
    }

    fun setFillFollowsRetract(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, enabled)
            .apply()
    }

    fun setCcTintTransition(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, enabled)
            .apply()
    }

    fun setTopTextScale(scale: Float) {
        val uiScale =
            scale.finiteIn(
                BATTERY_TOP_TEXT_UI_SCALE_MIN,
                BATTERY_TOP_TEXT_UI_SCALE_MAX,
                topTextUiScaleDefault(preferences.readLayout()),
            )
        writeProfileFloat(
            BATTERY_TOP_TEXT_SCALE_KEY,
            (uiScale * BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
                .coerceIn(BATTERY_TOP_TEXT_SCALE_MIN, BATTERY_TOP_TEXT_SCALE_MAX),
        )
    }

    fun setTopTextWeight(weight: Int) {
        writeProfileInt(
            BATTERY_TOP_TEXT_WEIGHT_KEY,
            weight.coerceIn(BATTERY_TOP_TEXT_WEIGHT_MIN, BATTERY_TOP_TEXT_WEIGHT_MAX),
        )
    }

    fun setTopOffsetY(offset: Float) {
        val uiOffset =
            offset.finiteIn(
                BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
                0f,
            )
        writeProfileFloat(BATTERY_TOP_VERTICAL_OFFSET_KEY, topOffsetYRaw(uiOffset))
    }

    fun setWifiOffsetY(offset: Float) {
        writeProfileFloat(WIFI_VERTICAL_OFFSET_KEY, offset.normalizedNetworkOffset())
    }

    fun setMobileTypeOffsetY(offset: Float) {
        writeProfileFloat(MOBILE_TYPE_VERTICAL_OFFSET_KEY, offset.normalizedNetworkOffset())
    }

    fun setSignalDotOffsetY(offset: Float) {
        preferences.writeSignalDotOffsetY(offset)
    }

    fun setCombinedScale(scale: Float) {
        writeProfileFloat(COMBINED_SCALE_KEY, scale.finiteIn(
            COMBINED_SCALE_MIN, COMBINED_SCALE_MAX, COMBINED_SCALE_DEFAULT,
        ))
    }

    fun setOuterWeightScale(scale: Float) {
        writeProfileFloat(OUTER_WEIGHT_SCALE_KEY, scale.finiteIn(
            OUTER_WEIGHT_SCALE_MIN, OUTER_WEIGHT_SCALE_MAX, OUTER_WEIGHT_SCALE_DEFAULT,
        ))
    }

    fun setWifiWeight(weight: Int) {
        writeProfileInt(WIFI_WEIGHT_KEY, weight.coerceIn(WIFI_WEIGHT_MIN, WIFI_WEIGHT_MAX))
    }

    fun setWifiScale(scale: Float) {
        writeProfileFloat(WIFI_SIZE_SCALE_KEY, scale.finiteIn(
            WIFI_SIZE_SCALE_MIN, WIFI_SIZE_SCALE_MAX, WIFI_SIZE_SCALE_DEFAULT,
        ))
    }

    fun setAirplaneWeight(weight: Int) {
        writeProfileInt(AIRPLANE_WEIGHT_KEY, weight.coerceIn(AIRPLANE_WEIGHT_MIN, AIRPLANE_WEIGHT_MAX))
    }

    fun setAirplaneScale(scale: Float) {
        writeProfileFloat(AIRPLANE_SIZE_SCALE_KEY, scale.finiteIn(
            AIRPLANE_SIZE_SCALE_MIN, AIRPLANE_SIZE_SCALE_MAX, AIRPLANE_SIZE_SCALE_DEFAULT,
        ))
    }

    fun setNoSimScale(scale: Float) {
        writeProfileFloat(NO_SIM_SIZE_SCALE_KEY, scale.finiteIn(
            NO_SIM_SIZE_SCALE_MIN, NO_SIM_SIZE_SCALE_MAX, NO_SIM_SIZE_SCALE_DEFAULT,
        ))
    }

    fun setMobileTypeScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(MOBILE_TYPE_SIZE_SCALE_KEY),
                scale.finiteIn(
                    MOBILE_TYPE_SIZE_SCALE_MIN,
                    MOBILE_TYPE_SIZE_SCALE_MAX,
                    mobileTypeScaleDefault(preferences.readLayout()),
                ),
            )
            .apply()
    }

    fun setMobileTypeWeight(weight: Int) {
        writeProfileInt(
            MOBILE_TYPE_WEIGHT_KEY,
            weight.coerceIn(MOBILE_TYPE_WEIGHT_MIN, MOBILE_TYPE_WEIGHT_MAX),
        )
    }

    fun backup(): VisualBackup {
        val layout = preferences.readLayout()
        return VisualBackup(
            singleLayout = layout,
            dualLayout = layout,
            dualEnabled = preferences.dualSignalEnabled(),
            dualInitialized = preferences.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false),
            profiles = listOf(false, true).flatMap { dual ->
                ContentLayout.entries.map { layout ->
                    preferences.readVisualCfg(dual = dual, layoutOverride = layout)
                }
            },
        )
    }

    fun backupSnapshot(): Map<String, Any?> = preferences.backupVisualSnapshot()

    fun rollbackBackup(snapshot: Map<String, Any?>): Boolean =
        preferences.rollbackBackupVisuals(snapshot)

    fun restore(backup: VisualBackup, library: BatteryColorSchemeLibrary): Boolean {
        val editor = preferences.editBackupVisuals()
        backup.profiles.filter { it.dualSignal == backup.dualEnabled }
            .forEach { editor.putVisualCfg(it) }
        val active = backup.profiles.first { it.dualSignal == backup.dualEnabled }
        editor.putBatteryColors(active)
        editor.putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, active.fillFollowsRetract)
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, active.ccTintTransition)
        val applied = library.matchingKey(active) ?: library.activeSchemeKey
        writeBatterySchemeLibrary(editor, library.copy(activeSchemeKey = applied))
        val activeLayout = if (backup.dualEnabled) backup.dualLayout else backup.singleLayout
        editor.putString(CONTENT_LAYOUT_KEY, activeLayout.persistedValue)
        // Shared visual profiles require no separate dual-bank initialization.
        // Even a valid legacy backup with dualInitialized=false must be ready
        // for toggling dual mode and exporting a new backup.
        editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        editor.putBoolean(DUAL_SIGNAL_ENABLED_KEY, backup.dualEnabled)
        editor.putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        editor.putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
        // Restored physical charging scales use the current reference.
        return editor.commit()
    }

    fun resetToDefaults() {
        resetVisualPreferences(preferences, BatterySchemeRepo(context).current())
    }

    fun setTopChargingIconScale(scale: Float) {
        val uiScale =
            scale.finiteIn(
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
                topChargingIconUiScaleDefault(preferences.readLayout()),
            )
        writeProfileFloat(
            BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
            (uiScale * BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
                .coerceIn(
                    BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
                    BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
                ),
        )
    }
}
