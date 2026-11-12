package com.chaners.guiyuan.xposed

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.network.SysUiDefaultDataSubSource
import com.chaners.guiyuan.xposed.network.SysUiNetworkSource
import com.chaners.guiyuan.xposed.network.SignalStrength

internal object NativePresentationResolver {
    fun resolve(
        state: StatusStateStore.Snapshot,
        defaultDataSubscriptionId: Int =
            SysUiDefaultDataSubSource.currentSubscriptionId(),
        pendingMobileTypeDrawable: Drawable? = null,
    ): Snapshot {
        val bindings =
            SysUiNetworkSource.mobilePresentationBindings()
                .filter { binding -> binding.root.isAttachedToWindow }
        val boundSubscriptionIds = bindings.map { binding -> binding.subscriptionId }
        val semanticActiveSubIds =
            linkedSetOf<Int>().apply {
                for (subscriptionId in boundSubscriptionIds) {
                    val signal = state.mobile[subscriptionId]?.signal
                    if (signal != null && signal !is SignalStrength.Unknown) {
                        add(subscriptionId)
                    }
                }
            }
        val platformSubscriptions =
            bindings
                .firstOrNull()
                ?.root
                ?.context
                ?.let(::activeSubscriptions)
        val platformActive = platformSubscriptions?.map { it.first }?.toSet()
        val resolvedActive =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = boundSubscriptionIds,
                semanticActiveSubscriptionIds = semanticActiveSubIds,
                authoritativeActiveSubscriptionIds =
                    platformActive,
            )
        val activeBindingSubIds = resolvedActive.subscriptionIds
        val slotSubscriptionIds =
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeBindingSubIds,
                platformSubscriptions,
            )
        val activeBindings =
            bindings.filter { binding ->
                binding.subscriptionId in activeBindingSubIds
            }
        val visible =
            activeBindings.filter { binding ->
                binding.root.visibility == View.VISIBLE
            }

        val mode =
            NativeSubscriptionPolicy.classify(
                boundRoots = activeBindings.size,
                visibleRoots = visible.size,
                activeSubscriptions = activeBindingSubIds.size,
            )

        val effectiveDataSubscriptionId =
            defaultDataSubscriptionId
                .takeIf { subscriptionId -> subscriptionId >= 0 }
                ?: activeBindingSubIds.firstOrNull()

        val target =
            when (mode) {
                Mode.DUAL_AGGREGATED,
                Mode.SINGLE,
                -> visible.firstOrNull()

                Mode.DUAL_SEPARATE ->
                    visible.firstOrNull { binding ->
                        binding.subscriptionId == effectiveDataSubscriptionId
                    } ?: visible.firstOrNull()

                Mode.UNKNOWN -> visible.firstOrNull()
            }

        val networkTypeSubscriptionId =
            NativeSubscriptionPolicy.selectNetworkTypeSubscriptionId(
                effectiveDataSubscriptionId = effectiveDataSubscriptionId,
                presentationRootSubscriptionId = target?.subscriptionId,
                boundSubscriptionIds =
                    activeBindings.map { binding -> binding.subscriptionId },
            )
        val networkTypeTarget =
            networkTypeSubscriptionId?.let { subscriptionId ->
                activeBindings.firstOrNull { binding ->
                    binding.subscriptionId == subscriptionId
                }
            }
        val networkType =
            networkTypeTarget?.let { binding ->
                NativeMobileTypePresentation.resolveNetworkType(
                    root = binding.root,
                    pendingMobileTypeDrawable = pendingMobileTypeDrawable,
                )
            } ?: if (bindings.isEmpty()) {
                resolveUnboundNativeType(pendingMobileTypeDrawable)
            } else {
                null
            }

        return Snapshot(
            mode = mode,
            boundRoots = bindings.size,
            activeBoundRoots = activeBindings.size,
            visibleRoots = visible.size,
            activeSubscriptionIds = activeBindingSubIds.sorted(),
            slotSubscriptionIds = slotSubscriptionIds,
            activeSubsAuthoritative = resolvedActive.authoritative,
            platformActiveSubscriptionIds = platformActive,
            presentationRootSubscriptionId = target?.subscriptionId,
            effectiveDataSubscriptionId = effectiveDataSubscriptionId,
            networkTypeSubscriptionId = networkTypeSubscriptionId,
            networkType = networkType,
        )
    }

    // Binder callbacks are not replayed for an existing SystemUI icon after Hot Reload.
    // A sole visible native mobile view supplies its label, not an invented SIM binding.
    private fun resolveUnboundNativeType(pendingMobileTypeDrawable: Drawable?): NetworkType? {
        val host = SysUiHostRegistry.current() ?: return null
        val group = NativeParticipantAccess.groupFor(host) ?: return null
        if (group.childCount > 64) return null

        var candidate: ViewGroup? = null
        for (index in 0 until group.childCount) {
            val root = group.getChildAt(index)
            if (
                root.javaClass.simpleName != "ModernStatusBarMobileView" ||
                NativeParticipantAccess.resourceEntryName(root) != "mobile_combo" ||
                root !is ViewGroup ||
                !root.isAttachedToWindow ||
                root.visibility != View.VISIBLE
            ) {
                continue
            }
            if (candidate != null) return null
            candidate = root
        }
        return candidate?.let { NativeMobileTypePresentation.resolveNetworkType(it, pendingMobileTypeDrawable) }
    }

    internal data class ActiveBindingResolution(
        val subscriptionIds: Set<Int>,
        val authoritative: Boolean,
    )

    internal enum class Mode {
        SINGLE,
        DUAL_SEPARATE,
        DUAL_AGGREGATED,
        UNKNOWN,
    }

    internal enum class NetworkTypeSource {
        MOBILE_TYPE_DRAWABLE,
        MOBILE_TYPE_SINGLE,
    }

    internal data class NetworkType(
        val label: String,
        val enhanced: Boolean,
        val source: NetworkTypeSource,
        val typeface: Typeface? = null,
        val fontPath: String? = null,
    )


    private fun activeSubscriptions(context: Context): List<Pair<Int, Int>>? {
        val manager =
            context.getSystemService(SubscriptionManager::class.java)
                ?: return null

        // null means the platform source is unavailable; an empty list is authoritative.
        return SysUiPresentationRuntime.optionalNativeRead {
            manager.activeSubscriptionInfoList
                ?.filter { info -> info.subscriptionId >= 0 }
                ?.map { info -> info.subscriptionId to info.simSlotIndex }
        }
    }

    internal data class Snapshot(
        val mode: Mode,
        val boundRoots: Int,
        val activeBoundRoots: Int = boundRoots,
        val visibleRoots: Int,
        val activeSubscriptionIds: List<Int>,
        val slotSubscriptionIds: Map<Int, Int> = emptyMap(),
        val activeSubsAuthoritative: Boolean = false,
        val platformActiveSubscriptionIds: Set<Int>? = null,
        val presentationRootSubscriptionId: Int?,
        val effectiveDataSubscriptionId: Int?,
        val networkTypeSubscriptionId: Int?,
        val networkType: NetworkType?,
    ) {
        // Diagnostic values are stable; only the source fact controls rendering.
        val activeSubscriptionAuthority: String
            get() = if (activeSubsAuthoritative) {
                "subscription-manager"
            } else {
                "pipeline-semantic-fallback"
            }

        val nativeMobileReplacementReady: Boolean
            get() {
                val platform = platformActiveSubscriptionIds
                // A partially bound dual-SIM host must not masquerade as single SIM.
                if (platform != null &&
                    (platform.size != activeSubscriptionIds.size ||
                        activeSubscriptionIds.any { it !in platform })
                ) {
                    return false
                }
                return when (mode) {
                    Mode.SINGLE ->
                        activeSubscriptionIds.size == 1 &&
                            visibleRoots == 1

                    Mode.DUAL_AGGREGATED ->
                        activeSubscriptionIds.size >= 2 &&
                            visibleRoots == 1

                    Mode.DUAL_SEPARATE,
                    Mode.UNKNOWN,
                    -> false
                }
            }
    }


}
