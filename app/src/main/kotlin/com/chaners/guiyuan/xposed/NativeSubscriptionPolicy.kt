package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.NativePresentationResolver.ActiveBindingResolution
import com.chaners.guiyuan.xposed.NativePresentationResolver.Mode

internal object NativeSubscriptionPolicy {
    internal fun selectNetworkTypeSubscriptionId(
        effectiveDataSubscriptionId: Int?,
        presentationRootSubscriptionId: Int?,
        boundSubscriptionIds: List<Int>,
    ): Int? =
        effectiveDataSubscriptionId
            ?.takeIf { subscriptionId -> subscriptionId in boundSubscriptionIds }
            ?: presentationRootSubscriptionId
                ?.takeIf { subscriptionId -> subscriptionId in boundSubscriptionIds }
            ?: boundSubscriptionIds.firstOrNull()

    internal fun resolveActiveSubIds(
        boundSubscriptionIds: List<Int>,
        semanticActiveSubscriptionIds: Set<Int>,
        authoritativeActiveSubscriptionIds: Set<Int>?,
    ): ActiveBindingResolution {
        val bound = boundSubscriptionIds.filter { it >= 0 }.toSet()
        val authoritative = authoritativeActiveSubscriptionIds != null
        val source =
            authoritativeActiveSubscriptionIds
                ?: semanticActiveSubscriptionIds
        return ActiveBindingResolution(
            subscriptionIds = bound.intersect(source),
            authoritative = authoritative,
        )
    }

    internal fun classify(
        boundRoots: Int,
        visibleRoots: Int,
        activeSubscriptions: Int,
    ): Mode =
        when {
            boundRoots <= 0 -> Mode.UNKNOWN
            activeSubscriptions <= 1 && visibleRoots <= 1 -> Mode.SINGLE
            activeSubscriptions >= 2 && visibleRoots >= 2 -> Mode.DUAL_SEPARATE
            activeSubscriptions >= 2 && visibleRoots == 1 -> Mode.DUAL_AGGREGATED
            else -> Mode.UNKNOWN
        }

    // Slot order is physical SIM1/SIM2 order, not numeric subscription-ID order.
    internal fun resolveSlotSubscriptions(
        activeSubscriptionIds: Set<Int>,
        platformSubscriptions: List<Pair<Int, Int>>?,
    ): Map<Int, Int> {
        val platform = platformSubscriptions ?: return emptyMap()
        // Bound roots can lag behind an additional active platform subscription.
        if (platform.size != 2) return emptyMap()
        val pairs =
            platform.filter { (id, slot) -> id in activeSubscriptionIds && slot in 0..1 }
        if (
            activeSubscriptionIds.size != 2 ||
            pairs.size != 2 ||
            pairs.map { it.first }.toSet() != activeSubscriptionIds ||
            pairs.map { it.second }.toSet() != setOf(0, 1)
        ) {
            return emptyMap()
        }
        return pairs.associate { (id, slot) -> slot to id }
    }
}
