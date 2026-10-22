package com.chaners.guiyuan.settings

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class FontWeightAxis(val min: Float, val default: Float, val max: Float)

internal object FontAxes {
    private const val TTC = 0x74746366
    private const val TRUE_TYPE = 0x00010000
    private const val OTTO = 0x4f54544f
    private const val TRUE = 0x74727565
    private const val TYP1 = 0x74797031
    private const val FVAR = 0x66766172
    private const val WGHT = 0x77676874

    // Without a verified TTC face index, a multi-face collection is ambiguous.
    fun weight(font: ByteBuffer, faceIndex: Int? = null): FontWeightAxis? {
        val data = font.slice().order(ByteOrder.BIG_ENDIAN)
        val size = data.limit().toLong()
        fun valid(at: Long, bytes: Long): Boolean =
            at >= 0 && bytes >= 0 && at <= size && bytes <= size - at
        fun int(at: Long): Int = data.getInt(at.toInt())
        fun uint(at: Long): Long = int(at).toLong() and 0xffffffffL
        fun short(at: Long): Int = data.getShort(at.toInt()).toInt() and 0xffff
        if ((faceIndex != null && faceIndex < 0) || !valid(0, 12)) return null
        val start =
            if (int(0) == TTC) {
                val faces = uint(8)
                val index = faceIndex ?: if (faces == 1L) 0 else return null
                val offset = 12L + index.toLong() * 4
                if (faces < 1 || faces > 4096 || index >= faces || !valid(offset, 4)) return null
                uint(offset)
            } else {
                if (faceIndex != null && faceIndex != 0) return null
                0L
            }
        if (!valid(start, 12) || int(start) !in setOf(TRUE_TYPE, OTTO, TRUE, TYP1)) return null
        val tables = short(start + 4)
        if (tables !in 1..4096 || !valid(start + 12, tables.toLong() * 16)) return null
        for (index in 0 until tables) {
            val entry = start + 12 + index * 16L
            if (int(entry) != FVAR) continue
            val offset = uint(entry + 8)
            val length = uint(entry + 12)
            if (length < 16 || !valid(offset, length)) return null
            val axesOffset = short(offset + 4).toLong()
            val axisCount = short(offset + 8)
            val axisSize = short(offset + 10).toLong()
            if (axisCount !in 1..64 || axisSize < 20 ||
                axesOffset + axisCount * axisSize > length
            ) return null
            for (axis in 0 until axisCount) {
                val at = offset + axesOffset + axis * axisSize
                if (int(at) != WGHT) continue
                val min = int(at + 4) / 65536f
                val default = int(at + 8) / 65536f
                val max = int(at + 12) / 65536f
                if (min <= 0 || min > default || default > max || max > 10000) return null
                return FontWeightAxis(min, default, max)
            }
            return null
        }
        return null
    }
}
