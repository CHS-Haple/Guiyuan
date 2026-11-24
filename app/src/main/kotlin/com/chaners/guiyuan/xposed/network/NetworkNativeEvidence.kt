package com.chaners.guiyuan.xposed.network

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import com.chaners.guiyuan.xposed.NativeStatusIdentity
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import com.chaners.guiyuan.xposed.tintHexDigits

// Native read-only witnesses and formatting; never publishes network state.
internal object NetworkNativeEvidence {
    private const val MAX_PARENT_CHAIN_DEPTH = 8

    // Diagnostic-only witness: native activity may change visibility, image alpha
    // or drawable state without changing the resource ID we currently transfer.
    // Do not synthesize an activity event from these observations.
    fun nativeActivityObservation(image: ImageView, hookResult: Any?): String {
        val drawable = image.drawable
        val nativeId = (image.tag as? Number)?.toInt() ?: 0
        val nativeName = nativeId.takeIf { it > 0 }?.let { resourceName(image, it) }
        return "viewId=" + NativeStatusIdentity.resourceId(image) +
            " tag=" + nativeId +
            " tagName=" + (nativeName ?: "none") +
            " visibility=" + NativeStatusIdentity.visibilityName(image.visibility) +
            " shown=" + image.isShown +
            " viewAlpha=" + image.alpha +
            " imageAlpha=" + image.imageAlpha +
            " drawableType=" + (drawable?.javaClass?.simpleName ?: "none") +
            " drawableAlpha=" + (drawable?.alpha ?: -1) +
            " drawableLevel=" + (drawable?.level ?: -1) +
            " nativeResult=" + (hookResult?.javaClass?.simpleName ?: "null")
    }

    fun parentChain(view: View): String =
        buildList {
            var current: View? = view
            for (index in 0 until MAX_PARENT_CHAIN_DEPTH) {
                val value = current ?: break
                add(
                    value.javaClass.simpleName +
                        "[" + NativeStatusIdentity.resourceId(value) + "]",
                )
                current = value.parent as? View
            }
        }.joinToString(">")

    fun layoutToken(view: View): String {
        val params = view.layoutParams
        return if (params == null) {
            "none"
        } else {
            params.width.toString() + "x" + params.height +
                ":measured=" + view.measuredWidth + "x" + view.measuredHeight
        }
    }

    fun wifiPresentationToken(image: ImageView): String {
        val drawable = image.drawable
        val bounds = drawable?.bounds
        val tint = image.imageTintList
        return buildString {
            append("drawable=")
            append(drawable?.javaClass?.simpleName ?: "none")
            append(" intrinsic=")
            append(drawable?.intrinsicWidth ?: 0)
            append('x')
            append(drawable?.intrinsicHeight ?: 0)
            append(" bounds=")
            if (bounds == null) {
                append("none")
            } else {
                append(bounds.left)
                append(',')
                append(bounds.top)
                append(',')
                append(bounds.right)
                append(',')
                append(bounds.bottom)
            }
            append(" drawableAlpha=")
            append(drawable?.alpha ?: -1)
            append(" imageAlpha=")
            append(image.imageAlpha)
            append(" tint=")
            append(colorStateListToken(tint, image.drawableState))
            append(" tintMode=")
            append(image.imageTintMode?.name ?: "none")
            append(" colorFilter=")
            append(image.colorFilter?.javaClass?.simpleName ?: "none")
            append(" scaleType=")
            append(image.scaleType?.name ?: "none")
            append(" matrix=")
            append(image.imageMatrix?.toShortString() ?: "none")
            append(" padding=")
            append(image.paddingLeft)
            append(',')
            append(image.paddingTop)
            append(',')
            append(image.paddingRight)
            append(',')
            append(image.paddingBottom)
            append(" measured=")
            append(image.measuredWidth)
            append('x')
            append(image.measuredHeight)
        }
    }

    private fun colorStateListToken(
        tint: ColorStateList?,
        state: IntArray,
    ): String {
        if (tint == null) return "none"
        val resolved = tint.getColorForState(state, tint.defaultColor)
        return "0x" + tintHexDigits(resolved) +
            "/default=0x" + tintHexDigits(tint.defaultColor)
    }

    fun resourceName(
        view: View,
        resId: Int,
    ): String {
        if (resId == 0) {
            return "none"
        }

        return optionalNativeRead {
            view.resources.getResourceName(resId)
        } ?: resId.toString()
    }
}
