package com.chaners.guiyuan.settings

import android.content.SharedPreferences

internal fun SharedPreferences.readVisualCfg(
    dual: Boolean = dualSignalEnabled(),
    layoutOverride: ContentLayout? = null,
): VisualCfg {
    val layout = layoutOverride ?: readLayout()
    // SystemUI can bind an older remote snapshot before the app migrates it.
    // Only read the retired dual bank when that bank actually exists.
    val legacyDualBank = usesLegacyVisualBank(dual, layout)
    fun flag(key: String, default: Boolean) =
        profileBoolean(layout, key, default, legacyDualBank)
    fun size(key: String, default: Float) =
        profileFloat(layout, key, default, legacyDualBank)
    fun weight(key: String, default: Int) =
        profileInt(layout, key, default, legacyDualBank)
    val topOffset = size(BATTERY_TOP_VERTICAL_OFFSET_KEY, topOffsetYDefault(layout))
    // Preserve existing battery-centered network position when split keys are first read.
    val oldNetworkOffset = legacyNetworkOffset(layout, topOffset)

    val dualStyle = readDualDotStyle(dual, layout, legacyDualBank)
    val singleStyle = readSingleDotStyle(layout, legacyDualBank, dualStyle)

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
        batteryColorPreset = readBatteryColorPreset(),
        batteryColorModes = readBatteryColorModes(),
        batteryColorOverrides = readBatteryColorOverrides(),
    ).normalized()
}

private fun SharedPreferences.usesLegacyVisualBank(
    dual: Boolean,
    layout: ContentLayout,
): Boolean =
    dual &&
        getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0) < 1 &&
        // The local v4 style migration may leave a temporary old dual bank.
        getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) < 4 &&
        getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false) &&
        PROFILE_VISUAL_BASE_KEYS.any { key ->
            contains(visualModeKey(true, visualProfileKey(layout, key)))
        }

private fun legacyNetworkOffset(layout: ContentLayout, topOffset: Float): Float =
    if (layout == ContentLayout.BATTERY_CENTER) topOffsetYUi(topOffset) else 0f

private fun SharedPreferences.readDualDotStyle(
    dual: Boolean,
    layout: ContentLayout,
    legacyDualBank: Boolean,
): SignalDotArrangement =
    (
        profileString(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY, null, legacyDualBank)
            ?: getString(visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY), null)
        )?.let(SignalDotArrangement::fromPersisted)
        ?: SignalDotArrangement.BOTH_SIDES

private fun SharedPreferences.readSingleDotStyle(
    layout: ContentLayout,
    legacyDualBank: Boolean,
    dualStyle: SignalDotArrangement,
): SignalDotArrangement =
    profileString(layout, SINGLE_ROW_DOT_STYLE_KEY, null, legacyDualBank)
        ?.let(SignalDotArrangement::fromPersisted)
        ?: if (profileBoolean(layout, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY, false, legacyDualBank) &&
            dualStyle == SignalDotArrangement.HORIZONTAL) {
            SignalDotArrangement.HORIZONTAL
        } else {
            SignalDotArrangement.CURVED
        }

private fun SharedPreferences.readBatteryColorPreset(): BatteryColorPreset =
    BatteryColorPreset.fromPersisted(
        getString(
            BATTERY_COLOR_PRESET_KEY,
            BatteryColorPreset.HYPEROS.persistedValue,
        ),
    )

private fun SharedPreferences.readBatteryColorModes(): BatteryColorModes =
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
    )

private fun SharedPreferences.readBatteryColorOverrides(): BatteryColorOverrides =
    BatteryColorOverrides(
        normal = optionalColor(BATTERY_COLOR_NORMAL_KEY),
        powerSave = optionalColor(BATTERY_COLOR_POWER_SAVE_KEY),
        performance = optionalColor(BATTERY_COLOR_PERFORMANCE_KEY),
        superPowerSave = optionalColor(BATTERY_COLOR_SUPER_POWER_SAVE_KEY),
        charging = optionalColor(BATTERY_COLOR_CHARGING_KEY),
        low = optionalColor(BATTERY_COLOR_LOW_KEY),
    )

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
