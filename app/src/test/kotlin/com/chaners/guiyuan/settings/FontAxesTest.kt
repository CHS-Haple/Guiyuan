package com.chaners.guiyuan.settings

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FontAxesTest {
    @Test
    fun readsWeightAxisFromTrueType() {
        val font = sfnt()
        assertEquals(FontWeightAxis(150f, 330f, 700f), FontAxes.weight(font))
        assertEquals(
            FontWeightAxis(150f, 330f, 700f),
            FontAxes.weight(font.asReadOnlyBuffer()),
        )
    }

    @Test
    fun readsWeightAxisFromCollection() {
        val font = ByteBuffer.allocate(160).order(ByteOrder.BIG_ENDIAN)
        font.putInt(0, 0x74746366)
        font.putInt(4, 0x00010000)
        font.putInt(8, 1)
        font.putInt(12, 32)
        putSfnt(font, 32, 80)
        assertEquals(FontWeightAxis(150f, 330f, 700f), FontAxes.weight(font))
        assertNull(FontAxes.weight(font, 1))
    }

    @Test
    fun requiresVerifiedFaceIndexForMultiFaceCollections() {
        val font = ByteBuffer.allocate(400).order(ByteOrder.BIG_ENDIAN)
        font.putInt(0, 0x74746366)
        font.putInt(4, 0x00010000)
        font.putInt(8, 2)
        font.putInt(12, 32)
        font.putInt(16, 96)
        putSfnt(font, 32, 176)
        putSfnt(font, 96, 256)
        font.putInt(256 + 20, 250 * 65536)
        font.putInt(256 + 24, 400 * 65536)
        font.putInt(256 + 28, 750 * 65536)
        assertNull(FontAxes.weight(font))
        assertEquals(FontWeightAxis(150f, 330f, 700f), FontAxes.weight(font, 0))
        assertEquals(FontWeightAxis(250f, 400f, 750f), FontAxes.weight(font, 1))
    }

    @Test
    fun rejectsMalformedTablesAndAxes() {
        val font = sfnt()
        font.putInt(20, Int.MAX_VALUE)
        assertNull(FontAxes.weight(font))
        font.putInt(20, 40)
        font.putInt(68, 100 * 65536)
        assertNull(FontAxes.weight(font))
    }

    @Test
    fun rejectsMissingWeightAxis() {
        val font = sfnt()
        font.putInt(56, 0x77647468)
        assertNull(FontAxes.weight(font))
    }

    private fun sfnt(): ByteBuffer =
        ByteBuffer.allocate(128).order(ByteOrder.BIG_ENDIAN).also { putSfnt(it, 0, 40) }

    private fun putSfnt(font: ByteBuffer, offset: Int, fvar: Int) {
        font.putInt(offset, 0x00010000)
        font.putShort(offset + 4, 1)
        font.putInt(offset + 12, 0x66766172)
        font.putInt(offset + 20, fvar)
        font.putInt(offset + 24, 40)
        font.putShort(fvar + 4, 16)
        font.putShort(fvar + 8, 1)
        font.putShort(fvar + 10, 20)
        font.putInt(fvar + 16, 0x77676874)
        font.putInt(fvar + 20, 150 * 65536)
        font.putInt(fvar + 24, 330 * 65536)
        font.putInt(fvar + 28, 700 * 65536)
    }
}
