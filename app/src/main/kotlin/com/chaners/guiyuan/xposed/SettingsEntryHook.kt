package com.chaners.guiyuan.xposed

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.chaners.guiyuan.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import com.chaners.guiyuan.settings.SettingsEntry
import com.chaners.guiyuan.settings.SettingsEntryIcon
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.util.ArrayList

// An unrecognized Settings list stays entirely native.
internal object SettingsEntryHook {
    private const val TAG = "Guiyuan[Settings]"
    private const val MODULE_PKG = "com.chaners.guiyuan"
    private const val HEADER_CLASS =
        "com.android.settingslib.miuisettings.preference.PreferenceActivity\$Header"
    private const val HOST_CLASS = "com.android.settings.MiuiSettings"
    private const val HOOK_ID = "guiyuan.settings.header"
    private const val ICON_HOOK_ID = "guiyuan.settings.header.icon"
    private const val HEADER_ID = 0x475955414E4CL

    fun install(
        module: XposedModule,
        loader: ClassLoader,
        entry: SettingsEntry,
        style: SettingsEntryIcon,
    ) {
        if (entry == SettingsEntry.Disabled) return

        runCatching {
            val host = Class.forName(HOST_CLASS, false, loader)
            val headerClass = Class.forName(HEADER_CLASS, false, loader)
            val method = host.getDeclaredMethod("updateHeaderList", List::class.java)
            val id = headerClass.findField("id")
            val group = headerClass.findField("groupId")
            val title = headerClass.findField("title")
            val icon = headerClass.findField("iconRes")
            val intent = headerClass.findField("intent")
            val extras = headerClass.findField("extras")
            val ctor = headerClass.getDeclaredConstructor().apply { isAccessible = true }

            module.hook(method).setId(HOOK_ID).intercept(Hooker { chain ->
                val result = chain.proceed()
                runCatching {
                    val activity = chain.thisObject as? Activity ?: return@runCatching
                    @Suppress("UNCHECKED_CAST")
                    val headers = chain.getArg(0) as? MutableList<Any> ?: return@runCatching
                    if (headers.any { !headerClass.isInstance(it) }) return@runCatching
                    if (headers.any { id.getLong(it) == HEADER_ID }) return@runCatching
                    val anchor = anchorIndex(headers.map { id.getLong(it) }, activity, entry)
                        ?: return@runCatching

                    val header = ctor.newInstance()
                    id.setLong(header, HEADER_ID)
                    group.setInt(header, group.getInt(headers[anchor]))
                    // Android owns this resource; module APK resource IDs are not Settings IDs.
                    icon.setInt(header, android.R.drawable.ic_menu_preferences)
                    val app = activity.createPackageContext(MODULE_PKG, 0)
                    title.set(header, app.applicationInfo.loadLabel(app.packageManager))
                    intent.set(
                        header,
                        Intent().setClassName(MODULE_PKG, "$MODULE_PKG.MainActivity")
                            .putExtra("isDisplayHomeAsUpEnabled", true),
                    )
                    extras.set(
                        header,
                        Bundle().apply {
                            putParcelableArrayList(
                                "header_user", ArrayList(listOf(Process.myUserHandle())),
                            )
                        },
                    )
                    headers.add(anchor + 1, header)
                }.onFailure { Log.w(TAG, "Native Settings header insertion skipped", it) }
                result
            })
            installIcon(module, loader, headerClass, id, style)
        }.onFailure { Log.w(TAG, "Unsupported Settings header contract", it) }
    }

    // The header exposes only iconRes. Bind the module drawable only on our row,
    // after the native adapter has populated it; recycled native rows stay untouched.
    private fun installIcon(
        module: XposedModule,
        loader: ClassLoader,
        headerClass: Class<*>,
        id: Field,
        style: SettingsEntryIcon,
    ) {
        val names = listOf(
            "com.android.settingslib.miuisettings.preference.PreferenceActivity\$HeaderAdapter",
            "android.preference.PreferenceActivity\$HeaderAdapter",
        )
        for (name in names) {
            val adapter = runCatching { Class.forName(name, false, loader) }.getOrNull()
                ?: continue
            if (!BaseAdapter::class.java.isAssignableFrom(adapter)) continue
            val getView = runCatching {
                adapter.getDeclaredMethod(
                    "getView", Int::class.javaPrimitiveType, View::class.java, ViewGroup::class.java,
                )
            }.getOrNull() ?: continue
            val getItem = runCatching {
                adapter.getMethod("getItem", Int::class.javaPrimitiveType)
            }.getOrNull() ?: continue

            var artwork: Drawable.ConstantState? = null
            var nightMode = -1
            var warned = false
            module.hook(getView).setId(ICON_HOOK_ID).intercept(Hooker { chain ->
                val result = chain.proceed()
                runCatching {
                    val row = result as? View ?: return@runCatching
                    val index = chain.getArg(0) as? Int ?: return@runCatching
                    val header = getItem.invoke(chain.thisObject, index)
                    if (!headerClass.isInstance(header) || id.getLong(header) != HEADER_ID) {
                        return@runCatching
                    }

                    val iconId = row.resources.getIdentifier("icon", "id", row.context.packageName)
                    val view = (if (iconId != 0) row.findViewById<View>(iconId) else null)
                        ?: row.findViewById(android.R.id.icon)
                    val image = view as? ImageView ?: return@runCatching
                    val night = row.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    if (nightMode != night) {
                        nightMode = night
                        artwork = createIcon(row.context, style)?.constantState
                    }
                    artwork?.newDrawable(row.resources)?.mutate()?.let(image::setImageDrawable)
                }.onFailure {
                    if (!warned) {
                        warned = true
                        Log.w(TAG, "Guiyuan Settings icon skipped", it)
                    }
                }
                result
            })
            return
        }
        Log.w(TAG, "Native Settings header icon adapter unavailable; keeping native icon")
    }

    private fun createIcon(context: Context, style: SettingsEntryIcon): Drawable? {
        val app = context.createPackageContext(MODULE_PKG, 0)
        val size = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        if (style == SettingsEntryIcon.Rounded) {
            val icon = app.packageManager.getApplicationIcon(MODULE_PKG)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size)
            icon.draw(Canvas(bitmap))
            return BitmapDrawable(context.resources, bitmap)
        }

        val foreground = app.getDrawable(R.drawable.ic_launcher_foreground)?.mutate()
            ?: return null
        val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val tint = try {
            attrs.getColor(0, Color.BLACK)
        } finally {
            attrs.recycle()
        }
        foreground.setTint(tint)

        // Crop the existing app mark's transparent padding; its paths stay canonical.
        val source = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        foreground.setBounds(0, 0, size, size)
        foreground.draw(Canvas(source))
        val pixels = IntArray(size * size)
        source.getPixels(pixels, 0, size, 0, 0, size, size)
        var left = size
        var top = size
        var right = -1
        var bottom = -1
        for (y in 0 until size) for (x in 0 until size) {
            if (pixels[y * size + x] ushr 24 >= 24) {
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        if (right < left) return BitmapDrawable(context.resources, source)
        val width = right - left + 1
        val height = bottom - top + 1
        val scale = size * 0.74f / maxOf(width, height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.translate(
            (size - width * scale) / 2f - left * scale,
            (size - height * scale) / 2f - top * scale,
        )
        canvas.scale(scale, scale)
        foreground.draw(canvas)
        source.recycle()
        return BitmapDrawable(context.resources, output)
    }

    private fun Class<*>.findField(name: String): Field {
        var current: Class<*>? = this
        while (current != null) {
            val clazz = current
            val field = runCatching { clazz.getDeclaredField(name) }.getOrNull()
            if (field != null) return field.apply { isAccessible = true }
            current = clazz.superclass
        }
        error("Missing Header field: $name")
    }

    private fun anchorIndex(ids: List<Long>, context: Context, entry: SettingsEntry): Int? {
        val name = when (entry) {
            SettingsEntry.Disabled -> return null
            SettingsEntry.Top -> "my_device"
            SettingsEntry.Middle -> "launcher_settings"
            SettingsEntry.Bottom -> "app_timer"
        }
        val res = context.resources.getIdentifier(name, "id", context.packageName)
        if (res == 0) return null
        return ids.indexOf(res.toLong()).takeIf { it >= 0 }
    }
}
