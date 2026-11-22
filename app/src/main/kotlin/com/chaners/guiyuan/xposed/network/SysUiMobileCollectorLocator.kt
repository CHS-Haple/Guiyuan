package com.chaners.guiyuan.xposed.network

import android.widget.ImageView
import org.luckypray.dexkit.DexKitBridge

// Direct generated classes are used only when their reflected shape is compatible.
// Stable non-generated SystemUI hooks remain outside this dynamic resolver.
internal object SysUiMobileCollectorLocator {
    private const val BINDER_PACKAGE = "com.android.systemui.statusbar.pipeline.mobile.ui.binder"
    private const val CLASS_ID_FIELD = "\$r8\$classId"
    private const val OBJECT_TYPE_NAME = "java.lang.Object"

    enum class Role(val viewField: String, val methodName: String, val hasClassId: Boolean) {
        SIGNAL("\$mobile", "emit", true),
        ACTIVITY_RESOURCE("\$leftInOut", "invokeSuspend", false),
        ACTIVITY_VISIBILITY("\$satellite", "emit", true),
    }

    fun findMissing(
        classLoader: ClassLoader,
        knownClasses: Map<Role, String>,
    ): Map<Role, Class<*>> {
        val needsDiscovery = knownClasses.keys.filter { role ->
            try {
                Class.forName(knownClasses.getValue(role), false, classLoader)
                false
            } catch (_: ClassNotFoundException) {
                true
            }
        }
        if (needsDiscovery.isEmpty()) return emptyMap()

        // The caller already records install failures and restores native state.
        // Preserve scanner exceptions instead of reporting a false class-not-found.
        System.loadLibrary("dexkit")
        return DexKitBridge.create(classLoader, false).use { bridge ->
            val discovered = buildMap {
                for (role in needsDiscovery) {
                    findUniqueCandidate(bridge, role, classLoader)?.let { put(role, it) }
                }
            }
            // Never install two role-specific Hooks on one generated switch class.
            val knownNames = knownClasses.filterKeys { it !in needsDiscovery }.values
            if (hasDistinctOwners(knownNames + discovered.values.map(Class<*>::getName))) {
                discovered
            } else {
                emptyMap()
            }
        }
    }

    private fun findUniqueCandidate(
        bridge: DexKitBridge,
        role: Role,
        classLoader: ClassLoader,
    ): Class<*>? =
        bridge.findClass {
            searchPackages(BINDER_PACKAGE)
            matcher {
                fields {
                    add {
                        name = role.viewField
                        type = ImageView::class.java.name
                    }
                    if (role.hasClassId) {
                        add {
                            name = CLASS_ID_FIELD
                            type = "int"
                        }
                    }
                }
                methods {
                    add {
                        name = role.methodName
                        returnType = OBJECT_TYPE_NAME
                        if (role.methodName == "emit") {
                            paramTypes(OBJECT_TYPE_NAME, "kotlin.coroutines.Continuation")
                        } else {
                            paramTypes(OBJECT_TYPE_NAME)
                        }
                    }
                }
            }
        }.asSequence()
            .filter { data ->
                data.name.startsWith(BINDER_PACKAGE + ".MiuiMobileIconBinder\$bind\$")
            }
            .mapNotNull { data ->
                try {
                    Class.forName(data.name, false, classLoader)
                } catch (_: ClassNotFoundException) {
                    null
                }
            }
            .filter { candidate -> matchesCollectorShape(candidate, role, classLoader) }
            .singleOrNull()

    internal fun hasDistinctOwners(classNames: Collection<String>): Boolean =
        classNames.size == classNames.toSet().size

    // Structural proof only: actual signal semantics require runtime verification.
    internal fun matchesCollectorShape(
        candidate: Class<*>,
        role: Role,
        classLoader: ClassLoader,
    ): Boolean =
        try {
            if (candidate.getDeclaredField(role.viewField).type != ImageView::class.java) {
                false
            } else if (
                role.hasClassId &&
                candidate.getDeclaredField(CLASS_ID_FIELD).type != Int::class.javaPrimitiveType
            ) {
                false
            } else {
                val method = if (role.methodName == "emit") {
                    candidate.getDeclaredMethod(
                        role.methodName,
                        Any::class.java,
                        Class.forName("kotlin.coroutines.Continuation", false, classLoader),
                    )
                } else {
                    candidate.getDeclaredMethod(role.methodName, Any::class.java)
                }
                method.returnType == Any::class.java
            }
        } catch (_: ReflectiveOperationException) {
            false
        }
}
