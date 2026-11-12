package com.chaners.guiyuan.xposed

internal data class TintState(
    val appliedTint: Int,
    val statusIconTint: Int? = null,
) {
    val isVisible: Boolean
        get() = (appliedTint ushr 24) != 0
}

// Diagnostics preserve full unsigned ARGB bytes; callers own the visible prefix.
internal fun tintHexDigits(color: Int): String =
    color.toUInt().toString(16).padStart(8, '0')

internal fun colorHex(color: Int): String = "#" + tintHexDigits(color)
