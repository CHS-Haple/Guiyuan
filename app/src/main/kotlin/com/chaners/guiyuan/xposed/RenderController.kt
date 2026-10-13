package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.network.SysUiDefaultDataSubSource

internal class RenderController(
    private val view: RenderView,
) {
    private var stableModel: RenderModel? = null
    private var previewShown = false
    private var stableTint: TintState? = null

    fun update(
        snapshot: StatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
        preview: RenderModel? = null,
    ): ModelUpdate {
        val defaultDataSubscriptionId =
            SysUiDefaultDataSubSource.currentSubscriptionId()
        val real =
            RenderModel.from(
                snapshot = snapshot,
                presentation = PresentationStore.snapshot(),
                defaultDataSubscriptionId = defaultDataSubscriptionId,
            )
        // A preview must not become the fallback for incomplete native state.
        val candidate = if (real != null) preview ?: real else null
        val previous = stableModel
        val model = candidate ?: previous.takeUnless { previewShown }
        previewShown = preview != null && real != null

        if (model != previous) {
            stableModel = model
            view.setModel(model, trace)
        }

        return ModelUpdate(
            candidateComplete = candidate != null,
            retainedStable = candidate == null && model != null,
            model = model,
            changed = model != previous,
            defaultDataSubscriptionId = defaultDataSubscriptionId,
        )
    }

    fun updateVisualCfg(visual: VisualCfg) {
        view.setVisualCfg(visual)
    }

    fun currentTintState(): TintState? = stableTint

    fun updateTint(state: TintState): TintUpdate {
        val previous = stableTint
        val validCandidate = state.isVisible
        val resolved =
            if (validCandidate) {
                state
            } else {
                previous
            }

        if (resolved != null && resolved != previous) {
            stableTint = resolved
            view.setTintState(resolved)
        }

        return TintUpdate(
            resolved = resolved,
            changed = resolved != null && resolved != previous,
            rejectedInvalidCandidate = !validCandidate,
        )
    }

    internal data class ModelUpdate(
        val candidateComplete: Boolean,
        val retainedStable: Boolean,
        val model: RenderModel?,
        val changed: Boolean,
        val defaultDataSubscriptionId: Int,
    )

    internal data class TintUpdate(
        val resolved: TintState?,
        val changed: Boolean,
        val rejectedInvalidCandidate: Boolean,
    )
}
