package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import io.github.libxposed.api.XposedModule

internal object PresentationRuntime {
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
    ) {
        val tintReady: Boolean
            get() = tintHooks == TintSource.HOOK_COUNT
        val sceneReady: Boolean
            get() = sceneHooks == SceneSource.HOOK_COUNT
        val mobileTypeReady: Boolean
            get() = mobileTypeHooks == MobileTypeSource.HOOK_COUNT
        val keyguardAodReady: Boolean
            get() = keyguardAodHooks == KeyguardAodSource.HOOK_COUNT
        val keyguardFullAodReady: Boolean
            get() =
                keyguardFullAodHooks ==
                    KeyguardAodTransition.HOOK_COUNT
        val keyguardStatusIconReady: Boolean
            get() =
                keyguardStatusIconHooks ==
                    KeyguardIconTransition.HOOK_COUNT
    }

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (TintSource.TintUpdate) -> Unit,
        onSceneState: (SceneSource.SceneUpdate) -> Unit,
        onKeyguardAodState: (KeyguardAodSource.AodUpdate) -> Unit,
        onAodTransitionStart: () -> Unit,
        onAodTransitionCommit: () -> Unit,
        onKeyguardIconTransition: () -> Unit,
        onMobileTypeChanged: (Drawable) -> Unit,
        onTintEvent: ((String) -> Unit)?,
        onSceneEvent: ((String) -> Unit)?,
        onKeyguardAodEvent: ((String) -> Unit)?,
    ): AttachResult {
        val tintHooks =
            TintSource.install(
                module = module,
                classLoader = classLoader,
                onTintState = onTintState,
                onEvent = onTintEvent,
            ).size
        val keyguardAodHooks =
            runCatching {
                KeyguardAodSource.install(
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
                0
            }
        val keyguardFullAodHooks =
            runCatching {
                KeyguardAodTransition.install(
                    module = module,
                    classLoader = classLoader,
                    onTransitionStarted = onAodTransitionStart,
                    onTransitionCommitted = onAodTransitionCommit,
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
                KeyguardIconTransition.install(
                    module = module,
                    classLoader = classLoader,
                    onTransition = onKeyguardIconTransition,
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
        val sceneHooks =
            SceneSource.install(
                module = module,
                classLoader = classLoader,
                onSceneState = onSceneState,
                onEvent = onSceneEvent,
            ).size
        val mobileTypeHooks =
            MobileTypeSource.install(
                module = module,
                classLoader = classLoader,
                onChanged = onMobileTypeChanged,
            ).size

        return AttachResult(
            tintHooks = tintHooks,
            sceneHooks = sceneHooks,
            mobileTypeHooks = mobileTypeHooks,
            keyguardAodHooks = keyguardAodHooks,
            keyguardFullAodHooks = keyguardFullAodHooks,
            keyguardStatusIconHooks = keyguardStatusIconHooks,
        ).also { current = it }
    }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        TintSource.resetRuntimeState()
        SceneSource.resetRuntimeState()
        KeyguardAodSource.resetRuntimeState()
        KeyguardHostProbe.resetRuntimeState()
    }
}
