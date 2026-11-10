package com.chaners.guiyuan.settings

import android.content.SharedPreferences

internal fun SharedPreferences.Editor.putVisualCfg(
    settings: VisualCfg,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val editor =
        putSignalVisuals(normalized)
            .putBatteryReadoutVisuals(normalized)
            .putScaleAndColorVisuals(normalized)
    // One writer serves app-local settings and the SystemUI remote mirror.
    // Publish the schema with the values so stale dual banks cannot win later.
    return editor
        .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        .putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
}

private fun SharedPreferences.Editor.putSignalVisuals(cfg: VisualCfg): SharedPreferences.Editor {
    val layout = cfg.layout
    return putBoolean(
        DUAL_SIGNAL_ENABLED_KEY,
        cfg.dualSignal,
    ).putString(
        CONTENT_LAYOUT_KEY,
        layout.persistedValue,
    ).putString(
        visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY),
        cfg.dualRowDotArrangement.persistedValue,
    ).putString(
        visualProfileKey(layout, SINGLE_ROW_DOT_STYLE_KEY),
        cfg.singleRowDotArrangement.persistedValue,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_KEY),
        cfg.bothSidesShowActivity,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY),
        cfg.bothSidesHideIdleActivity,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_CHARGING_KEY),
        cfg.bothSidesBottomCharging,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_SCALE_KEY),
        cfg.bothSidesActivityScale,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY),
        cfg.bothSidesActivityOffsetY,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_CHARGING_OFFSET_Y_KEY),
        cfg.bothSidesChargingOffsetY,
    ).putBoolean(
        visualProfileKey(layout, LOWER_FIXED_OPENING_KEY),
        cfg.fixedLowerOpening,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY),
        cfg.mobileTypeBottomOffsetY,
    ).putBoolean(
        visualProfileKey(layout, MOBILE_FOLLOWS_BATTERY_COLOR_KEY),
        cfg.mobileFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, CENTER_FOLLOWS_BATTERY_COLOR_KEY),
        cfg.centerFollowsBatteryColor,
    )
}

private fun SharedPreferences.Editor.putBatteryReadoutVisuals(cfg: VisualCfg): SharedPreferences.Editor {
    val layout = cfg.layout
    return putBoolean(
        visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY),
        cfg.showTopReadout,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY),
        cfg.topTextFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_ENABLED_KEY),
        cfg.showChargingIcon,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY),
        cfg.topChargingIconFollowsBatteryColor,
    ).putBoolean(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        cfg.fillFollowsRetract,
    ).putBoolean(
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
        cfg.ccTintTransition,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_TEXT_SCALE_KEY),
        cfg.topTextScale,
    ).putInt(
        visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY),
        cfg.topTextWeight,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY),
        cfg.topOffsetY,
    ).putFloat(
        visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY),
        cfg.wifiOffsetY,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_VERTICAL_OFFSET_KEY),
        cfg.mobileTypeOffsetY,
    ).putFloat(
        visualProfileKey(layout, CURVED_SIGNAL_DOT_OFFSET_Y_KEY),
        cfg.curvedSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY),
        cfg.horizontalSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_SIGNAL_DOT_OFFSET_Y_KEY),
        cfg.bothSidesSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
        cfg.topChargingIconScale,
    )
}

private fun SharedPreferences.Editor.putScaleAndColorVisuals(cfg: VisualCfg): SharedPreferences.Editor {
    val layout = cfg.layout
    return putFloat(
        visualProfileKey(layout, COMBINED_SCALE_KEY),
        cfg.combinedScale,
    ).putFloat(
        visualProfileKey(layout, OUTER_WEIGHT_SCALE_KEY),
        cfg.outerWeightScale,
    ).putFloat(
        visualProfileKey(layout, WIFI_SIZE_SCALE_KEY),
        cfg.wifiScale,
    ).putInt(
        visualProfileKey(layout, WIFI_WEIGHT_KEY),
        cfg.wifiWeight,
    ).putFloat(
        visualProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY),
        cfg.airplaneScale,
    ).putInt(
        visualProfileKey(layout, AIRPLANE_WEIGHT_KEY),
        cfg.airplaneWeight,
    ).putFloat(
        visualProfileKey(layout, NO_SIM_SIZE_SCALE_KEY),
        cfg.noSimScale,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY),
        cfg.mobileTypeScale,
    ).putInt(
        visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY),
        cfg.mobileTypeWeight,
    ).putBatteryColors(cfg)
}
