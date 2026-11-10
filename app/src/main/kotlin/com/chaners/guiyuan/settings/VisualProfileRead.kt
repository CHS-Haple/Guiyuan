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

internal fun SharedPreferences.profileBoolean(
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

internal fun SharedPreferences.profileFloat(
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

internal fun SharedPreferences.profileInt(
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

internal fun SharedPreferences.profileString(
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
