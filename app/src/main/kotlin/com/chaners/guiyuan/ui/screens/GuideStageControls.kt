package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideStageHeading(step: Int) {
    val title = when (step) {
        1 -> stringResource(R.string.guide_permissions_title)
        2 -> stringResource(R.string.guide_privacy_title)
        else -> stringResource(R.string.guide_basic_title)
    }
    val illustration = when (step) {
        1 -> R.drawable.ic_material_symbol_security
        2 -> R.drawable.ic_material_symbol_balance
        else -> R.drawable.ic_material_symbol_settings
    }
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier.fillMaxWidth().height(82.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MiuixTheme.colorScheme.primary,
        )
    }
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onBackground,
        )
    }
    Spacer(Modifier.height(30.dp))
}

@Composable
internal fun GuideStageContinueButton(
    step: Int,
    canContinue: Boolean,
    showSecondary: Boolean,
    onContinue: () -> Unit,
) {
    TextButton(
        text = stringResource(
            if (step == 2) R.string.guide_agree_continue else R.string.guide_continue,
        ),
        modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth().heightIn(min = 50.dp),
        cornerRadius = 16.dp,
        textStyle = MiuixTheme.textStyles.button,
        colors = if (showSecondary) {
            ButtonDefaults.textButtonColors()
        } else {
            ButtonDefaults.textButtonColorsPrimary()
        },
        enabled = canContinue,
        onClick = onContinue,
    )
}
