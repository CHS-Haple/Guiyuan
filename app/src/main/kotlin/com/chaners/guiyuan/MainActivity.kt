package com.chaners.guiyuan

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.chaners.guiyuan.settings.AppPlatformSettings
import com.chaners.guiyuan.settings.AppThemeMode
import com.chaners.guiyuan.settings.AppearanceSettings
import com.chaners.guiyuan.settings.AppearanceRepo
import com.chaners.guiyuan.settings.FloatingNavContent
import com.chaners.guiyuan.settings.FloatingNavStyle
import com.chaners.guiyuan.ui.GyApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        (application as GyApplication).refreshXposedStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialSystemDarkMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { initialSystemDarkMode },
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { initialSystemDarkMode },
        )
        window.isNavigationBarContrastEnforced = false

        val repository = AppearanceRepo(applicationContext)
        val initialAppLanguage = AppPlatformSettings.currentLanguage(this)
        val initialLauncherIconHidden = AppPlatformSettings.isLauncherIconHidden(this)

        setContent {
            val settings by repository.settings.collectAsState(initial = AppearanceSettings())
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val darkMode =
                when (settings.themeMode) {
                    AppThemeMode.Light -> false
                    AppThemeMode.Dark -> true
                    AppThemeMode.System -> systemDark
                }
            var appLanguage by remember {
                mutableStateOf(initialAppLanguage)
            }
            var launcherIconHidden by remember {
                mutableStateOf(initialLauncherIconHidden)
            }

            DisposableEffect(darkMode) {
                updateSystemBarIconAppearance(darkMode)
                onDispose { }
            }

            GyApp(
                settings = settings,
                darkMode = darkMode,
                appLanguage = appLanguage,
                launcherIconHidden = launcherIconHidden,
                onHotReload = { onComplete ->
                    (application as GyApplication).hotReloadSystemUi(onComplete)
                },
                onThemeModeChange = { mode ->
                    scope.launch { repository.setThemeMode(mode) }
                },
                onDynamicColorEnabledChange = { enabled ->
                    scope.launch { repository.setDynamicColorEnabled(enabled) }
                },
                onFloatingNavigationBarEnabledChange = { enabled ->
                    scope.launch { repository.setFloatingNavigationBarEnabled(enabled) }
                },
                onFloatingNavStyleChange = { style: FloatingNavStyle ->
                    scope.launch { repository.setFloatingNavStyle(style) }
                },
                onFloatingNavContentChange = { content: FloatingNavContent ->
                    scope.launch { repository.setFloatingNavContent(content) }
                },
                onSwipeBackEnabledChange = { enabled ->
                    scope.launch { repository.setSwipeBackEnabled(enabled) }
                },
                onAppLanguageChange = { language ->
                    if (language != appLanguage) {
                        appLanguage = language
                        AppPlatformSettings.setLanguage(this, language)
                    }
                },
                onLauncherIconHiddenChange = { hidden ->
                    AppPlatformSettings.setLauncherIconHidden(this, hidden)
                    launcherIconHidden = hidden
                },
            )
        }
    }

    private fun updateSystemBarIconAppearance(darkMode: Boolean) {
        val lightBarsMask =
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(
            if (darkMode) 0 else lightBarsMask,
            lightBarsMask,
        )
    }
}
