package com.chaners.guiyuan.xposed

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.lang.reflect.Field
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import com.chaners.guiyuan.settings.FontAxes
import com.chaners.guiyuan.settings.NativeFontAxis
import java.util.concurrent.ConcurrentHashMap
import com.chaners.guiyuan.xposed.network.SysUiMobileTypeSource
import com.chaners.guiyuan.xposed.NativePresentationResolver.NetworkType
import com.chaners.guiyuan.xposed.NativePresentationResolver.NetworkTypeSource

// Owns the SystemUI mobile-type drawable and font reflection cache, not subscription selection.
internal object NativeMobileTypePresentation {
    private data class DrawableFields(val type: Field?, val enhanced: Field?, val textPaint: Field?)

    private val drawableFields = ConcurrentHashMap<Class<*>, DrawableFields>()

    internal fun resolveNetworkType(
        root: ViewGroup,
        pendingMobileTypeDrawable: Drawable?,
    ): NetworkType? {

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
                                NativePresentationResolver.readOrNull {
                                    (drawableFields[native.javaClass]?.textPaint?.get(native) as? Paint)
                                        ?.typeface
                                }
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
    fun sandboxMobileTypeface(context: Context): Typeface? = NativePresentationResolver.readOrNull {
        val verified = verifiedMobileFont(context)
        if (verified == null) {
            null
        } else {
            val (face, path) = verified
            val file = File(path)
            if (file.isFile && file.length() in 1..(64L * 1024 * 1024)) {
                val axis = FileInputStream(file).channel.use { channel ->
                    FontAxes.weight(channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size()))
                }?.let(NativeFontAxis::checked)
                face.takeIf { axis != null }
            } else {
                null
            }
        }
    }

    // This static SystemUI face remains authoritative when no mobile type is visible.
    fun verifiedMobileFont(context: Context): Pair<Typeface, String>? = NativePresentationResolver.readOrNull {
        val loader = context.classLoader
        val clazz = loader.loadClass(SysUiMobileTypeSource.CLASS_NAME)
        val face = clazz.getField("sMiproTypeface").get(null) as? Typeface
        if (face == null) {
            null
        } else {
            val paint = Paint()
            clazz.getMethod("setMiuiStatusBarTypeface", Array<Paint>::class.java)
                .invoke(null, arrayOf(paint) as Any)
            if (paint.typeface === face) {
                val config = loader.loadClass("com.miui.utils.configs.MiuiConfigs")
                (config.getField("FONT_MIPRO_PATH").get(null) as? String)?.let { face to it }
            } else {
                null
            }
        }
    }

    // Only the exact Typeface built from MiuiConfigs.FONT_MIPRO_PATH proves file identity.
    private fun nativeFontPath(drawable: Drawable, face: Typeface): String? =
        NativePresentationResolver.readOrNull {
            val clazz = drawable.javaClass
            val original = clazz.getField("sMiproTypeface").get(null) as? Typeface
            if (original === face) {
                val configs = clazz.classLoader!!.loadClass("com.miui.utils.configs.MiuiConfigs")
                configs.getField("FONT_MIPRO_PATH").get(null) as? String
            } else {
                null
            }
        }

    // Snapshot the verified native Paint; never retain its mutable SystemUI owner.
    internal fun nativeMobileTextPaint(view: View?): Paint? {
        val drawable = (view as? ImageView)?.drawable ?: return null
        if (drawable.javaClass.name != SysUiMobileTypeSource.CLASS_NAME) return null
        val fields = drawableFields[drawable.javaClass] ?: return null
        return NativePresentationResolver.readOrNull {
            (fields.textPaint?.get(drawable) as? Paint)?.let { Paint(it) }
        }
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
                    NativePresentationResolver.readOrNull { clazz.getDeclaredField(name).apply { isAccessible = true } }
                DrawableFields(
                    type = field(MOBILE_TYPE_FIELD),
                    enhanced = field(MOBILE_TYPE_ENHANCED_FIELD),
                    textPaint = field("mMobileTypeTextPaint"),
                )
            }
        // Drawables can change text without replacing their ImageView.
        val label = NativePresentationResolver.readOrNull { fields.type?.get(drawable) as? String }
        val enhanced =
            NativePresentationResolver.readOrNull { fields.enhanced?.getBoolean(drawable) } == true
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

    private const val MOBILE_TYPE_RESOURCE_ENTRY = "mobile_type"
    private const val MOBILE_TYPE_SINGLE_RESOURCE_ENTRY = "mobile_type_single"
    private const val MOBILE_TYPE_FIELD = "mMobileType"
    private const val MOBILE_TYPE_ENHANCED_FIELD = "mShowMobileTypeDoublePlus"
    private const val MOBILE_TYPE_DOUBLE_PLUS_LABEL = "5G++"
    private const val MOBILE_TYPE_DOUBLE_PLUS_BASE_LABEL = "5G"
}
