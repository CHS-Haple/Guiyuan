package com.chaners.guiyuan.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.util.WeakHashMap

// Owns controller observation and the one pending activation; native geometry stays elsewhere.
internal object NativeParticipantRuntime {
    private const val CONTROLLER_IMPL =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val ADD_ICON_GROUP = "addIconGroup"
    private const val CONTROLLER_HOOK_ID =
        "combinedstatus.nativeParticipant.controllerObserver"

    private var pending: PendingActivation? = null
    private var controllerHook: HookHandle? = null
    private val controllersByManager =
        WeakHashMap<Any, WeakReference<Any>>()

    val installedHookCount: Int
        @Synchronized get() = if (controllerHook != null) 1 else 0

    @Synchronized
    fun installControllerObserver(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
    ): String? {
        if (controllerHook != null) {
            return null
        }

        val controllerClass =
            runCatching {
                Class.forName(CONTROLLER_IMPL, false, classLoader)
            }.getOrElse {
                return "controller-class-missing"
            }

        val method =
            controllerClass.declaredMethods
                .filter { candidate ->
                    candidate.name == ADD_ICON_GROUP &&
                        candidate.parameterCount == 1 &&
                        candidate.parameterTypes[0].name.contains("IconManager")
                }
                .sortedBy { candidate ->
                    candidate.parameterTypes[0].name
                }
                .firstOrNull()
                ?: return "add-icon-group-method-missing"

        method.isAccessible = true
        val handle =
            runCatching {
                module
                    .hook(method)
                    .setId(CONTROLLER_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val manager = chain.getArg(0)
                            val result = chain.proceed()
                            val controller = chain.thisObject
                            if (manager != null && controller != null) {
                                recordController(
                                    manager = manager,
                                    controller = controller,
                                )
                                notifyControllerObserved(manager)
                                onEvent?.invoke(
                                    "nativeParticipantController observed " +
                                        "controller=" + controller.javaClass.name +
                                        " manager=" + manager.javaClass.name +
                                        " source=addIconGroup",
                                )
                            }
                            result
                        },
                    )
            }.getOrElse { error ->
                return "controller-observer-hook-" +
                        (error.message ?: error.javaClass.simpleName)
            }

        controllerHook = handle
        return null
    }

    @Synchronized
    fun controllerFor(manager: Any): Any? =
        controllersByManager[manager]?.get()

    @Synchronized
    fun restoreExistingController(host: Any): Boolean {
        val manager =
            NativeParticipantRuntimeAccess.managerFor(host)
                ?: return false
        val handles =
            when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return false
            }
        if (handles.manager !== manager) {
            return false
        }
        controllersByManager[manager] = WeakReference(handles.controller)
        return true
    }

    @Synchronized
    private fun recordController(
        manager: Any,
        controller: Any,
    ) {
        controllersByManager[manager] = WeakReference(controller)
    }

    @Synchronized
    fun resetControllerRuntimeState() {
        controllersByManager.clear()
        controllerHook = null
    }

    @Synchronized
    fun schedule(
        host: Any,
        onReady: (Any) -> Unit,
        onFailure: (String) -> Unit,
    ): String? {
        cancelPendingLocked()

        val hostView =
            host as? View
                ?: return "host-not-view"

        val activation =
            PendingActivation(
                hostView = hostView,
                onReady = onReady,
                onFailure = onFailure,
            )
        pending = activation

        return if (activation.start()) {
            null
        } else {
            pending = null
            "native-controller-readiness-rejected"
        }
    }

    @Synchronized
    fun cancelPending(): Boolean = cancelPendingLocked()

    @Synchronized
    private fun complete(activation: PendingActivation): Boolean {
        if (pending !== activation) {
            return false
        }
        pending = null
        return true
    }

    @Synchronized
    private fun notifyControllerObserved(manager: Any) {
        pending?.onControllerObserved(manager)
    }

    private fun cancelPendingLocked(): Boolean {
        val activation = pending ?: return false
        pending = null
        activation.cancel()
        return true
    }

    private class PendingActivation(
        private val hostView: View,
        private val onReady: (Any) -> Unit,
        private val onFailure: (String) -> Unit,
    ) : View.OnAttachStateChangeListener, Runnable {
        private var listeningForAttach = false
        private var readyPosted = false

        fun start(): Boolean {
            return if (hostView.isAttachedToWindow) {
                armForController()
            } else {
                hostView.addOnAttachStateChangeListener(this)
                listeningForAttach = true
                true
            }
        }

        fun cancel() {
            if (listeningForAttach) {
                hostView.removeOnAttachStateChangeListener(this)
                listeningForAttach = false
            }
            if (readyPosted) {
                hostView.removeCallbacks(this)
                readyPosted = false
            }
        }

        override fun onViewAttachedToWindow(view: View) {
            if (listeningForAttach) {
                view.removeOnAttachStateChangeListener(this)
                listeningForAttach = false
            }
            if (!armForController() && complete(this)) {
                onFailure("native-controller-readiness-rejected")
            }
        }

        override fun onViewDetachedFromWindow(view: View) = Unit

        fun onControllerObserved(manager: Any) {
            if (readyPosted) {
                return
            }
            val targetManager =
                NativeParticipantRuntimeAccess.managerFor(hostView)
                    ?: return
            if (targetManager !== manager) {
                return
            }
            postReady()
        }

        private fun armForController(): Boolean {
            val manager =
                NativeParticipantRuntimeAccess.managerFor(hostView)
            if (
                manager != null &&
                controllerFor(manager) != null
            ) {
                return postReady()
            }
            return true
        }

        private fun postReady(): Boolean {
            if (readyPosted) {
                return true
            }
            readyPosted = hostView.post(this)
            return readyPosted
        }

        override fun run() {
            readyPosted = false
            if (!complete(this)) {
                return
            }
            onReady(hostView)
        }
    }

}
