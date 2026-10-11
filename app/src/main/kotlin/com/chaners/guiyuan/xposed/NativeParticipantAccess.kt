package com.chaners.guiyuan.xposed

import android.view.View
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal object NativeParticipantAccess {
    // Layout callbacks query slot names repeatedly; resolve each concrete getter once.
    private val slotAccessorCache = ConcurrentHashMap<Class<*>, Method>()
    private val slotAccessorMissing = ConcurrentHashMap.newKeySet<Class<*>>()

    fun slotOf(view: View): String? {
        val viewClass = view.javaClass
        val getSlot =
            slotAccessorCache[viewClass]
                ?: run {
                    if (viewClass in slotAccessorMissing) {
                        return null
                    }
                    val resolved =
                        viewClass
                            .allMethods()
                            .firstOrNull { method ->
                                method.name == "getSlot" &&
                                    method.parameterCount == 0 &&
                                    method.returnType == String::class.java
                            }
                    if (resolved == null) {
                        slotAccessorMissing += viewClass
                        return null
                    }
                    resolved.isAccessible = true
                    slotAccessorCache.putIfAbsent(viewClass, resolved) ?: resolved
                }

        return runCatching {
            getSlot.invoke(view) as? String
        }.getOrNull()
    }

    fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    private fun Class<*>.allMethods(): Sequence<Method> =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredMethods.asSequence() }
}
