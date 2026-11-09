package com.android.systemui

/** JVM-only stand-in for the native Dependency.get(Class) contract. */
class Dependency {
    companion object {
        var lookup: ((Class<*>) -> Any?)? = null

        @JvmStatic
        fun get(type: Class<*>): Any? = lookup?.invoke(type)
    }
}
