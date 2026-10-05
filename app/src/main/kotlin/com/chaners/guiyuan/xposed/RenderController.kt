package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusVisualSettings

internal class RenderController(
    private val view: RenderView,
) {
    private var stableModel: RenderModel? = null
    private var stableTint: CombinedStatusTintState? = null

    fun update(
        snapshot: CombinedStatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ): ModelUpdate {
        val defaultDataSubscriptionId =
            SystemUiDefaultDataSubscriptionSource.currentSubscriptionId()
        val candidate =
            RenderModel.from(
                snapshot = snapshot,
                presentation = PresentationStore.snapshot(),
                defaultDataSubscriptionId = defaultDataSubscriptionId,
            )
        val previous = stableModel
        val model =
            PresentationPolicy.resolveModel(
                previous = previous,
                candidate = candidate,
            )

        if (model != previous) {
            stableModel = model
            view.setModel(model, trace)
        }

        return ModelUpdate(
            candidateComplete = candidate != null,
            retainedStable = candidate == null && previous != null,
            model = model,
            changed = model != previous,
            defaultDataSubscriptionId = defaultDataSubscriptionId,
        )
    }

    fun updateVisualSettings(state: CombinedStatusVisualSettings) {
        view.setVisualSettings(state)
    }

    fun currentTintState(): CombinedStatusTintState? = stableTint

    fun updateTint(state: CombinedStatusTintState): TintUpdate {
        val previous = stableTint
        val resolved =
            PresentationPolicy.resolveTint(
                previous = previous,
                candidate = state,
            )
        val validCandidate =
            PresentationPolicy.isValidTint(state)

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
        val resolved: CombinedStatusTintState?,
        val changed: Boolean,
        val rejectedInvalidCandidate: Boolean,
    )
}
