package com.chaners.guiyuan.xposed

// Access order and capacity match the existing on-draw text/ink caches.
internal class BoundedAccessCache<K, V>(capacity: Int) :
    LinkedHashMap<K, V>(capacity, 0.75f, true) {
    private val maxEntries = capacity

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
        size > maxEntries
}
