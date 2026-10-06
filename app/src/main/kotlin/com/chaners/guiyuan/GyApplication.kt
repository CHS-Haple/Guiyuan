package com.chaners.guiyuan

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.chaners.guiyuan.settings.COMBINED_STATUS_AOD_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_FEATURE_PREFS_NAME
import com.chaners.guiyuan.settings.COMBINED_STATUS_KEYGUARD_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_VISUAL_PREFS_NAME
import com.chaners.guiyuan.settings.DIAG_LEVEL_KEY
import com.chaners.guiyuan.settings.DIAG_PREFS
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.settings.isVisualPreferenceKey
import com.chaners.guiyuan.settings.migrateBatteryTopChargingScaleReferenceIfNeeded
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
        getSharedPreferences(DIAG_PREFS, Context.MODE_PRIVATE)
    }

    private val featurePrefs: SharedPreferences by lazy {
        getSharedPreferences(COMBINED_STATUS_FEATURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val visualPrefs: SharedPreferences by lazy {
        getSharedPreferences(COMBINED_STATUS_VISUAL_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var xposedService: XposedService? = null

    private val handler = Handler(Looper.getMainLooper())
    private val _xposedStatus =
        MutableStateFlow<XposedStatus>(XposedStatus.Checking)
    internal val xposedStatus: StateFlow<XposedStatus> =
        _xposedStatus.asStateFlow()

    private val bindTimeout =
        Runnable {
            if (
                xposedService == null &&
                _xposedStatus.value == XposedStatus.Checking
            ) {
                _xposedStatus.value = XposedStatus.FrameworkUnavailable
            }
        }

    private val diagListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DIAG_LEVEL_KEY) {
                xposedService?.let(::syncRuntime)
            }
        }

    private val featListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == COMBINED_STATUS_ENABLED_KEY ||
                key == COMBINED_STATUS_KEYGUARD_ENABLED_KEY ||
                key == COMBINED_STATUS_AOD_ENABLED_KEY
            ) {
                xposedService?.let(::syncRuntime)
            }
        }

    private val visualListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isVisualPreferenceKey(key)) {
                xposedService?.let(::syncRuntime)
            }
        }

    override fun onCreate() {
        super.onCreate()
        migrateBatteryTopChargingScaleReferenceIfNeeded(visualPrefs)
        diagPrefs.registerOnSharedPreferenceChangeListener(diagListener)
        featurePrefs.registerOnSharedPreferenceChangeListener(featListener)
        visualPrefs.registerOnSharedPreferenceChangeListener(visualListener)
        XposedServiceHelper.registerListener(this)
        handler.postDelayed(
            bindTimeout,
            XPOSED_BIND_TIMEOUT_MS,
        )
    }

    override fun onServiceBind(service: XposedService) {
        handler.removeCallbacks(bindTimeout)
        xposedService = service
        syncRuntime(service)
        refreshXposedStatus(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
            _xposedStatus.value = XposedStatus.FrameworkUnavailable
        }
    }

    override fun onTerminate() {
        diagPrefs.unregisterOnSharedPreferenceChangeListener(diagListener)
        featurePrefs.unregisterOnSharedPreferenceChangeListener(featListener)
        visualPrefs.unregisterOnSharedPreferenceChangeListener(visualListener)
        handler.removeCallbacks(bindTimeout)
        xposedService = null
        super.onTerminate()
    }

    internal fun refreshXposedStatus() {
        xposedService?.let(::refreshXposedStatus)
    }

    fun hotReloadSysUi(onComplete: () -> Unit = {}): Boolean {
        val service = xposedService ?: return false
        if (service.apiVersion < 102) return false

        val target =
            runCatching {
                service.runningTargets.firstOrNull { it.processName == SYS_UI_PROCESS }
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
        _xposedStatus.value =
            runCatching {
                val running =
                    service.runningTargets.any { target ->
                        target.processName == SYS_UI_PROCESS
                    }
                val inScope =
                    service.scope.any { packageName ->
                        packageName == SYS_UI_PROCESS
                    }

                XposedStatus.Connected(
                    sysUiInScope = inScope,
                    sysUiRunning = running,
                )
            }.getOrElse { throwable ->
                Log.w(
                    TAG,
                    "Unable to query Xposed runtime status: " + throwable.message,
                )
                XposedStatus.QueryUnavailable
            }
    }

    // App prefs stay authoritative; Xposed receives one mirrored runtime snapshot.
    private fun syncRuntime(service: XposedService) {
        val level =
            diagPrefs.getString(
                DIAG_LEVEL_KEY,
                DiagLevel.General.name,
            ) ?: DiagLevel.General.name
        val enabled =
            featurePrefs.getBoolean(
                COMBINED_STATUS_ENABLED_KEY,
                true,
            )
        val keyguard =
            featurePrefs.getBoolean(
                COMBINED_STATUS_KEYGUARD_ENABLED_KEY,
                false,
            )
        val aod =
            featurePrefs.getBoolean(
                COMBINED_STATUS_AOD_ENABLED_KEY,
                false,
            )
        val featureChangedAtNs =
            featurePrefs.getLong(
                COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                0L,
            )
        val visual =
            visualPrefs.readVisualSettings()

        runCatching {
            val remote = service.getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME)
            val editor = remote.edit() ?: error("remote preference editor unavailable")
            editor
                .putString(DIAG_LEVEL_KEY, level)
                .putBoolean(
                    COMBINED_STATUS_ENABLED_KEY,
                    enabled,
                )
                .putBoolean(
                    COMBINED_STATUS_KEYGUARD_ENABLED_KEY,
                    keyguard,
                )
                .putBoolean(
                    COMBINED_STATUS_AOD_ENABLED_KEY,
                    aod,
                )
                .putLong(
                    COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                    featureChangedAtNs,
                )
                .putVisualSettings(visual)
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
        const val SYS_UI_PROCESS = "com.android.systemui"
        const val XPOSED_BIND_TIMEOUT_MS = 1_000L
    }
}
