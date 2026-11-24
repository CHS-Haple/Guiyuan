package com.chaners.guiyuan.xposed.network

import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

// Read-only native island visibility evidence, reset with the observation generation.
internal object NativeIslandEvidence {
    // The island peer mirror samples native icon state from onLayout. These
    // reflective contracts are stable for the SystemUI process lifetime, so
    // cache discovery by concrete class and keep the per-layout path to invoke
    // + direct Field.get only.
    private val transitionStateAccessorCache = ConcurrentHashMap<Class<*>, Method>()
    private val transitionStateAccessorMissing = ConcurrentHashMap.newKeySet<Class<*>>()
    private val transitionStateFieldCache =
        ConcurrentHashMap<Class<*>, TransitionStateFields>()

    fun resetCaches() {
        transitionStateAccessorCache.clear()
        transitionStateAccessorMissing.clear()
        transitionStateFieldCache.clear()
    }

    fun read(
        group: ViewGroup,
        view: View,
    ): IslandVisibilityState? {
        val state = transitionStateObject(group, view) ?: return null
        val fields = transitionStateFields(state.javaClass)
        return IslandVisibilityState(
            visibleState = (readCachedField(fields.visibleState, state) as? Number)?.toInt(),
            inIslandState = (readCachedField(fields.inIslandState, state) as? Number)?.toInt(),
            hiddenBySpace = readCachedField(fields.hiddenBySpace, state) as? Boolean,
        )
    }

    private fun transitionStateObject(
        group: ViewGroup,
        view: View,
    ): Any? {
        val groupClass = group.javaClass
        val accessor =
            transitionStateAccessorCache[groupClass]
                ?: run {
                    if (groupClass in transitionStateAccessorMissing) {
                        return null
                    }
                    val companionClass =
                        optionalNativeRead {
                            Class.forName(
                                groupClass.name + "\$Companion",
                                false,
                                groupClass.classLoader,
                            )
                        }
                    val resolved =
                        optionalNativeRead {
                            companionClass
                                ?.declaredMethods
                                ?.firstOrNull { method ->
                                    method.name == "access\$getViewStateFromChild" &&
                                        method.parameterTypes.contentEquals(arrayOf(View::class.java))
                                }?.apply { isAccessible = true }
                        }
                    if (resolved == null) {
                        transitionStateAccessorMissing += groupClass
                        return null
                    }
                    transitionStateAccessorCache.putIfAbsent(groupClass, resolved) ?: resolved
                }
        return optionalNativeRead {
            accessor.invoke(null, view)
        }
    }

    private fun transitionStateFields(stateClass: Class<*>): TransitionStateFields =
        transitionStateFieldCache[stateClass]
            ?: TransitionStateFields(
                visibleState = resolveCachedField(stateClass, "visibleState"),
                inIslandState = resolveCachedField(stateClass, "inIslandState"),
                hiddenBySpace = resolveCachedField(stateClass, "hiddenBySpace"),
            ).let { resolved ->
                transitionStateFieldCache.putIfAbsent(stateClass, resolved) ?: resolved
            }

    private fun resolveCachedField(
        clazz: Class<*>,
        name: String,
    ): Field? =
        optionalNativeRead {
            generateSequence(clazz) { candidate -> candidate.superclass }
                .mapNotNull { candidate ->
                    candidate.declaredFields.firstOrNull { field -> field.name == name }
                }
                .firstOrNull()
                ?.apply { isAccessible = true }
        }

    private fun readCachedField(
        field: Field?,
        target: Any,
    ): Any? =
        field?.let { resolved ->
            optionalNativeRead { resolved.get(target) }
        }

    data class IslandVisibilityState(
        val visibleState: Int?,
        val inIslandState: Int?,
        val hiddenBySpace: Boolean?,
    )

    private data class TransitionStateFields(
        val visibleState: Field?,
        val inIslandState: Field?,
        val hiddenBySpace: Field?,
    )
}
