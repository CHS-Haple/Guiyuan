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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.chaners.guiyuan.settings.AppPlatform
import com.chaners.guiyuan.settings.ThemeMode
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.AppearanceRepo
import com.chaners.guiyuan.settings.LiquidMode
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.ui.GyApp
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
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
        val initialLang = AppPlatform.language(this)
        val initialIconHidden = AppPlatform.iconHidden(this)

        setContent {
            val appearance by repo.settings.collectAsState(initial = Appearance())
            val scope = rememberCoroutineScope()
            val sysDark = isSystemInDarkTheme()
            val dark =
                when (appearance.theme) {
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                    ThemeMode.System -> sysDark
                }
            // Sandbox only overrides this Activity window; never the saved app theme.
            var previewBarDark by remember { mutableStateOf<Boolean?>(null) }
            var lang by remember {
                mutableStateOf(initialLang)
            }
            var iconHidden by remember {
                mutableStateOf(initialIconHidden)
            }

            DisposableEffect(previewBarDark, dark) {
                updateBarIcons(previewBarDark ?: dark)
                onDispose { }
            }

            val systemDensity = LocalDensity.current
            val uiDensity = remember(systemDensity, appearance.uiScalePercent) {
                Density(
                    systemDensity.density * appearance.uiScalePercent / 100f,
                    systemDensity.fontScale,
                )
            }
            CompositionLocalProvider(LocalDensity provides uiDensity) {
                GyApp(
                    appearance = appearance,
                    dark = dark,
                    onPreviewThemeChange = { previewBarDark = it },
                    lang = lang,
                    iconHidden = iconHidden,
                    onHotReload = {
                        (application as GyApplication).hotReloadSysUi()
                    },
                    onThemeChange = { mode ->
                        scope.launch { repo.setTheme(mode) }
                    },
                    onDynamicChange = { enabled ->
                        scope.launch { repo.setDynamicColor(enabled) }
                    },
                    onColorSpecChange = { spec: ThemeColorSpec? ->
                        scope.launch { repo.setColorSpec(spec) }
                    },
                    onPaletteStyleChange = { style: ThemePaletteStyle ->
                        scope.launch { repo.setPaletteStyle(style) }
                    },
                    onNavEnabledChange = { enabled ->
                        scope.launch { repo.setNavEnabled(enabled) }
                    },
                    onNavStyleChange = { style: NavStyle ->
                        scope.launch { repo.setNavStyle(style) }
                    },
                    onNavContentChange = { content: NavContent ->
                        scope.launch { repo.setNavContent(content) }
                    },
                    onLiquidModeChange = { mode: LiquidMode ->
                        scope.launch { repo.setLiquidMode(mode) }
                    },
                    onSwipeBackChange = { enabled ->
                        scope.launch { repo.setSwipeBack(enabled) }
                    },
                    onUiScaleChange = { percent ->
                        scope.launch { repo.setUiScalePercent(percent) }
                    },
                    onLangChange = { language ->
                        if (language != lang) {
                            lang = language
                            AppPlatform.setLanguage(this, language)
                        }
                    },
                    onIconHiddenChange = { hidden ->
                        AppPlatform.setIconHidden(this, hidden)
                        iconHidden = hidden
                    },
                )
            }
        }
    }

    private fun updateBarIcons(dark: Boolean) {
        val lightBarsMask =
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(
            if (dark) 0 else lightBarsMask,
            lightBarsMask,
        )
    }
}
