package com.chaners.guiyuan.sandbox

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

    @Volatile private var id = 0L
    @Volatile private var latest: PreviewSandboxUiState? = null
    private var heartbeat: Job? = null

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
                                if (update.id != id) return@runCatching false
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

                if (update is Update.Show && update.id == id) {
                    _active.value = written
                    if (!written) stop()
                }
            }
        }
    }

    fun start(state: PreviewSandboxUiState) {
        if (id != 0L) {
            update(state)
            return
        }
        val session = (SystemClock.elapsedRealtimeNanos() and Long.MAX_VALUE).coerceAtLeast(1L)
        latest = state
        id = session
        updates.trySend(Update.Show(session, state))
        heartbeat?.cancel()
        heartbeat = scope.launch {
            while (isActive && id == session) {
                delay(SANDBOX_RENEW_MS)
                if (id != session) break
                latest?.let { updates.trySend(Update.Show(session, it)) }
            }
        }
    }

    fun update(state: PreviewSandboxUiState) {
        latest = state
        val session = id
        if (session != 0L) updates.trySend(Update.Show(session, state))
    }

    fun stop() {
        if (id == 0L && !_active.value) return
        id = 0L
        latest = null
        heartbeat?.cancel()
        heartbeat = null
        _active.value = false
        updates.trySend(Update.Clear)
    }
}
