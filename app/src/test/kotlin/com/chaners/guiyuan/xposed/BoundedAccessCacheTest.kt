package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedAccessCacheTest {
    @Test
    fun lookupPromotesAnEntryAndEvictsLeastRecentlyUsed() {
        val cache = BoundedAccessCache<Int, String>(2)
        cache[1] = "one"
        cache[2] = "two"
        assertEquals("one", cache[1])
        cache[3] = "three"

        assertEquals(listOf(1, 3), cache.keys.toList())
        assertFalse(cache.containsKey(2))
        assertEquals("one", cache[1])
    }

    @Test
    fun textAndInkCapacitiesRetainTheirExistingLimits() {
        for (capacity in listOf(128, 48)) {
            val cache = BoundedAccessCache<Int, Int>(capacity)
            for (value in 0..capacity) cache[value] = value

            assertEquals(capacity, cache.size)
            assertFalse(cache.containsKey(0))
            assertTrue(cache.containsKey(capacity))
        }
    }

    @Test
    fun nativeStrokeCapacityCanExceedInitialAllocation() {
        val cache = BoundedAccessCache<Int, String>(capacity = 64, initialCapacity = 24)
        for (value in 0..64) cache[value] = "weight-$value"

        assertEquals(64, cache.size)
        assertFalse(cache.containsKey(0))
        assertEquals("weight-1", cache[1])
        cache[65] = "weight-65"
        assertFalse(cache.containsKey(2))
        assertTrue(cache.containsKey(1))
    }

    @Test
    fun nullValueStillCountsAsCachedAndPromotesOnRead() {
        val cache = BoundedAccessCache<Int, String?>(2)
        cache[1] = null
        cache[2] = "present"
        assertTrue(cache.containsKey(1))
        assertEquals(null, cache[1])
        cache[3] = "new"

        assertTrue(cache.containsKey(1))
        assertFalse(cache.containsKey(2))
        assertEquals(listOf(1, 3), cache.keys.toList())
    }
}
