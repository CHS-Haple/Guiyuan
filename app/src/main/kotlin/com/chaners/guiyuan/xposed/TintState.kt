package com.chaners.guiyuan.xposed

internal data class TintState(
    val appliedTint: Int,
    val statusIconTint: Int? = null,
)

// 过渡提交期间保留旧名，下一步收完引用后删掉。
internal typealias CombinedStatusTintState = TintState
