package com.chaners.guiyuan.sandbox

import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import android.os.SystemClock
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class SandboxClient(
    private val service: () -> XposedService?,
) {
    private sealed interface Update {
        data class Show(val id: Long, val state: PreviewSandboxUiState) : Update
        data object Clear : Update
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = Channel<Update>(Channel.CONFLATED)
    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    // Keep cancellation and renewal ordered; an interrupted write still expires in SystemUI.
    private var sessionId = 0L
    private var latestState: PreviewSandboxUiState? = null
    private var renewalJob: Job? = null

    init {
        scope.launch {
            for (update in updates) {
                val written =
                    runCatching {
                        val remote = service()?.takeIf {
                            it.apiVersion >= 102 && "com.android.systemui" in it.scope
                        }?.getRemotePreferences(SANDBOX_PREFS)
                        val editor = remote?.edit() ?: return@runCatching false
                        when (update) {
                            is Update.Show -> {
                                if (synchronized(this@SandboxClient) { update.id != sessionId }) {
                                    return@runCatching false
                                }
                                editor.putString(
                                    SANDBOX_DATA_KEY,
                                    SandboxLease(
                                        id = update.id,
                                        elapsedAt = SystemClock.elapsedRealtime(),
                                        wallAt = System.currentTimeMillis(),
                                        state = update.state,
                                    ).encode(),
                                )
                            }
                            Update.Clear -> editor.remove(SANDBOX_DATA_KEY)
                        }
                        editor.commit()
                    }.getOrDefault(false)

                if (update is Update.Show) {
                    synchronized(this@SandboxClient) {
                        if (update.id == sessionId) {
                            _active.value = written
                            if (!written) stop()
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun start(state: PreviewSandboxUiState) {
        if (sessionId != 0L) {
            update(state)
            return
        }
        val session = (SystemClock.elapsedRealtimeNanos() and Long.MAX_VALUE).coerceAtLeast(1L)
        latestState = state
        sessionId = session
        updates.trySend(Update.Show(session, state))
        renewalJob?.cancel()
        renewalJob = scope.launch {
            while (isActive) {
                delay(SANDBOX_RENEW_MS)
                val current = synchronized(this@SandboxClient) {
                    if (sessionId == session) {
                        latestState?.also { updates.trySend(Update.Show(session, it)) }
                    } else {
                        null
                    }
                }
                if (current == null) break
            }
        }
    }

    @Synchronized
    fun update(state: PreviewSandboxUiState) {
        if (sessionId == 0L || latestState == state) return
        latestState = state
        updates.trySend(Update.Show(sessionId, state))
    }

    @Synchronized
    fun stop() {
        if (sessionId == 0L && !_active.value) return
        sessionId = 0L
        latestState = null
        renewalJob?.cancel()
        renewalJob = null
        _active.value = false
        updates.trySend(Update.Clear)
    }
}
