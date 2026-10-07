package com.chaners.guiyuan.xposed

internal data class TintState(
    val appliedTint: Int,
    val statusIconTint: Int? = null,
) {
    val isVisible: Boolean
        get() = (appliedTint ushr 24) != 0
}
