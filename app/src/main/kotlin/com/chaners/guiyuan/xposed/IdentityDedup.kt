package com.chaners.guiyuan.xposed

import java.util.Collections
import java.util.IdentityHashMap

// Different objects can share an identityHashCode.
internal inline fun <T> Iterable<T>.distinctByIdentity(
    key: (T) -> Any?,
): List<T> {
    val seen = Collections.newSetFromMap(IdentityHashMap<Any?, Boolean>())
    return filter { seen.add(key(it)) }
}
