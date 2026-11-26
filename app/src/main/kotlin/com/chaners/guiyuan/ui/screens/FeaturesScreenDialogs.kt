package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import com.chaners.guiyuan.ui.components.dialogActionGap
import kotlin.math.roundToInt

internal data class FeaturesConfirmText(
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
)

/** Both restore and reset use the same MIUIX confirm/cancel layout and busy gate. */
@Composable
internal fun FeaturesConfirmationDialog(
    show: Boolean,
    labels: FeaturesConfirmText,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    UiOverlayDialog(
        title = stringResource(labels.titleRes),
        summary = stringResource(labels.summaryRes),
        show = show,
        onDismissRequest = { if (!busy) onDismiss() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            UiDialogButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = onDismiss,
            )
            Spacer(Modifier.width(dialogActionGap()))
            UiDialogButton(
                text = stringResource(R.string.restore),
                modifier = Modifier.weight(1f),
                enabled = !busy,
                role = GlassDialogButtonRole.Primary,
                onClick = onConfirm,
            )
        }
    }
}

internal data class FeaturesWeightDialogModel(
    @StringRes val titleRes: Int,
    val weight: FeatureWeightUi,
    val currentValue: Int,
    val fallbackRange: IntRange,
)

@Composable
internal fun FeaturesWeightDialog(
    show: Boolean,
    model: FeaturesWeightDialogModel,
    onDismiss: () -> Unit,
    onConfirmed: (Int) -> Unit,
) {
    val weight = model.weight
    IntegerInputDialog(
        show = show && (weight.verifiedRange != null || weight.allowUnverified),
        model = IntegerInputModel(
            title = stringResource(model.titleRes),
            summary = weight.rangeText ?: if (weight.allowUnverified) stringResource(R.string.font_method_lab_unverified) else "", 
            value = model.currentValue.coerceIn(
                weight.valueRange.start.roundToInt(),
                weight.valueRange.endInclusive.roundToInt(),
            ),
            range = weight.verifiedRange ?: model.fallbackRange,
        ),
        callbacks = IntegerInputCallbacks(
            onDismiss = onDismiss,
            onConfirm = { value ->
                onDismiss()
                val range = weight.verifiedRange ?: model.fallbackRange.takeIf { weight.allowUnverified }
                range?.let { onConfirmed(value.coerceIn(it)) }
            },
        ),
    )
}
