package com.chaners.guiyuan.xposed

internal object PresentationPolicy {
    fun resolveModel(
        previous: RenderModel?,
        candidate: RenderModel?,
    ): RenderModel? =
        candidate ?: previous

    fun resolveTint(
        previous: TintState?,
        candidate: TintState,
    ): TintState? =
        if (alpha(candidate.appliedTint) == 0) {
            previous
        } else {
            candidate
        }

    fun isValidTint(state: TintState): Boolean =
        alpha(state.appliedTint) != 0

    private fun alpha(color: Int): Int =
        color ushr 24
}
