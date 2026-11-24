package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.SysUiPresentationRuntime.optionalNativeRead
import com.chaners.guiyuan.xposed.SysUiKeyguardAodSource.AodUpdate
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicBoolean

// Stateless native-field evidence and passive diagnostics; no Hook handles or state ownership.
internal object SysUiKeyguardAodEvidence {
    // Optional AOD diagnostics are separate from native transition ownership.
    fun diagnosticCallback(
        onEvent: ((String) -> Unit)?,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        warned: AtomicBoolean,
    ): ((AodUpdate) -> Unit)? =
        onEvent?.let { sink ->
            { update ->
                SysUiPresentationRuntime.emitPassiveDiagnostic(warned, "Guiyuan[Aod]") {
                    if (isDetailedDiagnosticsEnabled()) {
                        sink(
                            "keyguardAod source=" + update.source +
                                " toAod=" + update.toAod +
                                " isAodAnimate=" + update.isAodAnimate +
                                " animToAod=" + (update.animToAod ?: "unavailable") +
                                " blocked=" + update.blocksProjection,
                        )
                    }
                }
            }
        }


    // Both native field contracts use the same subclass-first lookup.
    fun resolveField(
        type: Class<*>,
        name: String,
        acceptsType: (Class<*>) -> Boolean,
    ): Field? =
        generateSequence(type) { current -> current.superclass }
            .mapNotNull { current ->
                current.declaredFields.firstOrNull { field ->
                    field.name == name && acceptsType(field.type)
                }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    fun readBoolean(owner: Any, field: Field): Boolean? =
        optionalNativeRead { field.get(owner) as? Boolean }

    internal fun matchesToggleAodSignature(
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    ): Boolean =
        parameterTypes.size == 1 &&
            isBooleanType(parameterTypes[0]) &&
            returnType == Void.TYPE

    internal fun isBooleanType(type: Class<*>): Boolean =
        type == Boolean::class.javaPrimitiveType ||
            type == Boolean::class.javaObjectType
}
