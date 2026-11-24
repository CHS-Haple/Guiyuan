package com.chaners.guiyuan.xposed

// Access-order eviction shared by text, native drawable, and stroke caches.
internal class BoundedAccessCache<K, V>(
    capacity: Int,
    initialCapacity: Int = capacity,
) : LinkedHashMap<K, V>(initialCapacity, 0.75f, true) {
    private val maxEntries = capacity

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
        size > maxEntries
}
