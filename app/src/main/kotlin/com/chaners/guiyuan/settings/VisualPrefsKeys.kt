package com.chaners.guiyuan.settings

import android.content.SharedPreferences

internal fun visualModeKey(dual: Boolean, key: String): String =
    if (dual) "dual.$key" else key

internal fun SharedPreferences.writeSignalDotOffsetY(offset: Float) {
    val visual = readVisualCfg()
    val style = if (visual.dualSignal) visual.dualRowDotArrangement else visual.singleRowDotArrangement
    if (style == SignalDotArrangement.BOTH_SIDES) return
    edit().putFloat(
        visualProfileKey(visual.layout, MOBILE_SIGNAL_DOT_OFFSET_Y_KEY),
        offset.finiteIn(
            NETWORK_TOP_OFFSET_MIN,
            NETWORK_TOP_OFFSET_MAX,
            NETWORK_TOP_OFFSET_DEFAULT,
        ),
    ).apply()
}

internal fun SharedPreferences.dualSignalEnabled(): Boolean =
    getBoolean(DUAL_SIGNAL_ENABLED_KEY, false)

internal fun SharedPreferences.readLayout(): ContentLayout =
    ContentLayout.fromPersisted(
        getString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue),
    )

internal fun visualProfileKey(
    layout: ContentLayout,
    baseKey: String,
): String = layout.persistedValue + "." + baseKey
