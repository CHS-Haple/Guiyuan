package com.chaners.guiyuan.settings

internal fun topReadoutDefault(layout: ContentLayout): Boolean =
    layout == ContentLayout.BATTERY_CENTER

internal fun topTextUiScaleDefault(
    layout: ContentLayout,
): Float =
    when (layout) {
        ContentLayout.NETWORK_CENTER, ContentLayout.MOBILE_CENTER -> 1.2f
        ContentLayout.BATTERY_CENTER -> 1.3f
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
