package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.NativePresentationResolver.ActiveBindingResolution
import com.chaners.guiyuan.xposed.NativePresentationResolver.Mode

internal object NativeSubscriptionPolicy {
    internal fun selectNetworkTypeSubscriptionId(
        effectiveDataSubscriptionId: Int?,
        presentationRootSubscriptionId: Int?,
        boundSubscriptionIds: Collection<Int>,
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
        val authoritative = authoritativeActiveSubscriptionIds != null
        val source = authoritativeActiveSubscriptionIds ?: semanticActiveSubscriptionIds
        val resolved = linkedSetOf<Int>()
        for (subscriptionId in boundSubscriptionIds) {
            if (subscriptionId >= 0 && subscriptionId in source) {
                resolved.add(subscriptionId)
            }
        }
        return ActiveBindingResolution(
            subscriptionIds = resolved,
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
        // Both authoritative SIM slots must be present with distinct active IDs.
        // A partially bound or duplicated native witness stays unclassified.
        if (platform.size != 2 || activeSubscriptionIds.size != 2) return emptyMap()
        val first = platform[0]
        val second = platform[1]
        if (
            first.first == second.first ||
            first.first !in activeSubscriptionIds ||
            second.first !in activeSubscriptionIds ||
            first.second !in 0..1 ||
            second.second !in 0..1 ||
            first.second == second.second
        ) {
            return emptyMap()
        }
        return mapOf(first.second to first.first, second.second to second.first)
    }
}
