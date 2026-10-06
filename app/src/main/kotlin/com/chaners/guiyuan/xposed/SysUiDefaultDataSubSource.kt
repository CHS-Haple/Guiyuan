package com.chaners.guiyuan.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SubscriptionManager

internal object SysUiDefaultDataSubSource {
    private const val DEFAULT_DATA_SUBSCRIPTION_CHANGED_ACTION =
        "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"

    private var registeredContext: Context? = null
    private var receiver: BroadcastReceiver? = null

    @Volatile
    private var onChanged: ((Int) -> Unit)? = null

    @Volatile
    private var onEvent: ((String) -> Unit)? = null

    @Volatile
    private var currentSubscriptionId: Int =
        SubscriptionManager.INVALID_SUBSCRIPTION_ID

    fun currentSubscriptionId(): Int = currentSubscriptionId

    @Synchronized
    fun attach(
        context: Context,
        onChanged: (Int) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Boolean {
        this.onChanged = onChanged
        this.onEvent = onEvent

        val appContext = context.applicationContext
        if (registeredContext !== appContext || receiver == null) {
            unregisterReceiver()

            val nextReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        if (
                            intent?.action ==
                            DEFAULT_DATA_SUBSCRIPTION_CHANGED_ACTION
                        ) {
                            publish(
                                source = "broadcast",
                                force = false,
                            )
                        }
                    }
                }

            val registered =
                runCatching {
                    appContext.registerReceiver(
                        nextReceiver,
                        IntentFilter(
                            DEFAULT_DATA_SUBSCRIPTION_CHANGED_ACTION,
                        ),
                        Context.RECEIVER_EXPORTED,
                    )
                    true
                }.getOrDefault(false)

            if (registered) {
                registeredContext = appContext
                receiver = nextReceiver
            }
        }

        publish(
            source = "seed",
            force = true,
        )
        return receiver != null
    }

    @Synchronized
    fun detach() {
        unregisterReceiver()
        onChanged = null
        onEvent = null
        currentSubscriptionId =
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }

    @Synchronized
    private fun publish(
        source: String,
        force: Boolean,
    ) {
        val next =
            runCatching {
                SubscriptionManager.getDefaultDataSubscriptionId()
            }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        if (!force && currentSubscriptionId == next) {
            return
        }

        currentSubscriptionId = next
        onChanged?.invoke(next)
        onEvent?.invoke(
            "defaultDataSubscription id=" + next +
                " source=" + source +
                " eventDriven=true authoritativeRead=true",
        )
    }

    private fun unregisterReceiver() {
        val context = registeredContext
        val currentReceiver = receiver
        if (context != null && currentReceiver != null) {
            runCatching {
                context.unregisterReceiver(currentReceiver)
            }
        }
        receiver = null
        registeredContext = null
    }
}
