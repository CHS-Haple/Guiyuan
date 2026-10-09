package com.chaners.guiyuan.xposed.network

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

internal object SysUiAirplaneSource {
    private val uri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON)

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
        this.onAirplaneMode = onAirplaneMode
        this.onEvent = onEvent

        val nextResolver = context.contentResolver
        if (resolver !== nextResolver || observer == null) {
            observer?.let { old ->
                runCatching { resolver?.unregisterContentObserver(old) }
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
                runCatching {
                    nextResolver.registerContentObserver(
                        uri,
                        false,
                        nextObserver,
                    )
                }.isSuccess
            if (!registered) {
                onAirplaneMode = null
                onEvent = null
                lastState = null
                return false
            }
            resolver = nextResolver
            observer = nextObserver
        }

        publish("seed")
        return observer != null
    }

    @Synchronized
    fun detach() {
        observer?.let { currentObserver ->
            runCatching { resolver?.unregisterContentObserver(currentObserver) }
        }
        observer = null
        resolver = null
        onAirplaneMode = null
        onEvent = null
        lastState = null
    }

    @Synchronized
    private fun publish(source: String) {
        val currentResolver = resolver ?: return
        val enabled =
            runCatching {
                Settings.Global.getInt(
                    currentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0,
                ) != 0
            }.getOrNull() ?: return

        if (lastState == enabled) {
            return
        }
        lastState = enabled
        onAirplaneMode?.invoke(enabled)
        onEvent?.invoke(
            "airplaneState setting enabled=" + enabled +
                " source=" + source,
        )
    }
}
