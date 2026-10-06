package com.chaners.guiyuan

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.chaners.guiyuan.settings.AOD_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_PREFS_NAME
import com.chaners.guiyuan.settings.KEYGUARD_ENABLED_KEY
import com.chaners.guiyuan.settings.VISUAL_PREFS_NAME
import com.chaners.guiyuan.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.guiyuan.settings.DIAG_PREFS_NAME
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.settings.isVisualPreferenceKey
import com.chaners.guiyuan.settings.migrateChargingScaleRef
import com.chaners.guiyuan.settings.putVisualSettings
import com.chaners.guiyuan.settings.readVisualSettings
import com.chaners.guiyuan.system.XposedStatus
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GyApplication :
    Application(),
    XposedServiceHelper.OnServiceListener {

    private val diagPrefs: SharedPreferences by lazy {
        getSharedPreferences(DIAG_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val featurePreferences: SharedPreferences by lazy {
        getSharedPreferences(FEATURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val visualPreferences: SharedPreferences by lazy {
        getSharedPreferences(VISUAL_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var xposedService: XposedService? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _xposedRuntimeStatus =
        MutableStateFlow<XposedStatus>(XposedStatus.Checking)
    internal val xposedRuntimeStatus: StateFlow<XposedStatus> =
        _xposedRuntimeStatus.asStateFlow()

    private val xposedServiceBindTimeout =
        Runnable {
            if (
                xposedService == null &&
                _xposedRuntimeStatus.value == XposedStatus.Checking
            ) {
                _xposedRuntimeStatus.value = XposedStatus.FrameworkUnavailable
            }
        }

    private val diagnosticsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DIAGNOSTICS_LEVEL_KEY) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    private val featureListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == FEATURE_ENABLED_KEY ||
                key == KEYGUARD_ENABLED_KEY ||
                key == AOD_ENABLED_KEY
            ) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    private val visualListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isVisualPreferenceKey(key)) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    override fun onCreate() {
        super.onCreate()
        migrateChargingScaleRef(visualPreferences)
        diagPrefs.registerOnSharedPreferenceChangeListener(diagnosticsListener)
        featurePreferences.registerOnSharedPreferenceChangeListener(featureListener)
        visualPreferences.registerOnSharedPreferenceChangeListener(visualListener)
        XposedServiceHelper.registerListener(this)
        mainHandler.postDelayed(
            xposedServiceBindTimeout,
            XPOSED_SERVICE_BIND_TIMEOUT_MS,
        )
    }

    override fun onServiceBind(service: XposedService) {
        mainHandler.removeCallbacks(xposedServiceBindTimeout)
        xposedService = service
        syncRuntimeConfig(service)
        refreshXposedStatus(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
            _xposedRuntimeStatus.value = XposedStatus.FrameworkUnavailable
        }
    }

    override fun onTerminate() {
        diagPrefs.unregisterOnSharedPreferenceChangeListener(diagnosticsListener)
        featurePreferences.unregisterOnSharedPreferenceChangeListener(featureListener)
        visualPreferences.unregisterOnSharedPreferenceChangeListener(visualListener)
        mainHandler.removeCallbacks(xposedServiceBindTimeout)
        xposedService = null
        super.onTerminate()
    }

    internal fun refreshXposedStatus() {
        xposedService?.let(::refreshXposedStatus)
    }

    fun hotReloadSystemUi(onComplete: () -> Unit = {}): Boolean {
        val service = xposedService ?: return false
        if (service.apiVersion < 102) return false

        val target =
            runCatching {
                service.runningTargets.firstOrNull { it.processName == SYSTEM_UI_PROCESS }
            }.getOrElse { throwable ->
                Log.w(TAG, "Unable to query running targets: " + throwable.message)
                return false
            } ?: return false

        return runCatching {
            service.hotReloadModule(target, null) { process, result ->
                Log.i(
                    TAG,
                    "Hot reload completed process=" + process.processName + " result=" + result,
                )
                mainExecutor.execute {
                    refreshXposedStatus()
                    onComplete()
                }
            }
            true
        }.getOrElse { throwable ->
            Log.w(TAG, "Unable to request hot reload: " + throwable.message)
            false
        }
    }

    private fun refreshXposedStatus(service: XposedService) {
        _xposedRuntimeStatus.value =
            runCatching {
                val running =
                    service.runningTargets.any { target ->
                        target.processName == SYSTEM_UI_PROCESS
                    }
                val inScope =
                    service.scope.any { packageName ->
                        packageName == SYSTEM_UI_PROCESS
                    }

                XposedStatus.Connected(
                    systemUiInScope = inScope,
                    systemUiRunning = running,
                )
            }.getOrElse { throwable ->
                Log.w(
                    TAG,
                    "Unable to query Xposed runtime status: " + throwable.message,
                )
                XposedStatus.QueryUnavailable
            }
    }

    private fun syncRuntimeConfig(service: XposedService) {
        val level =
            diagPrefs.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagLevel.General.name,
            ) ?: DiagLevel.General.name
        val combinedStatusEnabled =
            featurePreferences.getBoolean(
                FEATURE_ENABLED_KEY,
                true,
            )
        val keyguardEnabled =
            featurePreferences.getBoolean(
                KEYGUARD_ENABLED_KEY,
                false,
            )
        val aodEnabled =
            featurePreferences.getBoolean(
                AOD_ENABLED_KEY,
                false,
            )
        val featureChangedNs =
            featurePreferences.getLong(
                FEATURE_CHANGED_NS_KEY,
                0L,
            )
        val visualSettings =
            visualPreferences.readVisualSettings()

        runCatching {
            val remote = service.getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME)
            val editor = remote.edit() ?: error("remote preference editor unavailable")
            editor
                .putString(DIAGNOSTICS_LEVEL_KEY, level)
                .putBoolean(
                    FEATURE_ENABLED_KEY,
                    combinedStatusEnabled,
                )
                .putBoolean(
                    KEYGUARD_ENABLED_KEY,
                    keyguardEnabled,
                )
                .putBoolean(
                    AOD_ENABLED_KEY,
                    aodEnabled,
                )
                .putLong(
                    FEATURE_CHANGED_NS_KEY,
                    featureChangedNs,
                )
                .putVisualSettings(visualSettings)
            check(editor.commit()) { "remote preference commit failed" }
        }.onFailure { throwable ->
            Log.w(
                TAG,
                "Unable to mirror runtime preferences: " + throwable.message,
            )
        }
    }

    private companion object {
        const val TAG = "CombinedStatus[App]"
        const val SYSTEM_UI_PROCESS = "com.android.systemui"
        const val XPOSED_SERVICE_BIND_TIMEOUT_MS = 1_000L
    }
}
