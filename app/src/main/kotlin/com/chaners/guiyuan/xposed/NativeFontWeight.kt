package com.chaners.guiyuan.xposed

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import com.chaners.guiyuan.settings.FontWeightAxis

internal enum class NativeWeightMethod {
    OVERRIDE,
    VARIATION_SETTINGS;

    fun apply(paint: Paint, weight: Int?): Boolean {
        val variation = weight?.let { "'wght' $it" }
        return when (this) {
            OVERRIDE -> {
                paint.setFontVariationOverride(variation)
                true
            }
            VARIATION_SETTINGS -> paint.setFontVariationSettings(variation)
        }
    }

    companion object {
        val platformDefault: NativeWeightMethod
            get() = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
                OVERRIDE
            } else VARIATION_SETTINGS
    }
}

internal data class NativeFontWeight(
    val axis: FontWeightAxis,
    val method: NativeWeightMethod,
)

// One source and its selected weight method; absence of weight does not discard the face.
internal data class NativeMobileFont(
    val face: Typeface,
    val weight: NativeFontWeight?,
)
