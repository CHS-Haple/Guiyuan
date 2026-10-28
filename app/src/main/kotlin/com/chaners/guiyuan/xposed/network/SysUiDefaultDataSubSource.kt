package com.chaners.guiyuan.xposed.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SubscriptionManager
import java.lang.reflect.InvocationTargetException

internal object SysUiDefaultDataSubSource {
    private const val DEFAULT_DATA_SUBSCRIPTION_CHANGED_ACTION =
        "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"

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

    private var registeredContext: Context? = null
    private var receiver: BroadcastReceiver? = null

    @Volatile
    private var onChanged: ((Int) -> Unit)? = null

    @Volatile
    private var onEvent: ((String) -> Unit)? = null

    @Volatile
    private var currentSubscriptionId: Int =
        SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private class Delivery(val subscriptionId: Int)

    private var lastDelivery: Delivery? = null

    fun currentSubscriptionId(): Int = currentSubscriptionId

    @Synchronized
    fun attach(
        context: Context,
        onChanged: (Int) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Boolean {
        val appContext = context.applicationContext
        if (registeredContext !== appContext || receiver == null) {
            try {
                unregisterReceiver()
            } catch (error: Throwable) {
                this.onChanged = null
                this.onEvent = null
                throw error
            }

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
                try {
                    appContext.registerReceiver(
                        nextReceiver,
                        IntentFilter(
                            DEFAULT_DATA_SUBSCRIPTION_CHANGED_ACTION,
                        ),
                        Context.RECEIVER_EXPORTED,
                    )
                    true
                } catch (error: Throwable) {
                    if (error is VirtualMachineError || error is ThreadDeath) throw error
                    false
                }

            if (registered) {
                registeredContext = appContext
                receiver = nextReceiver
            }
        }

        this.onChanged = onChanged
        this.onEvent = onEvent
        // A failed seed must not leave the previous listener's receipt in place.
        lastDelivery = null

        // Keep the seed even when registration fails, without retaining its callbacks.
        try {
            publish(
                source = "seed",
                force = true,
            )
        } finally {
            if (receiver == null) {
                this.onChanged = null
                this.onEvent = null
                lastDelivery = null
            }
        }
        return receiver != null
    }

    @Synchronized
    fun detach() {
        try {
            unregisterReceiver()
        } finally {
            onChanged = null
            onEvent = null
            currentSubscriptionId =
                SubscriptionManager.INVALID_SUBSCRIPTION_ID
            lastDelivery = null
        }
    }

    @Synchronized
    private fun publish(
        source: String,
        force: Boolean,
    ) {
        val next =
            readOrNull { SubscriptionManager.getDefaultDataSubscriptionId() }
                ?: return

        val deliver = onChanged
        if (deliver == null) {
            currentSubscriptionId = next
            return
        }

        val changed =
            try {
                deliverSubscription(next, force, deliver)
            } catch (error: Throwable) {
                rethrowFatal(error)
                readOrNull {
                    onEvent?.invoke(
                        "defaultDataSubscription delivery-failed type=" +
                            error.javaClass.simpleName + " source=" + source,
                    )
                }
                return
            }
        if (!changed) return
        readOrNull {
            onEvent?.invoke(
                "defaultDataSubscription id=" + next +
                    " source=" + source,
            )
        }
    }

    @Synchronized
    internal fun deliverSubscription(
        next: Int,
        force: Boolean,
        deliver: (Int) -> Unit,
    ): Boolean {
        currentSubscriptionId = next
        if (!force && lastDelivery?.subscriptionId == next) return false

        val previous = lastDelivery
        val pending = Delivery(next)
        lastDelivery = pending
        try {
            deliver(next)
        } catch (error: Throwable) {
            if (lastDelivery === pending) lastDelivery = previous
            throw error
        }
        return true
    }

    private fun unregisterReceiver() {
        val context = registeredContext
        val currentReceiver = receiver
        if (context != null && currentReceiver != null) {
            context.unregisterReceiver(currentReceiver)
        }
        receiver = null
        registeredContext = null
        lastDelivery = null
    }
}
