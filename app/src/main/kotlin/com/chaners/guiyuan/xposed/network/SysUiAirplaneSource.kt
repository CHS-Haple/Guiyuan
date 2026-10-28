package com.chaners.guiyuan.xposed.network

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.lang.reflect.InvocationTargetException

internal object SysUiAirplaneSource {
    private fun rethrowFatal(error: Throwable) {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
    }

    private inline fun <T> readOrNull(block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            rethrowFatal(error)
            null
        }

    private var resolver: ContentResolver? = null
    private var observer: ContentObserver? = null

    @Volatile
    private var onAirplaneMode: ((Boolean) -> Unit)? = null

    @Volatile
    private var onEvent: ((String) -> Unit)? = null

    private class Delivery(val enabled: Boolean)

    private var lastDelivery: Delivery? = null

    @Synchronized
    fun attach(
        context: Context,
        onAirplaneMode: (Boolean) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Boolean {
        val nextResolver = context.contentResolver
        if (resolver !== nextResolver || observer == null) {
            observer?.let { old ->
                try {
                    resolver?.unregisterContentObserver(old)
                } catch (error: Throwable) {
                    this.onAirplaneMode = null
                    this.onEvent = null
                    throw error
                }
            }
            resolver = null
            observer = null

            val nextObserver =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        publish("contentObserver")
                    }
                }

            val registered =
                try {
                    nextResolver.registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
                        false,
                        nextObserver,
                    )
                    true
                } catch (error: Throwable) {
                    rethrowFatal(error)
                    false
                }
            if (!registered) {
                this.onAirplaneMode = null
                this.onEvent = null
                lastDelivery = null
                return false
            }
            resolver = nextResolver
            observer = nextObserver
        }

        this.onAirplaneMode = onAirplaneMode
        this.onEvent = onEvent
        // The old sink's receipt cannot acknowledge delivery to this attachment.
        lastDelivery = null
        publish("seed", force = true)
        return observer != null
    }

    @Synchronized
    fun detach() {
        try {
            observer?.let { currentObserver ->
                resolver?.unregisterContentObserver(currentObserver)
            }
            observer = null
            resolver = null
        } finally {
            onAirplaneMode = null
            onEvent = null
            lastDelivery = null
        }
    }

    @Synchronized
    private fun publish(source: String, force: Boolean = false) {
        val currentResolver = resolver ?: return
        val enabled =
            readOrNull {
                Settings.Global.getInt(
                    currentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0,
                ) != 0
            } ?: return
        val deliver = onAirplaneMode ?: return

        val changed =
            try {
                deliverState(enabled, force, deliver)
            } catch (error: Throwable) {
                rethrowFatal(error)
                readOrNull {
                    onEvent?.invoke(
                        "airplaneState delivery-failed type=" +
                            error.javaClass.simpleName + " source=" + source,
                    )
                }
                return
            }
        if (!changed) return
        readOrNull {
            onEvent?.invoke(
                "airplaneState setting enabled=" + enabled +
                    " source=" + source,
            )
        }
    }

    @Synchronized
    internal fun deliverState(
        enabled: Boolean,
        force: Boolean,
        deliver: (Boolean) -> Unit,
    ): Boolean {
        if (!force && lastDelivery?.enabled == enabled) return false

        val previous = lastDelivery
        val next = Delivery(enabled)
        lastDelivery = next
        try {
            deliver(enabled)
        } catch (error: Throwable) {
            if (lastDelivery === next) lastDelivery = previous
            throw error
        }
        return true
    }
}
