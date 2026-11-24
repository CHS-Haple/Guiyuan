package com.chaners.guiyuan.settings

// Pure sender and variable-font validation, separate from persisted report ownership.
internal object NativeFontAxisPolicy {
    internal fun state(reported: Boolean, axis: FontWeightAxis?): NativeFontAxis.State =
        if (!reported) NativeFontAxis.State.Pending
        else checked(axis)?.let(NativeFontAxis.State::Variable) ?: NativeFontAxis.State.Unavailable

    internal fun isSystemUiSender(uid: Int, pkg: String?, installedUid: Int?): Boolean =
        uid >= 0 && pkg == NativeFontAxis.SYSTEM_UI && uid == installedUid

    fun checked(axis: FontWeightAxis?): FontWeightAxis? =
        axis?.takeIf {
            it.min.isFinite() && it.default.isFinite() && it.max.isFinite() &&
                it.min >= 1f && it.min <= it.default && it.default <= it.max &&
                it.max <= 1000f && it.min < it.max
        }
}
