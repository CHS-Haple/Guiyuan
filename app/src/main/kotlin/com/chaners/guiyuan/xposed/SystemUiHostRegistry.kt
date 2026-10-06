package com.chaners.guiyuan.xposed

import java.lang.ref.WeakReference

internal object HostRegistry {
    private var statusHost = WeakReference<Any>(null)

    @Synchronized
    fun currentStatusHost(): Any? = statusHost.get()

    @Synchronized
    fun restoreStatusHost(host: Any): Capture {
        statusHost = WeakReference(host)
        return Capture(
            host = host,
            className = host.javaClass.name,
            identity = System.identityHashCode(host),
            replacement = false,
        )
    }

    @Synchronized
    fun captureStatusHost(host: Any): Capture? {
        val previous = statusHost.get()
        if (previous === host) {
            return null
        }

        statusHost = WeakReference(host)
        return Capture(
            host = host,
            className = host.javaClass.name,
            identity = System.identityHashCode(host),
            replacement = previous != null,
        )
    }

    internal data class Capture(
        val host: Any,
        val className: String,
        val identity: Int,
        val replacement: Boolean,
    )
}
