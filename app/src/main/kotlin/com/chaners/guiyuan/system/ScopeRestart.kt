package com.chaners.guiyuan.system

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class RestartTarget(val pkg: String) {
    SYS_UI("com.android.systemui"),
    SETTINGS("com.android.settings"),
}

// An accepted restart must outlive the screen that initiated it.
internal object ScopeRestart {
    sealed interface State {
        data object Idle : State
        data object Running : State
        data class Failed(val targets: Set<RestartTarget>) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val current = MutableStateFlow<State>(State.Idle)
    val state = current.asStateFlow()
    val isRunning: Boolean get() = current.value is State.Running

    fun start(
        context: Context,
        targets: Set<RestartTarget>,
        onComplete: () -> Unit,
    ): Boolean {
        if (targets.isEmpty() || isRunning) return false

        val appContext = context.applicationContext
        val chosen = targets.toSet()
        current.value = State.Running
        scope.launch {
            try {
                val failed = mutableSetOf<RestartTarget>()
                for (target in RestartTarget.entries) {
                    if (target !in chosen) continue
                    val ok = try {
                        when (target) {
                            RestartTarget.SYS_UI -> SysUiScope.restart(appContext)
                            RestartTarget.SETTINGS -> SettingsScope.restart(appContext)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("Guiyuan[Restart]", "Unable to restart ${target.pkg}", error)
                        false
                    }
                    if (!ok) failed += target
                }
                current.value =
                    if (failed.isEmpty()) State.Idle else State.Failed(failed.toSet())
            } catch (cancelled: CancellationException) {
                current.value = State.Idle
                throw cancelled
            } finally {
                onComplete()
            }
        }
        return true
    }

    fun clearFailure() {
        if (current.value is State.Failed) current.value = State.Idle
    }
}
