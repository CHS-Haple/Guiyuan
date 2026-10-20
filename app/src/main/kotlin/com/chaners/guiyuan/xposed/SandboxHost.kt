package com.chaners.guiyuan.xposed

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.chaners.guiyuan.sandbox.SANDBOX_DATA_KEY
import com.chaners.guiyuan.sandbox.SANDBOX_LEASE_MS
import com.chaners.guiyuan.sandbox.SandboxLease
import com.chaners.guiyuan.ui.screens.PreviewSysUiResources
import com.chaners.guiyuan.ui.screens.toRenderModel
import com.chaners.guiyuan.ui.screens.previewCenterSource

// Temporary Home presentation only. Native state continues updating underneath.
internal object SandboxHost {
    private val handler = Handler(Looper.getMainLooper())
    private var source: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var onChanged: (() -> Unit)? = null
    @Volatile private var current: SandboxLease? = null
    private var resources: PreviewSysUiResources? = null
    private val expiry = Runnable { refresh() }

    fun bind(prefs: SharedPreferences, changed: () -> Unit) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            handler.post { bind(prefs, changed) }
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
        val wasActive = current != null
        current = null
        if (wasActive) onChanged?.invoke()
        source?.let { prefs ->
            listener?.let(prefs::unregisterOnSharedPreferenceChangeListener)
        }
        source = null
        listener = null
        onChanged = null
        resources = null
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
            if (lease.state.dataMobileNetwork ==
                com.chaners.guiyuan.ui.screens.PreviewMobileNetwork.FIVE_GA &&
                lease.state.previewCenterSource() ==
                com.chaners.guiyuan.ui.screens.PreviewCenterSource.MOBILE &&
                verifiedFace == null
            ) return@runCatching null
            lease.state.toRenderModel(resolver, mobileTypeface = verifiedFace)
        }.getOrNull()
    }

    private fun refresh() {
        handler.removeCallbacks(expiry)
        val next =
            source
                ?.let { prefs ->
                    runCatching { SandboxLease.decode(prefs.getString(SANDBOX_DATA_KEY, null)) }
                        .getOrNull()
                }
                ?.takeIf { it.valid(SystemClock.elapsedRealtime(), System.currentTimeMillis()) }
        val previous = current
        current = next
        if (next != null) {
            val remaining = (next.elapsedAt + SANDBOX_LEASE_MS - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            handler.postDelayed(expiry, remaining + 1L)
        }
        if ((previous?.state != next?.state) || (previous == null) != (next == null)) {
            onChanged?.invoke()
        }
    }
}
