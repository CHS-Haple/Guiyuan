package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import com.chaners.guiyuan.xposed.network.SysUiMobileTypeSource
import io.github.libxposed.api.XposedModule

internal object SysUiPresentationRuntime {
    private var current: AttachResult? = null

    val installedHookCount: Int
        @Synchronized get() =
            current?.let {
                it.tintHooks +
                    it.sceneHooks +
                    it.mobileTypeHooks +
                    it.keyguardAodHooks +
                    it.keyguardFullAodHooks +
                    it.keyguardStatusIconHooks
            } ?: 0

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

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (SysUiTintSource.TintUpdate) -> Unit,
        onSceneState: (SysUiSceneSource.SceneUpdate) -> Unit,
        onKeyguardAodState: (SysUiKeyguardAodSource.AodUpdate) -> Unit,
        onKeyguardFullAodTransitionStarted: () -> Unit,
        onKeyguardFullAodTransitionCommitted: () -> Unit,
        onKeyguardStatusIconTransition: () -> Unit,
        onMobileTypeChanged: (Drawable) -> Unit,
        onTintEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onSceneEvent: ((String) -> Unit)?,
        onKeyguardAodEvent: ((String) -> Unit)?,
    ): AttachResult {
        val tintHooks =
            SysUiTintSource.install(
                module = module,
                classLoader = classLoader,
                onTintState = onTintState,
                onEvent = onTintEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            ).size
        val keyguardAodHooks =
            runCatching {
                SysUiKeyguardAodSource.install(
                    module = module,
                    classLoader = classLoader,
                    onAodState = onKeyguardAodState,
                    onEvent = onKeyguardAodEvent,
                ).size
            }.getOrElse { error ->
                onKeyguardAodEvent?.invoke(
                    "keyguardAod install=unavailable reason=" +
                        (error.message ?: error.javaClass.simpleName) +
                        " fallback=native-keyguard",
                )
                SysUiKeyguardAodSource.failedInstallHookCount
            }
        val keyguardFullAodHooks =
            runCatching {
                FullAodTransitionSource.install(
                    module = module,
                    classLoader = classLoader,
                    onTransitionStarted = onKeyguardFullAodTransitionStarted,
                    onTransitionCommitted = onKeyguardFullAodTransitionCommitted,
                    onEvent = onKeyguardAodEvent,
                ).size
            }.getOrElse { error ->
                onKeyguardAodEvent?.invoke(
                    "keyguardFullAod install=unavailable reason=" +
                        (error.message ?: error.javaClass.simpleName) +
                        " fallback=status-icons-alpha",
                )
                0
            }
        val keyguardStatusIconHooks =
            runCatching {
                KeyguardIconTransitionSource.install(
                    module = module,
                    classLoader = classLoader,
                    onTransition = onKeyguardStatusIconTransition,
                    onEvent = onKeyguardAodEvent,
                ).size
            }.getOrElse { error ->
                onKeyguardAodEvent?.invoke(
                    "keyguardStatusIconTransition install=unavailable reason=" +
                        (error.message ?: error.javaClass.simpleName) +
                        " fallback=status-icons-alpha",
                )
                0
            }
        var sceneFailure: String? = null
        val sceneHooks =
            runCatching {
                SysUiSceneSource.install(
                    module = module,
                    classLoader = classLoader,
                    onSceneState = onSceneState,
                    onEvent = onSceneEvent,
                ).size
            }.getOrElse { error ->
                sceneFailure = error.message ?: error.javaClass.simpleName
                0
            }
        var mobileTypeFailure: String? = null
        val mobileTypeHooks =
            runCatching {
                SysUiMobileTypeSource.install(
                    module = module,
                    classLoader = classLoader,
                    onChanged = onMobileTypeChanged,
                ).size
            }.getOrElse { error ->
                mobileTypeFailure = error.message ?: error.javaClass.simpleName
                0
            }

        return AttachResult(
            tintHooks = tintHooks,
            sceneHooks = sceneHooks,
            mobileTypeHooks = mobileTypeHooks,
            keyguardAodHooks = keyguardAodHooks,
            keyguardFullAodHooks = keyguardFullAodHooks,
            keyguardStatusIconHooks = keyguardStatusIconHooks,
            sceneFailure = sceneFailure,
            mobileTypeFailure = mobileTypeFailure,
        ).also { current = it }
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
