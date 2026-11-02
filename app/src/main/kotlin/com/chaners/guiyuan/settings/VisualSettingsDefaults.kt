package com.chaners.guiyuan.settings


internal const val COMBINED_STATUS_VISUAL_PREFS_NAME = "combined_status_visual"
internal const val CONTENT_LAYOUT_KEY = "content_layout"
internal const val DUAL_SIGNAL_ENABLED_KEY = "dual_signal_enabled"
internal const val DUAL_PROFILES_INITIALIZED_KEY = "dual_profiles_initialized"
internal const val SIGNAL_DOT_ARRANGEMENT_KEY = "content_style"
internal const val HORIZONTAL_LOWER_SINGLE_ROW_KEY = "single_row_flat_dots"
// v1.0.0 released key, consumed once when introducing three layout profiles.
internal const val LEGACY_DUAL_DOT_STYLE_KEY = "dual_dot_style"
internal const val SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY = "shared_content_style_schema"
// Keep the original persisted names; these offsets now serve both SIM modes.
internal const val CURVED_SIGNAL_DOT_OFFSET_Y_KEY = "dual_default_offset_y"
internal const val HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY = "dual_duo_offset_y"
internal const val MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY = "mobile_type_bottom_offset_y"
internal const val MOBILE_TYPE_BOTTOM_OFFSET_DEFAULT = -3f
internal const val MOBILE_TYPE_BOTTOM_ZERO_REFERENCE = 3f
internal fun mobileTypeBottomOffsetDefault(layout: ContentLayout): Float =
    if (layout == ContentLayout.MOBILE_CENTER) MOBILE_TYPE_BOTTOM_OFFSET_DEFAULT
    else NETWORK_TOP_OFFSET_DEFAULT
internal const val MOBILE_FOLLOWS_BATTERY_COLOR_KEY = "mobile_follows_battery_color"
internal const val CENTER_FOLLOWS_BATTERY_COLOR_KEY = "center_follows_battery_color"
internal const val BATTERY_TOP_READOUT_ENABLED_KEY = "battery_top_readout_enabled"
internal const val BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY =
    "battery_top_text_follows_battery_color"
internal const val BATTERY_TOP_CHARGING_ICON_ENABLED_KEY =
    "battery_top_charging_icon_enabled"
internal const val BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY =
    "battery_top_charging_icon_follows_battery_color"
internal const val BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY =
    "battery_fill_follows_retract_endpoint"
internal const val CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY =
    "control_center_tint_transition_enabled"
internal const val BATTERY_TOP_TEXT_SCALE_KEY = "battery_top_text_scale"
internal const val BATTERY_TOP_TEXT_WEIGHT_KEY = "battery_top_text_weight"
internal const val BATTERY_TOP_VERTICAL_OFFSET_KEY = "battery_top_vertical_offset"
internal const val WIFI_VERTICAL_OFFSET_KEY = "wifi_vertical_offset"
internal const val MOBILE_TYPE_VERTICAL_OFFSET_KEY = "mobile_type_vertical_offset"
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_KEY = "battery_top_charging_icon_scale"
internal const val COMBINED_SCALE_KEY = "combined_scale"
internal const val OUTER_WEIGHT_SCALE_KEY = "outer_weight_scale"
internal const val WIFI_SIZE_SCALE_KEY = "wifi_size_scale"
internal const val WIFI_WEIGHT_KEY = "wifi_weight"
internal const val AIRPLANE_SIZE_SCALE_KEY = "airplane_size_scale"
internal const val AIRPLANE_WEIGHT_KEY = "airplane_weight"
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
        BATTERY_TOP_READOUT_ENABLED_KEY,
        BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
        BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_TEXT_SCALE_KEY,
        BATTERY_TOP_TEXT_WEIGHT_KEY,
        BATTERY_TOP_VERTICAL_OFFSET_KEY,
        WIFI_VERTICAL_OFFSET_KEY,
        MOBILE_TYPE_VERTICAL_OFFSET_KEY,
        SIGNAL_DOT_ARRANGEMENT_KEY,
        HORIZONTAL_LOWER_SINGLE_ROW_KEY,
        MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY,
        CURVED_SIGNAL_DOT_OFFSET_Y_KEY,
        HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY,
        BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
        COMBINED_SCALE_KEY,
        OUTER_WEIGHT_SCALE_KEY,
        WIFI_SIZE_SCALE_KEY,
        WIFI_WEIGHT_KEY,
        AIRPLANE_SIZE_SCALE_KEY,
        AIRPLANE_WEIGHT_KEY,
        NO_SIM_SIZE_SCALE_KEY,
        MOBILE_TYPE_SIZE_SCALE_KEY,
        MOBILE_TYPE_WEIGHT_KEY,
    )

internal val BATTERY_COLOR_KEYS =
    setOf(BATTERY_COLOR_PRESET_KEY) +
        BatteryColorSlot.entries.flatMap { slot ->
            listOf(batteryColorModeKey(slot), batteryColorOverrideKey(slot))
        }

internal val GLOBAL_VISUAL_KEYS =
    setOf(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
    ) + BATTERY_COLOR_KEYS

// Persisted text scale is physical; UI percentages use the 1.3x reference.
internal const val BATTERY_TOP_TEXT_UI_SCALE_REFERENCE = 1.3f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MIN = 0.4f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MAX = 1.6f
internal const val BATTERY_TOP_TEXT_SCALE_DEFAULT =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_TEXT_SCALE_MIN =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MIN
internal const val BATTERY_TOP_TEXT_SCALE_MAX =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MAX
internal const val BATTERY_TOP_TEXT_WEIGHT_DEFAULT = 600
internal const val BATTERY_TOP_TEXT_WEIGHT_MIN = 1
internal const val BATTERY_TOP_TEXT_WEIGHT_MAX = 1000
// Persisted offset stays physical; UI zero maps to the +3 reference.
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE = 3f
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_MIN = -10f
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_MAX = 10f
internal const val BATTERY_TOP_VERTICAL_OFFSET_DEFAULT =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE
internal const val BATTERY_TOP_VERTICAL_OFFSET_MIN =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE + BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
internal const val BATTERY_TOP_VERTICAL_OFFSET_MAX =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE + BATTERY_TOP_VERTICAL_OFFSET_UI_MAX
internal const val NETWORK_TOP_OFFSET_DEFAULT = 0f
internal const val NETWORK_TOP_OFFSET_MIN = -10f
internal const val NETWORK_TOP_OFFSET_MAX = 10f

// Physical 1.65x reference is the released 1.0.0 representation.
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE = 1.65f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN = 0.4f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX = 1.6f
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MIN =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE * BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MAX =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE * BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX

internal fun topReadoutDefault(layout: ContentLayout): Boolean =
    layout == ContentLayout.BATTERY_CENTER

internal fun topTextUiScaleDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER, ContentLayout.MOBILE_CENTER -> 1.2f
        ContentLayout.BATTERY_CENTER -> 1.3f
    }

internal fun mobileTypeScaleDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER -> MOBILE_TYPE_SIZE_SCALE_DEFAULT
        ContentLayout.BATTERY_CENTER -> 0.8f
        ContentLayout.MOBILE_CENTER -> 0.85f
    }

internal fun topChargingIconUiScaleDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER, ContentLayout.MOBILE_CENTER -> 1f
        ContentLayout.BATTERY_CENTER -> 1.1f
    }

internal fun topTextScaleDefault(
    layout: ContentLayout,
): Float =
    (
        BATTERY_TOP_TEXT_UI_SCALE_REFERENCE *
            topTextUiScaleDefault(layout)
    ).coerceIn(
        BATTERY_TOP_TEXT_SCALE_MIN,
        BATTERY_TOP_TEXT_SCALE_MAX,
    )

internal fun topChargingIconScaleDefault(
    layout: ContentLayout,
): Float =
    (
        BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE *
            topChargingIconUiScaleDefault(layout)
    ).coerceIn(
        BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
        BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
    )

internal fun topTextUiScale(rawScale: Float): Float =
    (rawScale / BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
        .coerceIn(
            BATTERY_TOP_TEXT_UI_SCALE_MIN,
            BATTERY_TOP_TEXT_UI_SCALE_MAX,
        )

internal fun topChargingIconUiScale(rawScale: Float): Float =
    (rawScale / BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
        .coerceIn(
            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
        )


internal fun topOffsetYUi(rawOffset: Float): Float =
    (rawOffset - BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE)
        .coerceIn(
            BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
        )

internal fun topOffsetYRaw(uiOffset: Float): Float =
    (
        uiOffset.coerceIn(
            BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
        ) + BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE
    ).coerceIn(
        BATTERY_TOP_VERTICAL_OFFSET_MIN,
        BATTERY_TOP_VERTICAL_OFFSET_MAX,
    )

internal fun topOffsetYDefault(layout: ContentLayout): Float =
    if (layout != ContentLayout.BATTERY_CENTER) {
        topOffsetYRaw(3f)
    } else {
        BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
    }


internal const val COMBINED_SCALE_DEFAULT = 1f
internal const val COMBINED_SCALE_MIN = 0.60f
internal const val COMBINED_SCALE_MAX = 1.00f
internal const val OUTER_WEIGHT_SCALE_DEFAULT = 1f
internal const val OUTER_WEIGHT_SCALE_MIN = 0.70f
internal const val OUTER_WEIGHT_SCALE_MAX = 1.30f
internal const val WIFI_WEIGHT_DEFAULT = 100
internal const val WIFI_WEIGHT_MIN = 60
internal const val WIFI_WEIGHT_MAX = 130
internal const val WIFI_SIZE_SCALE_DEFAULT = 1f
internal const val WIFI_SIZE_SCALE_MIN = 0.40f
internal const val WIFI_SIZE_SCALE_MAX = 1.25f
internal const val AIRPLANE_WEIGHT_DEFAULT = 100
internal const val AIRPLANE_WEIGHT_MIN = 60
internal const val AIRPLANE_WEIGHT_MAX = 130
internal const val AIRPLANE_SIZE_SCALE_DEFAULT = 1f
internal const val AIRPLANE_SIZE_SCALE_MIN = WIFI_SIZE_SCALE_MIN
internal const val AIRPLANE_SIZE_SCALE_MAX = WIFI_SIZE_SCALE_MAX
internal const val NO_SIM_SIZE_SCALE_DEFAULT = 1f
internal const val NO_SIM_SIZE_SCALE_MIN = WIFI_SIZE_SCALE_MIN
internal const val NO_SIM_SIZE_SCALE_MAX = WIFI_SIZE_SCALE_MAX
internal const val MOBILE_TYPE_SIZE_SCALE_DEFAULT = 1f
internal const val MOBILE_TYPE_SIZE_SCALE_MIN = 0.40f
internal const val MOBILE_TYPE_SIZE_SCALE_MAX = 1.25f
internal const val MOBILE_TYPE_WEIGHT_DEFAULT = 600
internal const val MOBILE_TYPE_WEIGHT_MIN = 1
internal const val MOBILE_TYPE_WEIGHT_MAX = 1000

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
