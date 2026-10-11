package com.chaners.guiyuan.xposed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import com.chaners.guiyuan.settings.SettingsEntry
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
    private const val HEADER_ID = 0x475955414E4CL

    fun install(module: XposedModule, loader: ClassLoader, entry: SettingsEntry) {
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
        }.onFailure { Log.w(TAG, "Unsupported Settings header contract", it) }
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
