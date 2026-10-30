package com.chaners.guiyuan.xposed

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.chaners.guiyuan.sandbox.SANDBOX_DATA_KEY
import com.chaners.guiyuan.sandbox.SANDBOX_LEASE_MS
import com.chaners.guiyuan.sandbox.SandboxLease
import com.chaners.guiyuan.sandbox.rethrowSandboxFatal
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewSysUiResources
import com.chaners.guiyuan.ui.screens.toRenderModel
import com.chaners.guiyuan.xposed.prefs.FeaturePrefsOwner
import com.chaners.guiyuan.xposed.prefs.PrefListenerRetirement
import com.chaners.guiyuan.ui.screens.previewCenterSource

// Temporary Home presentation only. Native state continues updating underneath.
internal object SandboxHost {
    private val handler = Handler(Looper.getMainLooper())
    private var source: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val listenerRetirement = PrefListenerRetirement()
    private var onChanged: ((PreviewSandboxUiState?) -> Unit)? = null
    @Volatile private var current: SandboxLease? = null
    private var resources: PreviewSysUiResources? = null
    private var staticMobileFaceChecked = false
    private var staticMobileFace: android.graphics.Typeface? = null
    private val expiry = Runnable { refresh() }

    fun bind(
        prefs: SharedPreferences,
        canBind: () -> Boolean,
        changed: (PreviewSandboxUiState?) -> Unit,
    ) {
        // A bind posted before Hot Reload may arrive after the old owner was retired.
        if (!canBind()) return
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            handler.post { bind(prefs, canBind, changed) }
            return
        }
        unbind()
        source = prefs
        onChanged = changed
        val observer =
            SharedPreferences.OnSharedPreferenceChangeListener { changedPrefs, key ->
                if (key == SANDBOX_DATA_KEY && changedPrefs === source) {
                    handler.post {
                        if (changedPrefs === source) refresh()
                    }
                }
            }
        listener = observer
        prefs.registerOnSharedPreferenceChangeListener(observer)
        refresh()
    }

    fun unbind() {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            handler.post { unbind() }
            return
        }
        handler.removeCallbacks(expiry)
        val notify = if (current != null) onChanged else null
        current = null
        val prefs = source
        val observer = listener
        // Retire state before entering the framework listener and notifying renderers.
        source = null
        listener = null
        onChanged = null
        resources = null
        staticMobileFace = null
        staticMobileFaceChecked = false
        try {
            listenerRetirement.unregister(prefs, observer)
        } finally {
            notify?.invoke(null)
        }
    }

    fun active(): Boolean = validLease() != null

    private fun validLease(): SandboxLease? {
        val lease = current ?: return null
        if (!lease.valid(SystemClock.elapsedRealtime(), System.currentTimeMillis())) {
            handler.post { refresh() }
            return null
        }
        return lease
    }

    fun model(context: Context): RenderModel? {
        val lease = validLease() ?: return null
        return runCatching {
            val resolver = resources ?: PreviewSysUiResources(context.applicationContext).also {
                resources = it
            }
            val native = PresentationStore.snapshot().mobilePresentation?.networkType
            val verifiedFace = native?.takeIf { it.fontPath != null }?.typeface
                ?: staticMobileFace(context)
            if (lease.state.dataMobileNetwork ==
                com.chaners.guiyuan.ui.screens.PreviewMobileNetwork.FIVE_GA &&
                lease.state.previewCenterSource() ==
                com.chaners.guiyuan.ui.screens.PreviewCenterSource.MOBILE &&
                verifiedFace == null
            ) return@runCatching null
            lease.state.toRenderModel(
                resolver,
                mobileTypeface = verifiedFace,
                lowBatteryThreshold = FeaturePrefsOwner.current().lowBatteryThreshold,
                nativeLowColor = StatusStateStore.snapshot().battery?.lowSystemColor,
            )
        }.getOrElse { error ->
            rethrowSandboxFatal(error)
            null
        }
    }

    private fun staticMobileFace(context: Context): android.graphics.Typeface? {
        if (!staticMobileFaceChecked) {
            staticMobileFaceChecked = true
            staticMobileFace = NativePresentationResolver.sandboxMobileTypeface(context)
        }
        return staticMobileFace
    }

    private fun refresh() {
        handler.removeCallbacks(expiry)
        val next =
            source
                ?.let { prefs ->
                    runCatching { SandboxLease.decode(prefs.getString(SANDBOX_DATA_KEY, null)) }
                        .getOrElse { error ->
                            rethrowSandboxFatal(error)
                            null
                        }
                }
                ?.takeIf { it.valid(SystemClock.elapsedRealtime(), System.currentTimeMillis()) }
        val previous = current
        current = next
        if (next != null) {
            val remaining = (next.elapsedAt + SANDBOX_LEASE_MS - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            handler.postDelayed(expiry, remaining + 1L)
        }
        if ((previous?.state != next?.state) || (previous == null) != (next == null)) {
            onChanged?.invoke(next?.state)
        }
    }
}
