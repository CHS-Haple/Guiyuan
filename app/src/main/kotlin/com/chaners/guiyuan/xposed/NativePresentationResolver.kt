package com.chaners.guiyuan.xposed

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.lang.reflect.Field
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import com.chaners.guiyuan.settings.FontAxes
import com.chaners.guiyuan.settings.NativeFontAxis
import java.util.concurrent.ConcurrentHashMap
import com.chaners.guiyuan.xposed.network.SysUiDefaultDataSubSource
import com.chaners.guiyuan.xposed.network.SysUiNetworkSource
import com.chaners.guiyuan.xposed.network.SysUiMobileTypeSource
import com.chaners.guiyuan.xposed.network.SignalStrength

internal object NativePresentationResolver {
    private data class DrawableFields(val type: Field?, val enhanced: Field?, val textPaint: Field?)

    private val drawableFields = ConcurrentHashMap<Class<*>, DrawableFields>()

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
        val platformSubscriptions =
            bindings
                .firstOrNull()
                ?.root
                ?.context
                ?.let(::activeSubscriptions)
        val platformActive = platformSubscriptions?.map { it.first }?.toSet()
        val resolvedActive =
            resolveActiveSubIds(
                boundSubscriptionIds =
                    bindings.map { binding -> binding.subscriptionId },
                semanticActiveSubscriptionIds = semanticActiveSubIds,
                authoritativeActiveSubscriptionIds =
                    platformActive,
            )
        val activeBindingSubIds = resolvedActive.subscriptionIds
        val slotSubscriptionIds =
            resolveSlotSubscriptions(
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
            slotSubscriptionIds = slotSubscriptionIds,
            activeSubsAuthoritative = resolvedActive.authoritative,
            platformActiveSubscriptionIds = platformActive,
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

        NativeParticipantAccess.findResourceView(root, MOBILE_TYPE_RESOURCE_ENTRY)
            ?.let { view ->
                val image = view as? ImageView
                val drawable = image?.drawable
                val networkType =
                    drawable?.let { nativeDrawableType(it, it === pendingMobileTypeDrawable) }
                if (networkType != null) {
                    // Native source and font must come from the same verified Drawable.
                    // Keep only immutable Typeface; never retain a SystemUI Paint/View.
                    val face =
                        drawable
                            .takeIf { it.javaClass.name == SysUiMobileTypeSource.CLASS_NAME }
                            ?.let { native ->
                                runCatching {
                                    (drawableFields[native.javaClass]?.textPaint?.get(native) as? Paint)
                                        ?.typeface
                                }.getOrNull()
                            }
                    return networkType.copy(
                        typeface = face,
                        fontPath =
                            drawable.takeIf { face != null }
                                ?.let { nativeFontPath(it, face!!) },
                    )
                }
            }

        NativeParticipantAccess.findResourceView(root, MOBILE_TYPE_SINGLE_RESOURCE_ENTRY)
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

    // The static native glyph face is available even when Wi-Fi hides the mobile type.
    // Follow SystemUI's own fallback selection; never substitute a guessed app font.
    fun sandboxMobileTypeface(context: Context): Typeface? = runCatching {
        val (face, path) = verifiedMobileFont(context) ?: return@runCatching null
        val file = File(path)
        if (!file.isFile || file.length() !in 1..(64L * 1024 * 1024)) return@runCatching null
        val axis = FileInputStream(file).channel.use { channel ->
            FontAxes.weight(channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size()))
        }?.let(NativeFontAxis::checked)
        face.takeIf { axis != null }
    }.getOrNull()

    // This static SystemUI face remains authoritative when no mobile type is visible.
    fun verifiedMobileFont(context: Context): Pair<Typeface, String>? = runCatching {
        val loader = context.classLoader
        val clazz = loader.loadClass(SysUiMobileTypeSource.CLASS_NAME)
        val face = clazz.getField("sMiproTypeface").get(null) as? Typeface
            ?: return@runCatching null
        val paint = Paint()
        clazz.getMethod("setMiuiStatusBarTypeface", Array<Paint>::class.java)
            .invoke(null, arrayOf(paint) as Any)
        if (paint.typeface !== face) return@runCatching null
        val config = loader.loadClass("com.miui.utils.configs.MiuiConfigs")
        val path = config.getField("FONT_MIPRO_PATH").get(null) as? String
            ?: return@runCatching null
        face to path
    }.getOrNull()

    // Only the exact Typeface built from MiuiConfigs.FONT_MIPRO_PATH proves file identity.
    private fun nativeFontPath(drawable: Drawable, face: Typeface): String? =
        runCatching {
            val clazz = drawable.javaClass
            val original = clazz.getField("sMiproTypeface").get(null) as? Typeface
            if (original !== face) return@runCatching null
            val configs = clazz.classLoader!!.loadClass("com.miui.utils.configs.MiuiConfigs")
            configs.getField("FONT_MIPRO_PATH").get(null) as? String
        }.getOrNull()

    // Snapshot the verified native Paint; never retain its mutable SystemUI owner.
    internal fun nativeMobileTextPaint(view: View?): Paint? {
        val drawable = (view as? ImageView)?.drawable ?: return null
        if (drawable.javaClass.name != SysUiMobileTypeSource.CLASS_NAME) return null
        val fields = drawableFields[drawable.javaClass] ?: return null
        return runCatching {
            (fields.textPaint?.get(drawable) as? Paint)?.let { Paint(it) }
        }.getOrNull()
    }

    // The visual CC target need not belong to the active data subscription.
    internal fun nativeTargetType(view: View?): NetworkType? =
        when (view) {
            is ImageView -> view.drawable?.let { nativeDrawableType(it, false) }
            is TextView ->
                view.text?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { NetworkType(it, false, NetworkTypeSource.MOBILE_TYPE_SINGLE) }
            else -> null
        }

    private fun nativeDrawableType(drawable: Drawable, beforeMeasure: Boolean): NetworkType? {
        val fields =
            drawableFields.computeIfAbsent(drawable.javaClass) { clazz ->
                fun field(name: String): Field? =
                    runCatching { clazz.getDeclaredField(name).apply { isAccessible = true } }
                        .getOrNull()
                DrawableFields(
                    type = field(MOBILE_TYPE_FIELD),
                    enhanced = field(MOBILE_TYPE_ENHANCED_FIELD),
                    textPaint = field("mMobileTypeTextPaint"),
                )
            }
        // Drawables can change text without replacing their ImageView.
        val label = runCatching { fields.type?.get(drawable) as? String }.getOrNull()
        val enhanced =
            runCatching { fields.enhanced?.getBoolean(drawable) }.getOrNull() == true
        return normalizeDrawableNetworkType(label.orEmpty(), enhanced, beforeMeasure)
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

    private fun activeSubscriptions(context: Context): List<Pair<Int, Int>>? {
        val manager =
            context.getSystemService(SubscriptionManager::class.java)
                ?: return null

        // null means the platform source is unavailable; an empty list is authoritative.
        return runCatching {
            manager.activeSubscriptionInfoList
                ?.filter { info -> info.subscriptionId >= 0 }
                ?.map { info -> info.subscriptionId to info.simSlotIndex }
        }.getOrNull()
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

    private const val MOBILE_TYPE_RESOURCE_ENTRY = "mobile_type"
    private const val MOBILE_TYPE_SINGLE_RESOURCE_ENTRY = "mobile_type_single"
    private const val MOBILE_TYPE_FIELD = "mMobileType"
    private const val MOBILE_TYPE_ENHANCED_FIELD = "mShowMobileTypeDoublePlus"
    private const val MOBILE_TYPE_DOUBLE_PLUS_LABEL = "5G++"
    private const val MOBILE_TYPE_DOUBLE_PLUS_BASE_LABEL = "5G"
}
