package com.chaners.guiyuan.settings

import android.app.LocaleManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList

internal enum class AppLanguage(
    val languageTag: String?,
) {
    System(null),
    English("en"),
    SimplifiedChinese("zh-CN"),
}

internal object AppPlatformSettings {
    private const val LauncherAliasSuffix = ".LauncherAlias"

    fun currentLanguage(context: Context): AppLanguage {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) {
            return AppLanguage.System
        }

        val tag = locales[0].toLanguageTag()
        return AppLanguage.entries.firstOrNull { language ->
            language.languageTag?.equals(tag, ignoreCase = true) == true
        } ?: when (locales[0].language) {
            "zh" -> AppLanguage.SimplifiedChinese
            "en" -> AppLanguage.English
            else -> AppLanguage.System
        }
    }

    fun setLanguage(
        context: Context,
        language: AppLanguage,
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
        // 这里只切桌面入口，不重启进程，设置页可以原地更新状态。
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
            context.packageName + LauncherAliasSuffix,
        )
}
