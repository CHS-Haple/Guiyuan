package com.chaners.guiyuan.xposed

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import com.chaners.guiyuan.settings.FontWeightAxis
import com.chaners.guiyuan.settings.FontTestMode

internal enum class NativeWeightMethod {
    OVERRIDE,
    VARIATION_SETTINGS;

    fun apply(paint: Paint, weight: Int): Boolean {
        val variation = "'wght' $weight"
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


// Lab-only projection. Never changes native font ownership or the automatic evidence gate.
internal data class FontLabSelection(
    val method: NativeWeightMethod,
    val weight: Int?,
)

internal fun FontTestMode.selectWeight(
    observedFace: Boolean,
    detected: NativeFontWeight?,
    automaticWeight: Int?,
    requestedWeight: Int,
): FontLabSelection {
    val automatic = FontLabSelection(
        method = detected?.method ?: NativeWeightMethod.platformDefault,
        weight = automaticWeight,
    )
    return when (this) {
        FontTestMode.AUTO -> automatic
        FontTestMode.NATIVE -> automatic.copy(weight = null)
        FontTestMode.OVERRIDE ->
            if (observedFace) FontLabSelection(NativeWeightMethod.OVERRIDE, requestedWeight.coerceIn(1, 1000))
            else automatic
        FontTestMode.SETTINGS ->
            if (observedFace) FontLabSelection(NativeWeightMethod.VARIATION_SETTINGS, requestedWeight.coerceIn(1, 1000))
            else automatic
    }
}
