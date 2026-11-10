package com.chaners.guiyuan.settings

import android.content.SharedPreferences

private fun storedProfileKey(
    layout: ContentLayout,
    baseKey: String,
    legacyDualBank: Boolean,
): String {
    val sharedKey = visualProfileKey(layout, baseKey)
    return if (legacyDualBank) visualModeKey(true, sharedKey) else sharedKey
}

private fun SharedPreferences.profileBoolean(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Boolean,
    legacyDualBank: Boolean = false,
): Boolean {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return if (contains(profileKey)) {
        getBoolean(profileKey, defaultValue)
    } else if (legacyDualBank) {
        defaultValue
    } else {
        getBoolean(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileFloat(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Float,
    legacyDualBank: Boolean = false,
): Float {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return if (contains(profileKey)) {
        getFloat(profileKey, defaultValue)
    } else if (legacyDualBank) {
        defaultValue
    } else {
        getFloat(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileInt(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Int,
    legacyDualBank: Boolean = false,
): Int {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else if (legacyDualBank) {
        defaultValue
    } else {
        getInt(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileString(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: String?,
    legacyDualBank: Boolean,
): String? {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return when {
        contains(profileKey) -> getString(profileKey, defaultValue)
        legacyDualBank -> defaultValue
        else -> getString(baseKey, defaultValue)
    }
}

internal fun SharedPreferences.readVisualCfg(
    dual: Boolean = dualSignalEnabled(),
    layoutOverride: ContentLayout? = null,
): VisualCfg {
    val layout = layoutOverride ?: readLayout()
    // SystemUI can bind an older remote snapshot before the app migrates it.
    // Only read the retired dual bank when that bank actually exists.
    val legacyDualBank = dual &&
        getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0) < 1 &&
        // The local v4 style migration may leave a temporary old dual bank.
        getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) < 4 &&
        getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false) &&
        PROFILE_VISUAL_BASE_KEYS.any { key ->
            contains(visualModeKey(true, visualProfileKey(layout, key)))
        }
    fun flag(key: String, default: Boolean) =
        profileBoolean(layout, key, default, legacyDualBank)
    fun size(key: String, default: Float) =
        profileFloat(layout, key, default, legacyDualBank)
    fun weight(key: String, default: Int) =
        profileInt(layout, key, default, legacyDualBank)
    val topOffset = size(BATTERY_TOP_VERTICAL_OFFSET_KEY, topOffsetYDefault(layout))
    // Preserve existing battery-centered network position when split keys are first read.
    val oldNetworkOffset =
        if (layout == ContentLayout.BATTERY_CENTER) topOffsetYUi(topOffset) else 0f

    val dualStyle = (
        profileString(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY, null, legacyDualBank)
            ?: getString(visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY), null)
        )?.let(SignalDotArrangement::fromPersisted)
        ?: SignalDotArrangement.BOTH_SIDES
    val singleStyle = profileString(layout, SINGLE_ROW_DOT_STYLE_KEY, null, legacyDualBank)
        ?.let(SignalDotArrangement::fromPersisted)
        ?: if (flag(LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY, false) &&
            dualStyle == SignalDotArrangement.HORIZONTAL) {
            SignalDotArrangement.HORIZONTAL
        } else {
            SignalDotArrangement.CURVED
        }

    return VisualCfg(
        layout = layout,
        dualSignal = dual,
        dualRowDotArrangement = dualStyle,
        singleRowDotArrangement = singleStyle,
        bothSidesShowActivity = flag(BOTH_SIDES_ACTIVITY_KEY, true),
        bothSidesHideIdleActivity = flag(BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY, false),
        bothSidesBottomCharging = flag(BOTH_SIDES_CHARGING_KEY, false),
        bothSidesActivityScale = size(BOTH_SIDES_ACTIVITY_SCALE_KEY, BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT),
        bothSidesActivityOffsetY = size(BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        bothSidesChargingOffsetY = size(BOTH_SIDES_CHARGING_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        fixedLowerOpening = flag(LOWER_FIXED_OPENING_KEY, false),
        mobileTypeBottomOffsetY = size(MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY, mobileTypeBottomOffsetDefault(layout)),
        mobileFollowsBatteryColor = flag(MOBILE_FOLLOWS_BATTERY_COLOR_KEY, false),
        centerFollowsBatteryColor = flag(CENTER_FOLLOWS_BATTERY_COLOR_KEY, false),
        showTopReadout = flag(BATTERY_TOP_READOUT_ENABLED_KEY, topReadoutDefault(layout)),
        topTextFollowsBatteryColor = flag(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY, true),
        showChargingIcon = flag(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY, true),
        topChargingIconFollowsBatteryColor = flag(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY, true),
        fillFollowsRetract = getBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, true),
        ccTintTransition = getBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, true),
        topTextScale = size(BATTERY_TOP_TEXT_SCALE_KEY, topTextScaleDefault(layout)),
        topTextWeight = weight(BATTERY_TOP_TEXT_WEIGHT_KEY, BATTERY_TOP_TEXT_WEIGHT_DEFAULT),
        topOffsetY = topOffset,
        wifiOffsetY = size(WIFI_VERTICAL_OFFSET_KEY, oldNetworkOffset),
        mobileTypeOffsetY = size(MOBILE_TYPE_VERTICAL_OFFSET_KEY, oldNetworkOffset),
        curvedSignalDotOffsetY = size(CURVED_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        horizontalSignalDotOffsetY = size(HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        bothSidesSignalDotOffsetY = size(BOTH_SIDES_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        topChargingIconScale = size(BATTERY_TOP_CHARGING_ICON_SCALE_KEY, topChargingIconScaleDefault(layout)),
        combinedScale = size(COMBINED_SCALE_KEY, COMBINED_SCALE_DEFAULT),
        outerWeightScale = size(OUTER_WEIGHT_SCALE_KEY, OUTER_WEIGHT_SCALE_DEFAULT),
        wifiScale = size(WIFI_SIZE_SCALE_KEY, WIFI_SIZE_SCALE_DEFAULT),
        wifiWeight = weight(WIFI_WEIGHT_KEY, WIFI_WEIGHT_DEFAULT),
        airplaneScale = size(AIRPLANE_SIZE_SCALE_KEY, AIRPLANE_SIZE_SCALE_DEFAULT),
        airplaneWeight = weight(AIRPLANE_WEIGHT_KEY, AIRPLANE_WEIGHT_DEFAULT),
        noSimScale = size(NO_SIM_SIZE_SCALE_KEY, NO_SIM_SIZE_SCALE_DEFAULT),
        mobileTypeScale = size(MOBILE_TYPE_SIZE_SCALE_KEY, mobileTypeScaleDefault(layout)),
        mobileTypeWeight = weight(MOBILE_TYPE_WEIGHT_KEY, MOBILE_TYPE_WEIGHT_DEFAULT),
        batteryColorPreset =
            BatteryColorPreset.fromPersisted(
                getString(
                    BATTERY_COLOR_PRESET_KEY,
                    BatteryColorPreset.HYPEROS.persistedValue,
                ),
            ),
        batteryColorModes =
            BatteryColorModes(
                normal =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_NORMAL_KEY,
                        colorKey = BATTERY_COLOR_NORMAL_KEY,
                    ),
                powerSave =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_POWER_SAVE_KEY,
                        colorKey = BATTERY_COLOR_POWER_SAVE_KEY,
                    ),
                performance =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_PERFORMANCE_KEY,
                        colorKey = BATTERY_COLOR_PERFORMANCE_KEY,
                    ),
                superPowerSave =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY,
                        colorKey = BATTERY_COLOR_SUPER_POWER_SAVE_KEY,
                    ),
                charging =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_CHARGING_KEY,
                        colorKey = BATTERY_COLOR_CHARGING_KEY,
                    ),
                low =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_LOW_KEY,
                        colorKey = BATTERY_COLOR_LOW_KEY,
                    ),
            ),
        batteryColorOverrides =
            BatteryColorOverrides(
                normal = optionalColor(BATTERY_COLOR_NORMAL_KEY),
                powerSave = optionalColor(BATTERY_COLOR_POWER_SAVE_KEY),
                performance = optionalColor(BATTERY_COLOR_PERFORMANCE_KEY),
                superPowerSave = optionalColor(BATTERY_COLOR_SUPER_POWER_SAVE_KEY),
                charging = optionalColor(BATTERY_COLOR_CHARGING_KEY),
                low = optionalColor(BATTERY_COLOR_LOW_KEY),
            ),
    ).normalized()
}

internal fun SharedPreferences.Editor.putVisualCfg(
    settings: VisualCfg,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val layout = normalized.layout
    val editor = putBoolean(
        DUAL_SIGNAL_ENABLED_KEY,
        normalized.dualSignal,
    ).putString(
        CONTENT_LAYOUT_KEY,
        layout.persistedValue,
    ).putString(
        visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY),
        normalized.dualRowDotArrangement.persistedValue,
    ).putString(
        visualProfileKey(layout, SINGLE_ROW_DOT_STYLE_KEY),
        normalized.singleRowDotArrangement.persistedValue,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_KEY),
        normalized.bothSidesShowActivity,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY),
        normalized.bothSidesHideIdleActivity,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_CHARGING_KEY),
        normalized.bothSidesBottomCharging,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_SCALE_KEY),
        normalized.bothSidesActivityScale,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY),
        normalized.bothSidesActivityOffsetY,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_CHARGING_OFFSET_Y_KEY),
        normalized.bothSidesChargingOffsetY,
    ).putBoolean(
        visualProfileKey(layout, LOWER_FIXED_OPENING_KEY),
        normalized.fixedLowerOpening,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY),
        normalized.mobileTypeBottomOffsetY,
    ).putBoolean(
        visualProfileKey(layout, MOBILE_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.mobileFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, CENTER_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.centerFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY),
        normalized.showTopReadout,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.topTextFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_ENABLED_KEY),
        normalized.showChargingIcon,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.topChargingIconFollowsBatteryColor,
    ).putBoolean(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        normalized.fillFollowsRetract,
    ).putBoolean(
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
        normalized.ccTintTransition,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_TEXT_SCALE_KEY),
        normalized.topTextScale,
    ).putInt(
        visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY),
        normalized.topTextWeight,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY),
        normalized.topOffsetY,
    ).putFloat(
        visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY),
        normalized.wifiOffsetY,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_VERTICAL_OFFSET_KEY),
        normalized.mobileTypeOffsetY,
    ).putFloat(
        visualProfileKey(layout, CURVED_SIGNAL_DOT_OFFSET_Y_KEY),
        normalized.curvedSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY),
        normalized.horizontalSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_SIGNAL_DOT_OFFSET_Y_KEY),
        normalized.bothSidesSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
        normalized.topChargingIconScale,
    ).putFloat(
        visualProfileKey(layout, COMBINED_SCALE_KEY),
        normalized.combinedScale,
    ).putFloat(
        visualProfileKey(layout, OUTER_WEIGHT_SCALE_KEY),
        normalized.outerWeightScale,
    ).putFloat(
        visualProfileKey(layout, WIFI_SIZE_SCALE_KEY),
        normalized.wifiScale,
    ).putInt(
        visualProfileKey(layout, WIFI_WEIGHT_KEY),
        normalized.wifiWeight,
    ).putFloat(
        visualProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY),
        normalized.airplaneScale,
    ).putInt(
        visualProfileKey(layout, AIRPLANE_WEIGHT_KEY),
        normalized.airplaneWeight,
    ).putFloat(
        visualProfileKey(layout, NO_SIM_SIZE_SCALE_KEY),
        normalized.noSimScale,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY),
        normalized.mobileTypeScale,
    ).putInt(
        visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY),
        normalized.mobileTypeWeight,
    ).putBatteryColors(normalized)
    // One writer serves app-local settings and the SystemUI remote mirror.
    // Publish the schema with the values so stale dual banks cannot win later.
    return editor
        .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        .putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
}

private fun SharedPreferences.batteryColorMode(
    modeKey: String,
    colorKey: String,
): BatteryColorMode =
    batteryColorModeFromPersisted(
        persistedMode = getString(modeKey, null),
        hasStoredColor = contains(colorKey),
    )

private fun SharedPreferences.optionalColor(key: String): Int? =
    if (contains(key)) {
        getInt(key, 0) or 0xFF000000.toInt()
    } else {
        null
    }
