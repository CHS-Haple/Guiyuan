package com.chaners.guiyuan.xposed.network

import android.widget.ImageView
import org.luckypray.dexkit.DexKitBridge

// Only missing compiler-generated classes are searched; stable hooks stay direct.
internal object SysUiMobileCollectorLocator {
    private const val BINDER_PACKAGE = "com.android.systemui.statusbar.pipeline.mobile.ui.binder"
    private const val CLASS_ID_FIELD = "\$r8\$classId"

    enum class Role(val viewField: String, val methodName: String, val hasClassId: Boolean) {
        SIGNAL("\$mobile", "emit", true),
        ACTIVITY_RESOURCE("\$leftInOut", "invokeSuspend", false),
        ACTIVITY_VISIBILITY("\$satellite", "emit", true),
    }

    fun findMissing(
        classLoader: ClassLoader,
        knownClasses: Map<Role, String>,
    ): Map<Role, Class<*>> {
        val missing = knownClasses.keys.filter { role ->
            try {
                Class.forName(knownClasses.getValue(role), false, classLoader)
                false
            } catch (_: ClassNotFoundException) {
                true
            }
        }
        if (missing.isEmpty()) return emptyMap()

        // The installing branch already records failures and restores native state.
        // Keep the real DexKit error instead of disguising it as a missing class.
        System.loadLibrary("dexkit")
        return DexKitBridge.create(classLoader, false).use { bridge ->
            val discovered = buildMap {
                for (role in missing) {
                    val candidates = bridge.findClass {
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
                                    returnType = "java.lang.Object"
                                    if (role.methodName == "emit") {
                                        paramTypes("java.lang.Object", "kotlin.coroutines.Continuation")
                                    } else {
                                        paramTypes("java.lang.Object")
                                    }
                                }
                            }
                        }
                    }.mapNotNull { data ->
                        if (!data.name.startsWith(BINDER_PACKAGE + ".MiuiMobileIconBinder\$bind\$")) {
                            null
                        } else {
                            try {
                                Class.forName(data.name, false, classLoader)
                            } catch (_: ClassNotFoundException) {
                                null
                            }
                        }
                    }.filter { candidate -> isVerifiedCandidate(candidate, role, classLoader) }

                    // An ambiguous match must not claim SystemUI ownership.
                    if (candidates.size == 1) put(role, candidates.single())
                }
            }
            // A generated switch class shared across two roles would install
            // overlapping Hooks on the same method. Never guess ownership.
            val originalNames = knownClasses.filterKeys { it !in missing }.values
            val allNames = originalNames + discovered.values.map(Class<*>::getName)
            if (hasDistinctOwners(allNames)) discovered else emptyMap()
        }
    }

    internal fun hasDistinctOwners(classNames: Collection<String>): Boolean =
        classNames.size == classNames.toSet().size

    internal fun isVerifiedCandidate(
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
                val params = if (role.methodName == "emit") {
                    arrayOf(Any::class.java, Class.forName("kotlin.coroutines.Continuation", false, classLoader))
                } else {
                    arrayOf(Any::class.java)
                }
                candidate.getDeclaredMethod(role.methodName, *params).returnType == Any::class.java
            }
        } catch (_: ReflectiveOperationException) {
            false
        }
}
