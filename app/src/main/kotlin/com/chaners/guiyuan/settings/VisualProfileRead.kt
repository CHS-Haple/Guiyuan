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

// Keep all typed readers on the same profile-key fallback contract.
// An absent retired dual-bank key must not inherit the unrelated base value.
private fun SharedPreferences.profileReadKey(
    layout: ContentLayout,
    baseKey: String,
    legacyDualBank: Boolean,
): String? {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return when {
        contains(profileKey) -> profileKey
        legacyDualBank -> null
        else -> baseKey
    }
}

internal fun SharedPreferences.profileBoolean(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Boolean,
    legacyDualBank: Boolean = false,
): Boolean {
    val key = profileReadKey(layout, baseKey, legacyDualBank) ?: return defaultValue
    return getBoolean(key, defaultValue)
}

internal fun SharedPreferences.profileFloat(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Float,
    legacyDualBank: Boolean = false,
): Float {
    val key = profileReadKey(layout, baseKey, legacyDualBank) ?: return defaultValue
    return getFloat(key, defaultValue)
}

internal fun SharedPreferences.profileInt(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Int,
    legacyDualBank: Boolean = false,
): Int {
    val key = profileReadKey(layout, baseKey, legacyDualBank) ?: return defaultValue
    return getInt(key, defaultValue)
}

internal fun SharedPreferences.profileString(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: String?,
    legacyDualBank: Boolean,
): String? {
    val key = profileReadKey(layout, baseKey, legacyDualBank) ?: return defaultValue
    return getString(key, defaultValue)
}
