package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IdentityDedupTest {
    private data class EqualValue(val value: Int)

    @Test
    fun keepsDistinctObjectsWithEqualValues() {
        val first = EqualValue(1)
        val second = EqualValue(1)
        val result = listOf(first, second, first).distinctByIdentity { it }

        assertEquals(2, result.size)
        assertSame(first, result[0])
        assertSame(second, result[1])
    }

    @Test
    fun nullKeysAreDeduplicatedWithoutDroppingOtherObjects() {
        val first = Any()
        val second = Any()
        val result = listOf(first, second, first, null, null).distinctByIdentity { it }

        assertEquals(3, result.size)
        assertSame(first, result[0])
        assertSame(second, result[1])
        assertEquals(null, result[2])
    }
}
