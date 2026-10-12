package com.chaners.guiyuan

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.chaners.guiyuan.settings.FEATURE_AOD_DEFAULT
import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_PREFS
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_DEFAULT
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_VISUAL_PREFS_NAME
import com.chaners.guiyuan.settings.DIAG_LEVEL_KEY
import com.chaners.guiyuan.settings.DIAG_PREFS
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.isDiagKey
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.settings.SETTINGS_ENTRY_ICON_KEY
import com.chaners.guiyuan.settings.SETTINGS_ENTRY_KEY
import com.chaners.guiyuan.settings.SETTINGS_ENTRY_PREFS
import com.chaners.guiyuan.settings.isFeatureKey
import com.chaners.guiyuan.settings.isVisualPreferenceKey
import com.chaners.guiyuan.settings.migrateBatteryTopChargingScaleReferenceIfNeeded
import com.chaners.guiyuan.settings.putVisualCfg
import com.chaners.guiyuan.settings.readVisualCfg
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.system.XposedStatus
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GyApplication :
    Application(),
    XposedServiceHelper.OnServiceListener {

    private val diagPrefs: SharedPreferences by lazy {
        getSharedPreferences(DIAG_PREFS, Context.MODE_PRIVATE)
    }

    private val featurePrefs: SharedPreferences by lazy {
        getSharedPreferences(FEATURE_PREFS, Context.MODE_PRIVATE)
    }

    private val visualPrefs: SharedPreferences by lazy {
        getSharedPreferences(COMBINED_STATUS_VISUAL_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val settingsPrefs: SharedPreferences by lazy {
        getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
    }

    @Volatile
    private var xposedService: XposedService? = null

    @Volatile
    private var lastMirror: Pair<XposedService, RuntimeSnapshot>? = null

    private val handler = Handler(Looper.getMainLooper())
    private val pendingScope = MutableStateFlow<ScopeRequest?>(null)
    internal val scopeRequestInFlight: Flow<Boolean> =
        pendingScope.map { it != null }.distinctUntilChanged()
    internal val scopeRequestActive: Boolean
        get() = pendingScope.value != null

    private var hotReloadPending: HotReloadPending? = null
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
            if (isDiagKey(key)) {
                xposedService?.let(::syncRuntime)
            }
        }

    private val featListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isFeatureKey(key)) {
                xposedService?.let(::syncRuntime)
            }
        }

    private val visualListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isVisualPreferenceKey(key)) {
                xposedService?.let(::syncRuntime)
            }
        }

    private val settingsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SETTINGS_ENTRY_KEY || key == SETTINGS_ENTRY_ICON_KEY) {
                xposedService?.let(::syncRuntime)
            }
        }

    override fun onCreate() {
        super.onCreate()
        migrateBatteryTopChargingScaleReferenceIfNeeded(visualPrefs)
        diagPrefs.registerOnSharedPreferenceChangeListener(diagListener)
        featurePrefs.registerOnSharedPreferenceChangeListener(featListener)
        visualPrefs.registerOnSharedPreferenceChangeListener(visualListener)
        settingsPrefs.registerOnSharedPreferenceChangeListener(settingsListener)
        XposedServiceHelper.registerListener(this)
        handler.postDelayed(
            bindTimeout,
            XPOSED_BIND_TIMEOUT_MS,
        )
    }

    override fun onServiceBind(service: XposedService) {
        // Framework Binder callbacks are not confined to the UI thread.
        handler.post {
            handler.removeCallbacks(bindTimeout)
            if (xposedService !== service) {
                finishScopeRequest(false)
                finishHotReload()
            }
            xposedService = service
            lastMirror = null
            syncRuntime(service)
            refreshXposedStatus(service)
        }
    }

    override fun onServiceDied(service: XposedService) {
        handler.post {
            if (xposedService === service) {
                xposedService = null
                lastMirror = null
                finishScopeRequest(false)
                finishHotReload()
                _xposedStatus.value = XposedStatus.FrameworkUnavailable
            }
        }
    }

    override fun onTerminate() {
        diagPrefs.unregisterOnSharedPreferenceChangeListener(diagListener)
        featurePrefs.unregisterOnSharedPreferenceChangeListener(featListener)
        visualPrefs.unregisterOnSharedPreferenceChangeListener(visualListener)
        settingsPrefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
        handler.removeCallbacks(bindTimeout)
        xposedService = null
        lastMirror = null
        pendingScope.value = null
        hotReloadPending = null
        super.onTerminate()
    }

    internal fun refreshXposedStatus() {
        xposedService?.let(::refreshXposedStatus)
    }

    // The framework's scope is authoritative, independent of UI feature toggles.
    fun enabledScopes(): Set<String>? {
        val service = xposedService ?: return null
        if (service.apiVersion < 102) return null
        return runCatching { service.scope.toSet() }
            .onFailure { Log.w(TAG, "Unable to read enabled scopes", it) }
            .getOrNull()
    }

    fun settingsInScope(): Boolean = enabledScopes()?.contains(SETTINGS_PROCESS) == true

    fun requestSettingsScope(onComplete: (Boolean) -> Unit): Boolean =
        requestScope(SETTINGS_PROCESS, onComplete)

    fun requestSysUiScope(onComplete: (Boolean) -> Unit): Boolean =
        requestScope(SYS_UI_PROCESS, onComplete)

    private fun requestScope(pkg: String, onComplete: (Boolean) -> Unit): Boolean {
        val service = xposedService ?: return false
        if (service.apiVersion < 102 || pendingScope.value != null || hotReloadPending != null || ScopeRestart.isRunning) return false

        val inScope = runCatching { pkg in service.scope }
            .getOrElse { return false }
        if (inScope) {
            refreshXposedStatus(service)
            onComplete(true)
            return true
        }

        val request = ScopeRequest(service, onComplete)
        pendingScope.value = request
        return runCatching {
            service.requestScope(
                listOf(pkg),
                object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(scope: List<String>) {
                        mainExecutor.execute {
                            if (xposedService === request.service && pendingScope.value === request) {
                                refreshXposedStatus(request.service)
                                finishScopeRequest(
                                    runCatching { pkg in service.scope }.getOrDefault(false),
                                )
                            }
                        }
                    }

                    override fun onScopeRequestFailed(message: String) {
                        Log.w(TAG, "Scope request failed: $message")
                        mainExecutor.execute {
                            if (xposedService === request.service && pendingScope.value === request) {
                                finishScopeRequest(false)
                            }
                        }
                    }
                },
            )
            true
        }.getOrElse { throwable ->
            if (pendingScope.value === request) pendingScope.value = null
            Log.w(TAG, "Unable to request $pkg scope: " + throwable.message)
            false
        }
    }

    private fun finishScopeRequest(granted: Boolean) {
        val request = pendingScope.value ?: return
        pendingScope.value = null
        request.onComplete(granted)
    }

    fun hotReloadSysUi(onComplete: () -> Unit = {}): Boolean {
        val service = xposedService ?: return false
        if (service.apiVersion < 102 || hotReloadPending != null || pendingScope.value != null || ScopeRestart.isRunning) return false

        val target =
            runCatching {
                service.runningTargets.firstOrNull { it.processName == SYS_UI_PROCESS }
            }.getOrElse { throwable ->
                Log.w(TAG, "Unable to query running targets: " + throwable.message)
                return false
            } ?: return false

        val pending = HotReloadPending(service, onComplete)
        hotReloadPending = pending
        return runCatching {
            service.hotReloadModule(target, null) { process, result ->
                Log.i(
                    TAG,
                    "Hot reload completed process=" + process.processName + " result=" + result,
                )
                mainExecutor.execute {
                    if (xposedService === pending.service && hotReloadPending === pending) {
                        refreshXposedStatus(pending.service)
                        finishHotReload()
                    }
                }
            }
            true
        }.getOrElse { throwable ->
            if (hotReloadPending === pending) hotReloadPending = null
            Log.w(TAG, "Unable to request hot reload: " + throwable.message)
            false
        }
    }

    private fun finishHotReload() {
        val pending = hotReloadPending ?: return
        hotReloadPending = null
        pending.onComplete()
    }

    private fun refreshXposedStatus(service: XposedService) {
        _xposedStatus.value =
            runCatching {
                XposedStatus.Connected(
                    apiVersion = service.apiVersion,
                    scopes = service.scope.toSet(),
                    runningTargets = service.runningTargets.associate { it.processName to it.state },
                )
            }.getOrElse { throwable ->
                Log.w(
                    TAG,
                    "Unable to query Xposed runtime status: " + throwable.message,
                )
                XposedStatus.QueryUnavailable
            }
    }

    // App prefs are authoritative; replay on bind, retry after errors, skip identical commits.
    private fun syncRuntime(service: XposedService) {
        val snapshot =
            RuntimeSnapshot(
                settingsEntry = settingsPrefs.getInt(SETTINGS_ENTRY_KEY, 0),
                settingsIcon = settingsPrefs.getInt(SETTINGS_ENTRY_ICON_KEY, 0),
                level =
                    diagPrefs.getString(
                        DIAG_LEVEL_KEY,
                        DiagLevel.General.name,
                    ) ?: DiagLevel.General.name,
                feature =
                    FeatureCfg(
                        enabled = featurePrefs.getBoolean(FEATURE_ENABLED_KEY, true),
                        keyguard =
                            featurePrefs.getBoolean(FEATURE_KEYGUARD_KEY, FEATURE_KEYGUARD_DEFAULT),
                        aod = featurePrefs.getBoolean(FEATURE_AOD_KEY, FEATURE_AOD_DEFAULT),
                    ),
                changedAtNs = featurePrefs.getLong(FEATURE_CHANGED_AT_NS_KEY, 0L),
                visual = visualPrefs.readVisualCfg(),
            )
        val previous = lastMirror
        if (previous?.first === service && previous.second == snapshot) return

        runCatching {
            val remote = service.getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME)
            val editor = remote.edit() ?: error("remote preference editor unavailable")
            editor
                .putInt(SETTINGS_ENTRY_KEY, snapshot.settingsEntry)
                .putInt(SETTINGS_ENTRY_ICON_KEY, snapshot.settingsIcon)
                .putString(DIAG_LEVEL_KEY, snapshot.level)
                .putBoolean(FEATURE_ENABLED_KEY, snapshot.feature.enabled)
                .putBoolean(FEATURE_KEYGUARD_KEY, snapshot.feature.keyguard)
                .putBoolean(FEATURE_AOD_KEY, snapshot.feature.aod)
                .putLong(FEATURE_CHANGED_AT_NS_KEY, snapshot.changedAtNs)
                .putVisualCfg(snapshot.visual)
            check(editor.commit()) { "remote preference commit failed" }
            if (xposedService === service) lastMirror = service to snapshot
        }.onFailure { throwable ->
            lastMirror = null
            Log.w(
                TAG,
                "Unable to mirror runtime preferences: " + throwable.message,
            )
        }
    }

    private class ScopeRequest(
        val service: XposedService,
        val onComplete: (Boolean) -> Unit,
    )

    private class HotReloadPending(
        val service: XposedService,
        val onComplete: () -> Unit,
    )

    private data class RuntimeSnapshot(
        val settingsEntry: Int,
        val settingsIcon: Int,
        val level: String,
        val feature: FeatureCfg,
        val changedAtNs: Long,
        val visual: VisualCfg,
    )

    private companion object {
        const val TAG = "CombinedStatus[App]"
        const val SYS_UI_PROCESS = "com.android.systemui"
        const val SETTINGS_PROCESS = "com.android.settings"
        const val XPOSED_BIND_TIMEOUT_MS = 1_000L
    }
}
