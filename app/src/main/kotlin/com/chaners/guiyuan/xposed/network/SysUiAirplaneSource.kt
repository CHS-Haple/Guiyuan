package com.chaners.guiyuan.xposed.network

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

internal object SysUiAirplaneSource {
    private val uri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON)

    private inline fun <T> readOrNull(block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            if (error is VirtualMachineError || error is ThreadDeath) throw error
            null
        }

    private var resolver: ContentResolver? = null
    private var observer: ContentObserver? = null

    @Volatile
    private var onAirplaneMode: ((Boolean) -> Unit)? = null

    @Volatile
    private var onEvent: ((String) -> Unit)? = null

    @Volatile
    private var lastState: Boolean? = null

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
                        uri,
                        false,
                        nextObserver,
                    )
                    true
                } catch (error: Throwable) {
                    if (error is VirtualMachineError || error is ThreadDeath) throw error
                    false
                }
            if (!registered) {
                this.onAirplaneMode = null
                this.onEvent = null
                lastState = null
                return false
            }
            resolver = nextResolver
            observer = nextObserver
        }

        this.onAirplaneMode = onAirplaneMode
        this.onEvent = onEvent
        publish("seed")
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
            lastState = null
        }
    }

    @Synchronized
    private fun publish(source: String) {
        val currentResolver = resolver ?: return
        val enabled =
            readOrNull {
                Settings.Global.getInt(
                    currentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0,
                ) != 0
            } ?: return

        if (lastState == enabled) {
            return
        }
        lastState = enabled
        onAirplaneMode?.invoke(enabled)
        readOrNull {
            onEvent?.invoke(
                "airplaneState setting enabled=" + enabled +
                    " source=" + source,
            )
        }
    }
}
