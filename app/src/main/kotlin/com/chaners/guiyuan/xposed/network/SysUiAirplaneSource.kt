package com.chaners.guiyuan.xposed.network

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.chaners.guiyuan.xposed.SysUiFatal.rethrow as rethrowFatal
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead

internal object SysUiAirplaneSource {

    private var resolver: ContentResolver? = null
    private var observer: ContentObserver? = null

    @Volatile
    private var onAirplaneMode: ((Boolean) -> Unit)? = null

    @Volatile
    private var onEvent: ((String) -> Unit)? = null

    private class Delivery(val enabled: Boolean)

    private var lastDelivery: Delivery? = null

    @Synchronized
    @Suppress("TooGenericExceptionCaught")
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
    @Suppress("TooGenericExceptionCaught")
    private fun publish(source: String, force: Boolean = false) {
        val currentResolver = resolver ?: return
        val enabled =
            optionalNativeRead {
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
                SysUiPresentationRuntime.emitPassiveDiagnostic {
                    onEvent?.invoke(
                        "airplaneState delivery-failed type=" +
                            error.javaClass.simpleName + " source=" + source,
                    )
                }
                return
            }
        if (!changed) return
        SysUiPresentationRuntime.emitPassiveDiagnostic {
            onEvent?.invoke(
                "airplaneState setting enabled=" + enabled +
                    " source=" + source,
            )
        }
    }

    // Roll back the delivery receipt for any callback failure, then rethrow.
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
        var completed = false
        try {
            deliver(enabled)
            completed = true
        } finally {
            if (!completed && lastDelivery === next) lastDelivery = previous
        }
        return true
    }
}
