package com.chaners.guiyuan.xposed

internal object PresentationPolicy {
    fun resolveModel(
        previous: CombinedStatusRenderModel?,
        candidate: CombinedStatusRenderModel?,
    ): CombinedStatusRenderModel? =
        candidate ?: previous

    fun resolveTint(
        previous: CombinedStatusTintState?,
        candidate: CombinedStatusTintState,
    ): CombinedStatusTintState? =
        if (alpha(candidate.appliedTint) == 0) {
            previous
        } else {
            candidate
        }

    fun isValidTint(state: CombinedStatusTintState): Boolean =
        alpha(state.appliedTint) != 0

    private fun alpha(color: Int): Int =
        color ushr 24
}
