package com.chaners.guiyuan.settings

import com.chaners.guiyuan.BuildConfig

// B1977-only experiment. Not a production preference or backup field.
internal const val FONT_METHOD_LAB_KEY = "b1977_font_weight_method_lab"

internal enum class FontTestMode(val persistedValue: String) {
    AUTO("auto"),
    OVERRIDE("override"),
    SETTINGS("variation_settings"),
    NATIVE("native");

    val allowsUnverifiedWeight: Boolean
        get() = this == OVERRIDE || this == SETTINGS

    companion object {
        val available: Boolean get() = BuildConfig.BUILD_ID == "20261126-1977"

        fun fromStored(raw: String?): FontTestMode =
            if (!available) AUTO else entries.firstOrNull { it.persistedValue == raw } ?: AUTO
    }
}
