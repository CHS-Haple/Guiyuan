package com.chaners.guiyuan.xposed

import android.content.Context
import android.graphics.drawable.Drawable
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        val semanticActiveSubIds =
            bindings
                .map { binding -> binding.subscriptionId }
                .distinct()
                .filter { subscriptionId ->
                    state.mobile[subscriptionId]
                        ?.signal
                        ?.let { signal -> signal !is SignalStrength.Unknown }
                        ?: false
                }
                .toSet()
        val platformActive =
            bindings
                .firstOrNull()
                ?.root
                ?.context
                ?.let(::activeSubscriptionIds)
        val resolvedActive =
            resolveActiveSubIds(
                boundSubscriptionIds =
                    bindings.map { binding -> binding.subscriptionId },
                semanticActiveSubscriptionIds = semanticActiveSubIds,
                authoritativeActiveSubscriptionIds =
                    platformActive,
            )
        val activeBindingSubIds = resolvedActive.subscriptionIds
        val activeBindings =
            bindings.filter { binding ->
                binding.subscriptionId in activeBindingSubIds
            }
        val visible =
            activeBindings.filter { binding ->
                binding.root.visibility == View.VISIBLE
            }

        val mode =
            classify(
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
            selectNetworkTypeSubscriptionId(
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
                resolveNetworkType(
                    binding = binding,
                    pendingMobileTypeDrawable = pendingMobileTypeDrawable,
                )
            }

        return Snapshot(
            mode = mode,
            boundRoots = bindings.size,
            activeBoundRoots = activeBindings.size,
            visibleRoots = visible.size,
            activeSubscriptionIds = activeBindingSubIds.sorted(),
            activeSubscriptionAuthority =
                if (resolvedActive.authoritative) {
                    "subscription-manager"
                } else {
                    "pipeline-semantic-fallback"
                },
            presentationRootSubscriptionId = target?.subscriptionId,
            effectiveDataSubscriptionId = effectiveDataSubscriptionId,
            networkTypeSubscriptionId = networkTypeSubscriptionId,
            networkType = networkType,
        )
    }

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

    private fun resolveNetworkType(
        binding: SysUiNetworkSource.MobilePresentationBinding,
        pendingMobileTypeDrawable: Drawable?,
    ): NetworkType? {
        val root = binding.root

        findViewByResourceEntry(root, MOBILE_TYPE_RESOURCE_ENTRY)
            ?.let { view ->
                val image = view as? ImageView
                val drawable = image?.drawable
                val networkType =
                    normalizeDrawableNetworkType(
                        rawLabel = drawable?.readStringField(MOBILE_TYPE_FIELD).orEmpty(),
                        enhanced =
                            drawable?.readBooleanField(MOBILE_TYPE_ENHANCED_FIELD) == true,
                        beforeMeasure = drawable != null && drawable === pendingMobileTypeDrawable,
                    )
                if (networkType != null) {
                    return networkType
                }
            }

        findViewByResourceEntry(root, MOBILE_TYPE_SINGLE_RESOURCE_ENTRY)
            ?.let { view ->
                val text = (view as? TextView)?.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    return NetworkType(
                        label = text,
                        enhanced = false,
                        source = NetworkTypeSource.MOBILE_TYPE_SINGLE,
                    )
                }
            }

        return null
    }

    internal fun normalizeDrawableNetworkType(
        rawLabel: String,
        enhanced: Boolean,
        beforeMeasure: Boolean,
    ): NetworkType? {
        val label = rawLabel.trim()
        if (label.isEmpty()) {
            return null
        }

        if (beforeMeasure) {
            return if (label == MOBILE_TYPE_DOUBLE_PLUS_LABEL) {
                NetworkType(
                    label = MOBILE_TYPE_DOUBLE_PLUS_BASE_LABEL,
                    enhanced = true,
                    source = NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                )
            } else {
                NetworkType(
                    label = label,
                    enhanced = false,
                    source = NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                )
            }
        }

        return NetworkType(
            label = label,
            enhanced = enhanced,
            source = NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
        )
    }

    private fun findViewByResourceEntry(
        root: View,
        entryName: String,
    ): View? {
        if (resourceEntryName(root) == entryName) {
            return root
        }
        val group = root as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findViewByResourceEntry(group.getChildAt(index), entryName)?.let {
                return it
            }
        }
        return null
    }

    private fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    private fun Drawable.readStringField(name: String): String? =
        runCatching {
            javaClass.getDeclaredField(name)
                .apply { isAccessible = true }
                .get(this) as? String
        }.getOrNull()

    private fun Drawable.readBooleanField(name: String): Boolean? =
        runCatching {
            javaClass.getDeclaredField(name)
                .apply { isAccessible = true }
                .getBoolean(this)
        }.getOrNull()

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
    )


    private fun activeSubscriptionIds(context: Context): Set<Int>? {
        val manager =
            context.getSystemService(SubscriptionManager::class.java)
                ?: return null

        // null means the platform source is unavailable; an empty set is still authoritative.
        return runCatching {
            manager.activeSubscriptionInfoList
                ?.map { info -> info.subscriptionId }
                ?.filter { subscriptionId -> subscriptionId >= 0 }
                ?.toSet()
        }.getOrNull()
    }

    internal data class Snapshot(
        val mode: Mode,
        val boundRoots: Int,
        val activeBoundRoots: Int = boundRoots,
        val visibleRoots: Int,
        val activeSubscriptionIds: List<Int>,
        val activeSubscriptionAuthority: String = "pipeline-semantic-fallback",
        val presentationRootSubscriptionId: Int?,
        val effectiveDataSubscriptionId: Int?,
        val networkTypeSubscriptionId: Int?,
        val networkType: NetworkType?,
    ) {
        val nativeMobileReplacementReady: Boolean
            get() =
                when (mode) {
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

    private const val MOBILE_TYPE_RESOURCE_ENTRY = "mobile_type"
    private const val MOBILE_TYPE_SINGLE_RESOURCE_ENTRY = "mobile_type_single"
    private const val MOBILE_TYPE_FIELD = "mMobileType"
    private const val MOBILE_TYPE_ENHANCED_FIELD = "mShowMobileTypeDoublePlus"
    private const val MOBILE_TYPE_DOUBLE_PLUS_LABEL = "5G++"
    private const val MOBILE_TYPE_DOUBLE_PLUS_BASE_LABEL = "5G"
}
