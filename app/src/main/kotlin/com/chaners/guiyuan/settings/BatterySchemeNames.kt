package com.chaners.guiyuan.settings

internal fun limitCustomSchemeName(value: String): String {
    val codePointCount = value.codePointCount(0, value.length)
    if (codePointCount <= BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS) return value
    val endIndex =
        value.offsetByCodePoints(
            0,
            BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS,
        )
    return value.substring(0, endIndex)
}

// Shared safety boundary for user-created names and imported archive metadata.
internal fun isUnsafePortableTextCodePoint(point: Int): Boolean =
    Character.isISOControl(point) ||
        point == 0x061C ||
        point in 0x200E..0x200F ||
        point in 0x2028..0x202E ||
        point in 0x2066..0x206F

internal fun normalizeCustomSchemeName(value: String): String {
    val clean = buildString {
        var index = 0
        while (index < value.length) {
            val point = Character.codePointAt(value, index)
            val unsafe =
                point in 0xD800..0xDFFF || isUnsafePortableTextCodePoint(point)
            if (unsafe) append(' ') else appendCodePoint(point)
            index += Character.charCount(point)
        }
    }
    return limitCustomSchemeName(clean.trim())
}
