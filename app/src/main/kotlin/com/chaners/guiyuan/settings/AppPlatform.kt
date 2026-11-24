package com.chaners.guiyuan.settings

import android.app.LocaleManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList

internal enum class AppLang(
    val tag: String?,
) {
    System(null),
    English("en"),
    SimplifiedChinese("zh-CN"),
}

internal object AppPlatform {
    private const val ALIAS_SUFFIX = ".LauncherAlias"

    fun language(ctx: Context): AppLang {
        val locales = ctx.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) return AppLang.System

        val tag = locales[0].toLanguageTag()
        return AppLang.entries.firstOrNull { lang ->
            lang.tag?.equals(tag, ignoreCase = true) == true
        } ?: when (locales[0].language) {
            "zh" -> AppLang.SimplifiedChinese
            "en" -> AppLang.English
            else -> AppLang.System
        }
    }

    fun setLanguage(
        ctx: Context,
        lang: AppLang,
    ) {
        val manager = ctx.getSystemService(LocaleManager::class.java)
        manager.applicationLocales =
            lang.tag?.let(LocaleList::forLanguageTags)
                ?: LocaleList.getEmptyLocaleList()
    }

    fun iconHidden(ctx: Context): Boolean =
        when (
            ctx.packageManager.getComponentEnabledSetting(
                launcher(ctx),
            )
        ) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> true

            else -> false
        }

    fun setIconHidden(
        ctx: Context,
        hidden: Boolean,
    ) {
        // Toggle only the launcher alias; keep this process alive so the UI updates in place.
        ctx.packageManager.setComponentEnabledSetting(
            launcher(ctx),
            if (hidden) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            },
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun launcher(ctx: Context): ComponentName =
        ComponentName(
            ctx.packageName,
            ctx.packageName + ALIAS_SUFFIX,
        )
}
