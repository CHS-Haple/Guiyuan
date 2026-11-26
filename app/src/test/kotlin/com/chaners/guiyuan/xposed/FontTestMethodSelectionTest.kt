package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.FontTestMode
import com.chaners.guiyuan.settings.FontWeightAxis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FontTestMethodSelectionTest {
    private val verified = NativeFontWeight(
        FontWeightAxis(150f, 330f, 700f),
        NativeWeightMethod.OVERRIDE,
    )

    @Test
    fun autoPreservesVerifiedMethodAndWeight() {
        val selected = FontTestMode.AUTO.selectWeight(
            observedFace = true, detected = verified,
            automaticWeight = 620, requestedWeight = 760,
        )
        assertEquals(NativeWeightMethod.OVERRIDE, selected.method)
        assertEquals(620, selected.weight)
    }

    @Test
    fun manualModesCompareUnverifiedRawWeight() {
        val legacy = FontTestMode.SETTINGS.selectWeight(
            observedFace = true, detected = verified,
            automaticWeight = 620, requestedWeight = 760,
        )
        assertEquals(NativeWeightMethod.VARIATION_SETTINGS, legacy.method)
        assertEquals(760, legacy.weight)
        val override = FontTestMode.OVERRIDE.selectWeight(
            observedFace = true, detected = verified,
            automaticWeight = null, requestedWeight = 9999,
        )
        assertEquals(NativeWeightMethod.OVERRIDE, override.method)
        assertEquals(1000, override.weight)
    }

    @Test
    fun unknownFaceDoesNotFabricateAvailableFont() {
        val selected = FontTestMode.SETTINGS.selectWeight(
            observedFace = false, detected = verified,
            automaticWeight = null, requestedWeight = 900,
        )
        assertNull(selected.weight)
    }

    @Test
    fun unchangedKeepsTypefaceButNeverAppliesCustomWeight() {
        val selected = FontTestMode.NATIVE.selectWeight(
            observedFace = true, detected = verified,
            automaticWeight = 600, requestedWeight = 900,
        )
        assertNull(selected.weight)
        assertEquals(NativeWeightMethod.OVERRIDE, selected.method)
    }
}
