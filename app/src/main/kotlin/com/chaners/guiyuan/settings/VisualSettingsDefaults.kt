package com.chaners.guiyuan.settings

import android.content.SharedPreferences
import kotlin.math.abs

internal const val COMBINED_STATUS_VISUAL_PREFS_NAME = "combined_status_visual"
internal const val CONTENT_LAYOUT_KEY = "content_layout"
internal const val MOBILE_FOLLOWS_BATTERY_COLOR_KEY = "mobile_follows_battery_color"
internal const val CENTER_FOLLOWS_BATTERY_COLOR_KEY = "center_follows_battery_color"
internal const val TOP_READOUT_KEY = "battery_top_readout_enabled"
internal const val TOP_TEXT_FOLLOWS_BATTERY_KEY =
    "battery_top_text_follows_battery_color"
internal const val CHARGING_ICON_ENABLED_KEY =
    "battery_top_charging_icon_enabled"
internal const val CHARGING_ICON_FOLLOWS_BATTERY_KEY =
    "battery_top_charging_icon_follows_battery_color"
internal const val FILL_FOLLOWS_RETRACT_KEY =
    "battery_fill_follows_retract_endpoint"
internal const val CC_TINT_TRANSITION_KEY =
    "control_center_tint_transition_enabled"
internal const val TOP_TEXT_SCALE_KEY = "battery_top_text_scale"
internal const val TOP_TEXT_WEIGHT_KEY = "battery_top_text_weight"
internal const val TOP_OFFSET_KEY = "battery_top_vertical_offset"
internal const val CHARGING_ICON_SCALE_KEY = "battery_top_charging_icon_scale"
internal const val COMBINED_SCALE_KEY = "combined_scale"
internal const val OUTER_WEIGHT_SCALE_KEY = "outer_weight_scale"
internal const val WIFI_SIZE_SCALE_KEY = "wifi_size_scale"
internal const val AIRPLANE_SIZE_SCALE_KEY = "airplane_size_scale"
internal const val NO_SIM_SIZE_SCALE_KEY = "no_sim_size_scale"
internal const val MOBILE_TYPE_SIZE_SCALE_KEY = "mobile_type_size_scale"
internal const val MOBILE_TYPE_WEIGHT_KEY = "mobile_type_weight"
internal const val BATTERY_COLOR_PRESET_KEY = "battery_color_preset"
internal const val BATTERY_COLOR_MODE_NORMAL_KEY = "battery_color_mode_normal"
internal const val BATTERY_COLOR_MODE_POWER_SAVE_KEY = "battery_color_mode_power_save"
internal const val BATTERY_COLOR_MODE_PERFORMANCE_KEY = "battery_color_mode_performance"
internal const val BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY = "battery_color_mode_super_power_save"
internal const val BATTERY_COLOR_MODE_CHARGING_KEY = "battery_color_mode_charging"
internal const val BATTERY_COLOR_MODE_LOW_KEY = "battery_color_mode_low"
internal const val BATTERY_COLOR_NORMAL_KEY = "battery_color_normal"
internal const val BATTERY_COLOR_POWER_SAVE_KEY = "battery_color_power_save"
internal const val BATTERY_COLOR_PERFORMANCE_KEY = "battery_color_performance"
internal const val BATTERY_COLOR_SUPER_POWER_SAVE_KEY = "battery_color_super_power_save"
internal const val BATTERY_COLOR_CHARGING_KEY = "battery_color_charging"
internal const val BATTERY_COLOR_LOW_KEY = "battery_color_low"
internal const val RUNTIME_REMOTE_PREFS_NAME = "CombinedStatusRuntimeConfig"

internal val PROFILE_VISUAL_BASE_KEYS =
    setOf(
        MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
        CENTER_FOLLOWS_BATTERY_COLOR_KEY,
        TOP_READOUT_KEY,
        TOP_TEXT_FOLLOWS_BATTERY_KEY,
        CHARGING_ICON_ENABLED_KEY,
        CHARGING_ICON_FOLLOWS_BATTERY_KEY,
        TOP_TEXT_SCALE_KEY,
        TOP_TEXT_WEIGHT_KEY,
        TOP_OFFSET_KEY,
        CHARGING_ICON_SCALE_KEY,
        COMBINED_SCALE_KEY,
        OUTER_WEIGHT_SCALE_KEY,
        WIFI_SIZE_SCALE_KEY,
        AIRPLANE_SIZE_SCALE_KEY,
        NO_SIM_SIZE_SCALE_KEY,
        MOBILE_TYPE_SIZE_SCALE_KEY,
        MOBILE_TYPE_WEIGHT_KEY,
    )

internal val GLOBAL_VISUAL_KEYS =
    setOf(
        FILL_FOLLOWS_RETRACT_KEY,
        CC_TINT_TRANSITION_KEY,
        BATTERY_COLOR_PRESET_KEY,
        BATTERY_COLOR_MODE_NORMAL_KEY,
        BATTERY_COLOR_MODE_POWER_SAVE_KEY,
        BATTERY_COLOR_MODE_PERFORMANCE_KEY,
        BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY,
        BATTERY_COLOR_MODE_CHARGING_KEY,
        BATTERY_COLOR_MODE_LOW_KEY,
        BATTERY_COLOR_NORMAL_KEY,
        BATTERY_COLOR_POWER_SAVE_KEY,
        BATTERY_COLOR_PERFORMANCE_KEY,
        BATTERY_COLOR_SUPER_POWER_SAVE_KEY,
        BATTERY_COLOR_CHARGING_KEY,
        BATTERY_COLOR_LOW_KEY,
    )

// Persisted text scale remains in the pre-521 physical scale.
internal const val TOP_TEXT_UI_REF = 1.3f
internal const val TOP_TEXT_UI_MIN = 0.4f
internal const val TOP_TEXT_UI_MAX = 1.6f
internal const val BATTERY_TOP_TEXT_SCALE_DEFAULT =
    TOP_TEXT_UI_REF
internal const val BATTERY_TOP_TEXT_SCALE_MIN =
    TOP_TEXT_UI_REF * TOP_TEXT_UI_MIN
internal const val BATTERY_TOP_TEXT_SCALE_MAX =
    TOP_TEXT_UI_REF * TOP_TEXT_UI_MAX
internal const val BATTERY_TOP_TEXT_WEIGHT_DEFAULT = 900
internal const val BATTERY_TOP_TEXT_WEIGHT_MIN = 400
internal const val BATTERY_TOP_TEXT_WEIGHT_MAX = 1400
// Runtime/persisted offset is physical canonical displacement. Device review
// established that the previous +3 position is the intended user-facing zero.
internal const val TOP_OFFSET_UI_REF = 3f
internal const val TOP_OFFSET_UI_MIN = -10f
internal const val TOP_OFFSET_UI_MAX = 10f
internal const val TOP_OFFSET_DEFAULT =
    TOP_OFFSET_UI_REF
internal const val BATTERY_TOP_VERTICAL_OFFSET_MIN =
    TOP_OFFSET_UI_REF + TOP_OFFSET_UI_MIN
internal const val BATTERY_TOP_VERTICAL_OFFSET_MAX =
    TOP_OFFSET_UI_REF + TOP_OFFSET_UI_MAX

// Stored charging scale is a physical multiplier. Keep the old and current
// UI references so existing values migrate without changing rendered size.
private const val LEGACY_CHARGING_ICON_UI_REF = 1.5f
internal const val CHARGING_ICON_UI_REF = 1.65f
internal const val CHARGING_ICON_UI_MIN = 0.4f
internal const val CHARGING_ICON_UI_MAX = 1.6f
internal const val CHARGING_ICON_SCALE_DEFAULT =
    CHARGING_ICON_UI_REF
internal const val CHARGING_ICON_SCALE_MIN =
    CHARGING_ICON_UI_REF * CHARGING_ICON_UI_MIN
internal const val CHARGING_ICON_SCALE_MAX =
    CHARGING_ICON_UI_REF * CHARGING_ICON_UI_MAX
private const val BATTERY_COLOR_PRESET_SCHEMA_KEY =
    "battery_color_preset_schema"
private const val BATTERY_PRESET_SCHEMA = 2
internal const val CHARGING_SCALE_SCHEMA_KEY =
    "battery_top_charging_scale_schema"
private const val CHARGING_SCALE_SCHEMA = 2
private const val BATTERY_TOP_SCALE_EPSILON = 0.0001f

internal fun batteryColorPresetForMissingKey(
    hadPreviousVisualSchema: Boolean,
): BatteryColorPreset {
    @Suppress("UNUSED_VARIABLE")
    val compatibilityMarker = hadPreviousVisualSchema
    return BatteryColorPreset.HYPEROS
}

internal fun migrateBatteryPreset(
    preferences: SharedPreferences,
) {
    if (
        preferences.getInt(BATTERY_COLOR_PRESET_SCHEMA_KEY, 0) >=
            BATTERY_PRESET_SCHEMA
    ) {
        return
    }

    val editor = preferences.edit()
    if (!preferences.contains(BATTERY_COLOR_PRESET_KEY)) {
        val preset =
            batteryColorPresetForMissingKey(
                hadPreviousVisualSchema =
                    preferences.contains(CHARGING_SCALE_SCHEMA_KEY),
            )
        editor.putString(BATTERY_COLOR_PRESET_KEY, preset.persistedValue)
    }
    editor
        .putInt(
            BATTERY_COLOR_PRESET_SCHEMA_KEY,
            BATTERY_PRESET_SCHEMA,
        )
        .apply()
}

internal fun migrateChargingScaleRef(
    preferences: SharedPreferences,
) {
    if (
        preferences.getInt(CHARGING_SCALE_SCHEMA_KEY, 1) >=
            CHARGING_SCALE_SCHEMA
    ) {
        return
    }

    val editor = preferences.edit()
    if (preferences.contains(CHARGING_ICON_SCALE_KEY)) {
        val raw =
            preferences.getFloat(
                CHARGING_ICON_SCALE_KEY,
                LEGACY_CHARGING_ICON_UI_REF,
            )
        if (
            abs(raw - LEGACY_CHARGING_ICON_UI_REF) <=
                BATTERY_TOP_SCALE_EPSILON
        ) {
            editor.putFloat(
                CHARGING_ICON_SCALE_KEY,
                CHARGING_ICON_UI_REF,
            )
        }
    }
    editor
        .putInt(
            CHARGING_SCALE_SCHEMA_KEY,
            CHARGING_SCALE_SCHEMA,
        )
        .apply()
}

internal fun topTextUiDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER -> 1.2f
        ContentLayout.BATTERY_CENTER -> 1.4f
    }

internal fun mobileTypeSizeScaleDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER -> MOBILE_TYPE_SIZE_SCALE_DEFAULT
        ContentLayout.BATTERY_CENTER -> 0.8f
    }

internal fun chargingIconUiDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER -> 1f
        ContentLayout.BATTERY_CENTER -> 1.2f
    }

internal fun topTextScaleDefault(
    layout: ContentLayout,
): Float =
    (
        TOP_TEXT_UI_REF *
            topTextUiDefault(layout)
    ).coerceIn(
        BATTERY_TOP_TEXT_SCALE_MIN,
        BATTERY_TOP_TEXT_SCALE_MAX,
    )

internal fun chargingIconScaleDefault(
    layout: ContentLayout,
): Float =
    (
        CHARGING_ICON_UI_REF *
            chargingIconUiDefault(layout)
    ).coerceIn(
        CHARGING_ICON_SCALE_MIN,
        CHARGING_ICON_SCALE_MAX,
    )

internal fun batteryTopTextUiScale(rawScale: Float): Float =
    (rawScale / TOP_TEXT_UI_REF)
        .coerceIn(
            TOP_TEXT_UI_MIN,
            TOP_TEXT_UI_MAX,
        )

internal fun batteryTopChargingIconUiScale(rawScale: Float): Float =
    (rawScale / CHARGING_ICON_UI_REF)
        .coerceIn(
            CHARGING_ICON_UI_MIN,
            CHARGING_ICON_UI_MAX,
        )


internal fun topOffsetUi(rawOffset: Float): Float =
    (rawOffset - TOP_OFFSET_UI_REF)
        .coerceIn(
            TOP_OFFSET_UI_MIN,
            TOP_OFFSET_UI_MAX,
        )

internal fun topOffsetRaw(uiOffset: Float): Float =
    (
        uiOffset.coerceIn(
            TOP_OFFSET_UI_MIN,
            TOP_OFFSET_UI_MAX,
        ) + TOP_OFFSET_UI_REF
    ).coerceIn(
        BATTERY_TOP_VERTICAL_OFFSET_MIN,
        BATTERY_TOP_VERTICAL_OFFSET_MAX,
    )


internal const val COMBINED_SCALE_DEFAULT = 1f
internal const val COMBINED_SCALE_MIN = 0.60f
internal const val COMBINED_SCALE_MAX = 1.00f
internal const val OUTER_WEIGHT_SCALE_DEFAULT = 1f
internal const val OUTER_WEIGHT_SCALE_MIN = 0.70f
internal const val OUTER_WEIGHT_SCALE_MAX = 1.30f
internal const val WIFI_SIZE_SCALE_DEFAULT = 1f
internal const val WIFI_SIZE_SCALE_MIN = 0.40f
internal const val WIFI_SIZE_SCALE_MAX = 1.25f
internal const val AIRPLANE_SIZE_SCALE_DEFAULT = 1f
internal const val AIRPLANE_SIZE_SCALE_MIN = WIFI_SIZE_SCALE_MIN
internal const val AIRPLANE_SIZE_SCALE_MAX = WIFI_SIZE_SCALE_MAX
internal const val NO_SIM_SIZE_SCALE_DEFAULT = 1f
internal const val NO_SIM_SIZE_SCALE_MIN = WIFI_SIZE_SCALE_MIN
internal const val NO_SIM_SIZE_SCALE_MAX = WIFI_SIZE_SCALE_MAX
internal const val MOBILE_TYPE_SIZE_SCALE_DEFAULT = 1f
internal const val MOBILE_TYPE_SIZE_SCALE_MIN = 0.40f
internal const val MOBILE_TYPE_SIZE_SCALE_MAX = 1.25f
internal const val MOBILE_TYPE_WEIGHT_DEFAULT = 900
internal const val MOBILE_TYPE_WEIGHT_MIN = 400
internal const val MOBILE_TYPE_WEIGHT_MAX = 1400

internal fun batteryColorModeKey(slot: BatteryColorSlot): String =
    when (slot) {
        BatteryColorSlot.NORMAL -> BATTERY_COLOR_MODE_NORMAL_KEY
        BatteryColorSlot.POWER_SAVE -> BATTERY_COLOR_MODE_POWER_SAVE_KEY
        BatteryColorSlot.PERFORMANCE -> BATTERY_COLOR_MODE_PERFORMANCE_KEY
        BatteryColorSlot.SUPER_POWER_SAVE -> BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY
        BatteryColorSlot.CHARGING -> BATTERY_COLOR_MODE_CHARGING_KEY
        BatteryColorSlot.LOW -> BATTERY_COLOR_MODE_LOW_KEY
    }

internal fun batteryColorOverrideKey(slot: BatteryColorSlot): String =
    when (slot) {
        BatteryColorSlot.NORMAL -> BATTERY_COLOR_NORMAL_KEY
        BatteryColorSlot.POWER_SAVE -> BATTERY_COLOR_POWER_SAVE_KEY
        BatteryColorSlot.PERFORMANCE -> BATTERY_COLOR_PERFORMANCE_KEY
        BatteryColorSlot.SUPER_POWER_SAVE -> BATTERY_COLOR_SUPER_POWER_SAVE_KEY
        BatteryColorSlot.CHARGING -> BATTERY_COLOR_CHARGING_KEY
        BatteryColorSlot.LOW -> BATTERY_COLOR_LOW_KEY
    }

internal fun batteryColorModeFromPersisted(
    persistedMode: String?,
    hasStoredColor: Boolean,
): BatteryColorMode =
    if (persistedMode != null) {
        BatteryColorMode.fromPersisted(persistedMode)
    } else if (hasStoredColor) {
        BatteryColorMode.CUSTOM
    } else {
        BatteryColorMode.PRESET
    }
