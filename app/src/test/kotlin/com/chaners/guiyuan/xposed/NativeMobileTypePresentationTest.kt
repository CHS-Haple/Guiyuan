package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeMobileTypePresentationTest {
    @Test
    fun preMeasureDoublePlusMatchesNativeNormalization() {
        val networkType =
            NativeMobileTypePresentation.normalizeDrawableNetworkType(
                rawLabel = "5G++",
                enhanced = false,
                beforeMeasure = true,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }

    @Test
    fun preMeasureRegularTypeDoesNotReuseStaleDoublePlusFlag() {
        val networkType =
            NativeMobileTypePresentation.normalizeDrawableNetworkType(
                rawLabel = "4G",
                enhanced = true,
                beforeMeasure = true,
            )

        assertEquals("4G", networkType?.label)
        assertEquals(false, networkType?.enhanced)
    }

    @Test
    fun commonLegacyAndLteLabelsPassThroughNativeNormalization() {
        listOf("2G", "E", "3G", "H+", "4G", "LTE").forEach { label ->
            val networkType =
                NativeMobileTypePresentation.normalizeDrawableNetworkType(
                    rawLabel = label,
                    enhanced = false,
                    beforeMeasure = false,
                )

            assertEquals(label, networkType?.label)
            assertEquals(false, networkType?.enhanced)
        }
    }

    @Test
    fun postMeasureKeepsNativeDoublePlusFlag() {
        val networkType =
            NativeMobileTypePresentation.normalizeDrawableNetworkType(
                rawLabel = "5G",
                enhanced = true,
                beforeMeasure = false,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }
}
