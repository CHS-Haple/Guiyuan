package com.chaners.guiyuan.settings

import android.app.LocaleManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList

internal enum class AppLang(
    val languageTag: String?,
) {
    System(null),
    English("en"),
    SimplifiedChinese("zh-CN"),
}

internal object AppPlatform {
    private const val ALIAS_SUFFIX = ".LauncherAlias"

    fun currentLanguage(context: Context): AppLang {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) {
            return AppLang.System
        }

        val tag = locales[0].toLanguageTag()
        return AppLang.entries.firstOrNull { language ->
            language.languageTag?.equals(tag, ignoreCase = true) == true
        } ?: when (locales[0].language) {
            "zh" -> AppLang.SimplifiedChinese
            "en" -> AppLang.English
            else -> AppLang.System
        }
    }

    fun setLanguage(
        context: Context,
        language: AppLang,
    ) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = language.languageTag
            ?.let(LocaleList::forLanguageTags)
            ?: LocaleList.getEmptyLocaleList()
    }

    fun isLauncherIconHidden(context: Context): Boolean {
        return when (
            context.packageManager.getComponentEnabledSetting(
                launcherComponent(context),
            )
        ) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> true

            else -> false
        }
    }

    fun setLauncherIconHidden(
        context: Context,
        hidden: Boolean,
    ) {
        // Toggle only the launcher alias; keep this process alive so the UI updates in place.
        context.packageManager.setComponentEnabledSetting(
            launcherComponent(context),
            if (hidden) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            },
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun launcherComponent(context: Context): ComponentName =
        ComponentName(
            context.packageName,
            context.packageName + ALIAS_SUFFIX,
        )
}
