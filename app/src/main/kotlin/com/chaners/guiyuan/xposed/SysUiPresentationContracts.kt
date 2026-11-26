package com.chaners.guiyuan.xposed

import java.lang.reflect.Field
import java.lang.reflect.Method

// Read-only exact-ROM reflection contracts. Native mutation, session ownership,
// Hook installation and rollback remain entirely in SysUiPresentationOwner.
internal object SysUiPresentationContracts {
    fun ignoredSlotsField(containerClass: Class<*>): Field? =
        generateSequence(containerClass) { clazz -> clazz.superclass }
            .mapNotNull { clazz ->
                clazz.declaredFields.firstOrNull { candidate ->
                    candidate.name == "ignoredSlots" &&
                        List::class.java.isAssignableFrom(candidate.type)
                }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    // Both native ignored-slot mutations require the same unique List contract.
    fun ignoredSlotsMethod(
        containerClass: Class<*>,
        name: String,
    ): Method? =
        containerClass.declaredMethods
            .filter { method ->
                method.name == name &&
                    method.parameterTypes.contentEquals(arrayOf(List::class.java)) &&
                    method.returnType == Void.TYPE
            }
            .singleOrNull()
            ?.apply { isAccessible = true }

    fun batteryHideField(batteryContainerClass: Class<*>): Field? =
        generateSequence(batteryContainerClass) { clazz -> clazz.superclass }
            .mapNotNull { clazz ->
                clazz.declaredFields.firstOrNull { candidate ->
                    candidate.name == "mIsHideBattery" &&
                        candidate.type == java.lang.Boolean.TYPE
                }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    fun batteryHideMethod(batteryContainerClass: Class<*>): Method? =
        batteryContainerClass.declaredMethods
            .firstOrNull { method ->
                method.name == "setIsHideBattery" &&
                    method.parameterTypes.contentEquals(
                        arrayOf(Boolean::class.javaObjectType),
                    ) &&
                    method.returnType == Void.TYPE
            }
            ?.apply { isAccessible = true }

    fun privacyField(batteryContainerClass: Class<*>): Field? =
        batteryContainerClass.declaredFields.singleOrNull { candidate ->
            candidate.name == "mPrivacyState" &&
                candidate.type.name ==
                    "com.android.systemui.statusbar.phone.MiuiHomePrivacyController\$PrivacyState"
        }?.apply { isAccessible = true }

    fun privacyMethod(batteryContainerClass: Class<*>, nativePrivacyField: Field): Method? =
        batteryContainerClass.declaredMethods.singleOrNull { method ->
            method.name == "setPrivacyState" &&
                method.parameterTypes.contentEquals(arrayOf(nativePrivacyField.type)) &&
                method.returnType == Void.TYPE
        }?.apply { isAccessible = true }

    fun measureMethod(containerClass: Class<*>): Method? =
        containerClass.declaredMethods
            .firstOrNull { method ->
                method.name == "onMeasure" &&
                    method.parameterTypes.contentEquals(
                        arrayOf(
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                        ),
                    ) &&
                    method.returnType == Void.TYPE
            }
            ?.apply { isAccessible = true }

    fun layoutMethod(containerClass: Class<*>): Method? =
        containerClass.declaredMethods
            .firstOrNull { method ->
                method.name == "onLayout" &&
                    method.parameterTypes.contentEquals(
                        arrayOf(
                            Boolean::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                        ),
                    ) &&
                    method.returnType == Void.TYPE
            }
            ?.apply { isAccessible = true }

    fun islandShowingMethod(containerClass: Class<*>): Method? =
        generateSequence(containerClass) { clazz -> clazz.superclass }
            .mapNotNull { clazz ->
                clazz.declaredMethods.firstOrNull { method ->
                    method.name == "getIslandShowing" &&
                        method.parameterCount == 0 &&
                        method.returnType == Boolean::class.javaPrimitiveType
                }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }
}
