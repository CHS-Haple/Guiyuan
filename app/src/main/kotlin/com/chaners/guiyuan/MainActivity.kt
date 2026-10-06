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
import com.chaners.guiyuan.settings.AppPlatform
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.AppearanceRepo
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.ui.GyApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        (application as GyApplication).refreshXposedStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialDark =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { initialDark },
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { initialDark },
        )
        window.isNavigationBarContrastEnforced = false

        val repo = AppearanceRepo(applicationContext)
        val initialLanguage = AppPlatform.currentLanguage(this)
        val initialIconHidden = AppPlatform.isLauncherIconHidden(this)

        setContent {
            val settings by repo.settings.collectAsState(initial = Appearance())
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val darkMode =
                when (settings.themeMode) {
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                    ThemeMode.System -> systemDark
                }
            var appLanguage by remember {
                mutableStateOf(initialLanguage)
            }
            var launcherIconHidden by remember {
                mutableStateOf(initialIconHidden)
            }

            DisposableEffect(darkMode) {
                updateBarIcons(darkMode)
                onDispose { }
            }

            GyApp(
                settings = settings,
                darkMode = darkMode,
                appLanguage = appLanguage,
                launcherIconHidden = launcherIconHidden,
                onHotReload = { onComplete ->
                    (application as GyApplication).hotReloadSysUi(onComplete)
                },
                onThemeModeChange = { mode ->
                    scope.launch { repo.setThemeMode(mode) }
                },
                onDynamicColorEnabledChange = { enabled ->
                    scope.launch { repo.setDynamicColorEnabled(enabled) }
                },
                onFloatingNavigationBarEnabledChange = { enabled ->
                    scope.launch { repo.setFloatingNavigationBarEnabled(enabled) }
                },
                onNavStyleChange = { style: NavStyle ->
                    scope.launch { repo.setNavStyle(style) }
                },
                onNavContentChange = { content: NavContent ->
                    scope.launch { repo.setNavContent(content) }
                },
                onSwipeBackEnabledChange = { enabled ->
                    scope.launch { repo.setSwipeBackEnabled(enabled) }
                },
                onAppLangChange = { language ->
                    if (language != appLanguage) {
                        appLanguage = language
                        AppPlatform.setLanguage(this, language)
                    }
                },
                onLauncherIconHiddenChange = { hidden ->
                    AppPlatform.setLauncherIconHidden(this, hidden)
                    launcherIconHidden = hidden
                },
            )
        }
    }

    private fun updateBarIcons(darkMode: Boolean) {
        val lightBarsMask =
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(
            if (darkMode) 0 else lightBarsMask,
            lightBarsMask,
        )
    }
}
