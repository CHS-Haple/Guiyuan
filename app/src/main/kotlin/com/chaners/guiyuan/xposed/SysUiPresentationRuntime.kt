package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import android.util.Log
import com.chaners.guiyuan.xposed.network.SysUiMobileTypeSource
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

internal object SysUiPresentationRuntime {
    private var current: AttachResult? = null

    internal fun throwIfFatal(error: Throwable) = SysUiFatal.rethrow(error)

    // Passive evidence must never become a SystemUI callback failure.
    @Suppress("TooGenericExceptionCaught")
    internal inline fun emitPassiveDiagnostic(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            throwIfFatal(error)
        }
    }

    // Each installation owns its warning receipt, not a second diagnostic state machine.
    @Suppress("TooGenericExceptionCaught")
    internal inline fun emitPassiveDiagnostic(
        warned: AtomicBoolean,
        tag: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (error: Throwable) {
            throwIfFatal(error)
            if (warned.compareAndSet(false, true)) {
                emitPassiveDiagnostic { Log.w(tag, "Diagnostic failed", error) }
            }
        }
    }

    // Optional platform reads may be missing, but VM-fatal errors are not absence.
    @Suppress("TooGenericExceptionCaught")
    internal inline fun <T> optionalNativeRead(block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            throwIfFatal(error)
            null
        }

    // Hook installation may fall back to native only for nonfatal failures.
    // Centralize the fatal gate; each owner retains its own fallback receipt.
    internal inline fun <T> attemptHookInstall(block: () -> T): Result<T> =
        runCatching(block).onFailure(::throwIfFatal)

    // Preserve exact native signature selection; never guess among overloads.
    internal inline fun requireUniqueHookMethod(
        type: Class<*>,
        name: String,
        contract: String,
        matches: (Method) -> Boolean,
    ): Method {
        val candidates = type.declaredMethods.filter { method ->
            method.name == name && matches(method)
        }
        return candidates.singleOrNull()
            ?.apply { isAccessible = true }
            ?: error(contract + if (candidates.isEmpty()) "missing" else "ambiguous")
    }

    val installedHookCount: Int
        @Synchronized get() =
            current?.let {
                it.tintHooks +
                    it.sceneHooks +
                    it.mobileTypeHooks +
                    it.keyguardAodHooks +
                    it.keyguardFullAodHooks +
                    it.keyguardStatusIconHooks
            } ?: SysUiTintSource.failedInstallHookCount

    val keyguardAodReady: Boolean
        @Synchronized get() = current?.keyguardAodReady == true

    val keyguardFullAodReady: Boolean
        @Synchronized get() = current?.keyguardFullAodReady == true

    val keyguardStatusIconReady: Boolean
        @Synchronized get() = current?.keyguardStatusIconReady == true

    internal data class AttachResult(
        val tintHooks: Int,
        val sceneHooks: Int,
        val mobileTypeHooks: Int,
        val keyguardAodHooks: Int,
        val keyguardFullAodHooks: Int,
        val keyguardStatusIconHooks: Int,
        val tintFailure: String?,
        val sceneFailure: String?,
        val mobileTypeFailure: String?,
    ) {
        val tintReady: Boolean
            get() = tintHooks == SysUiTintSource.HOOK_COUNT
        val sceneReady: Boolean
            get() = sceneHooks == SysUiSceneSource.HOOK_COUNT
        val mobileTypeReady: Boolean
            get() = mobileTypeHooks == SysUiMobileTypeSource.HOOK_COUNT
        val keyguardAodReady: Boolean
            get() = keyguardAodHooks == SysUiKeyguardAodSource.HOOK_COUNT
        val keyguardFullAodReady: Boolean
            get() =
                keyguardFullAodHooks ==
                    FullAodTransitionSource.HOOK_COUNT
        val keyguardStatusIconReady: Boolean
            get() =
                keyguardStatusIconHooks ==
                    KeyguardIconTransitionSource.HOOK_COUNT
    }

    // Callbacks belong to the installing generation; these are immutable routing inputs.
    internal class KeyguardSinks(
        val onAodState: (SysUiKeyguardAodSource.AodUpdate) -> Unit,
        val onRuntimeFailure: (String, Throwable) -> Unit,
        val onFullAodStarted: () -> Unit,
        val onFullAodCommitted: () -> Unit,
        val onIconTransition: () -> Unit,
    )

    internal class Sinks(
        val onTintState: (SysUiTintSource.TintUpdate) -> Unit,
        val onSceneState: (SysUiSceneSource.SceneUpdate) -> Unit,
        val onMobileTypeChanged: (Drawable) -> Unit,
        val onRuntimeFailure: (String, Throwable) -> Unit,
        val keyguard: KeyguardSinks,
    )

    private class KeyguardSourceHooks(
        val aod: Int,
        val fullAod: Int,
        val statusIcon: Int,
    )

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        sinks: Sinks,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): AttachResult {
        var tintFailure: String? = null
        val tintHooks =
            attemptHookInstall {
                SysUiTintSource.install(
                    module = module,
                    classLoader = classLoader,
                    onTintState = sinks.onTintState,
                    onRuntimeFailure = { error -> sinks.onRuntimeFailure("tint", error) },
                    onEvent = onEvent,
                    isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                ).size
            }.getOrElse { error ->
                tintFailure = error.message ?: error.javaClass.simpleName
                SysUiTintSource.failedInstallHookCount
            }
        val keyguard = attachKeyguardSources(module, classLoader, sinks, onEvent, isDetailedDiagnosticsEnabled)
        var sceneFailure: String? = null
        val sceneHooks =
            attemptHookInstall {
                SysUiSceneSource.install(
                    module = module,
                    classLoader = classLoader,
                    onSceneState = sinks.onSceneState,
                    onRuntimeFailure = { error -> sinks.onRuntimeFailure("scene", error) },
                    onEvent = onEvent,
                    isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                ).size
            }.getOrElse { error ->
                sceneFailure = error.message ?: error.javaClass.simpleName
                0
            }
        var mobileTypeFailure: String? = null
        val mobileTypeHooks =
            attemptHookInstall {
                SysUiMobileTypeSource.install(
                    module = module,
                    classLoader = classLoader,
                    onChanged = sinks.onMobileTypeChanged,
                ).size
            }.getOrElse { error ->
                mobileTypeFailure = error.message ?: error.javaClass.simpleName
                0
            }

        return AttachResult(
            tintHooks = tintHooks,
            sceneHooks = sceneHooks,
            mobileTypeHooks = mobileTypeHooks,
            keyguardAodHooks = keyguard.aod,
            keyguardFullAodHooks = keyguard.fullAod,
            keyguardStatusIconHooks = keyguard.statusIcon,
            tintFailure = tintFailure,
            sceneFailure = sceneFailure,
            mobileTypeFailure = mobileTypeFailure,
        ).also { current = it }
    }

    /** Transient exact-Hook inputs owned by this single installation generation. */
    private class KeyguardHookInstallRequest(
        val module: XposedModule,
        val classLoader: ClassLoader,
        val sinks: Sinks,
        val onEvent: ((String) -> Unit)?,
        val isDetailedDiagnosticsEnabled: () -> Boolean,
    )

    private fun installKeyguardAod(
        request: KeyguardHookInstallRequest,
        reportAodFailure: (String) -> Unit,
    ): Int =
        attemptHookInstall {
            SysUiKeyguardAodSource.install(
                module = request.module,
                classLoader = request.classLoader,
                onAodState = request.sinks.keyguard.onAodState,
                onRuntimeFailure = { error ->
                    request.sinks.keyguard.onRuntimeFailure("aod-state", error)
                },
                onEvent = request.onEvent,
                isDetailedDiagnosticsEnabled = request.isDetailedDiagnosticsEnabled,
            ).size
        }.getOrElse { error ->
            reportAodFailure(
                "keyguardAod install=unavailable reason=" +
                    (error.message ?: error.javaClass.simpleName) +
                    " fallback=native-keyguard",
            )
            SysUiKeyguardAodSource.failedInstallHookCount
        }

    private fun installFullAodTransition(
        request: KeyguardHookInstallRequest,
        reportAodFailure: (String) -> Unit,
    ): Int =
        attemptHookInstall {
            FullAodTransitionSource.install(
                module = request.module,
                classLoader = request.classLoader,
                onTransitionStarted = request.sinks.keyguard.onFullAodStarted,
                onTransitionCommitted = request.sinks.keyguard.onFullAodCommitted,
                onRuntimeFailure = request.sinks.onRuntimeFailure,
                onEvent = request.onEvent,
                isDetailedDiagnosticsEnabled = request.isDetailedDiagnosticsEnabled,
            ).size
        }.getOrElse { error ->
            reportAodFailure(
                "keyguardFullAod install=unavailable reason=" +
                    (error.message ?: error.javaClass.simpleName) +
                    " fallback=status-icons-alpha",
            )
            0
        }

    private fun installKeyguardIconTransition(
        request: KeyguardHookInstallRequest,
        reportAodFailure: (String) -> Unit,
    ): Int =
        attemptHookInstall {
            KeyguardIconTransitionSource.install(
                module = request.module,
                classLoader = request.classLoader,
                onTransition = request.sinks.keyguard.onIconTransition,
                onRuntimeFailure = { error ->
                    request.sinks.keyguard.onRuntimeFailure("icon-transition", error)
                },
                onEvent = request.onEvent,
                isDetailedDiagnosticsEnabled = request.isDetailedDiagnosticsEnabled,
            ).size
        }.getOrElse { error ->
            reportAodFailure(
                "keyguardStatusIconTransition install=unavailable reason=" +
                    (error.message ?: error.javaClass.simpleName) +
                    " fallback=status-icons-alpha",
            )
            0
        }

    private fun attachKeyguardSources(
        module: XposedModule,
        classLoader: ClassLoader,
        sinks: Sinks,
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
    ): KeyguardSourceHooks {
        val warned = AtomicBoolean(false)
        @Suppress("TooGenericExceptionCaught")
        fun reportAodFailure(message: String) {
            try {
                onEvent?.invoke(message)
            } catch (error: Throwable) {
                throwIfFatal(error)
                if (warned.compareAndSet(false, true)) {
                    emitPassiveDiagnostic {
                        Log.w("Guiyuan[Aod]", "Install diagnostic failed", error)
                    }
                }
            }
        }
        val request = KeyguardHookInstallRequest(
            module, classLoader, sinks, onEvent, isDetailedDiagnosticsEnabled,
        )
        val keyguardAodHooks = installKeyguardAod(request, ::reportAodFailure)
        val keyguardFullAodHooks = installFullAodTransition(request, ::reportAodFailure)
        val keyguardStatusIconHooks = installKeyguardIconTransition(request, ::reportAodFailure)
        return KeyguardSourceHooks(keyguardAodHooks, keyguardFullAodHooks, keyguardStatusIconHooks)
    }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SysUiTintSource.resetRuntimeState()
        SysUiSceneSource.resetRuntimeState()
        SysUiKeyguardAodSource.resetRuntimeState()
        SysUiKeyguardHostProbe.resetRuntimeState()
    }
}
