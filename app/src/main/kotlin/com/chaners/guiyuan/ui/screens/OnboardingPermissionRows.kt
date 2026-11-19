package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuidePermissionItem(
    @DrawableRes iconRes: Int,
    title: String,
    status: String,
    purpose: String,
    checked: Boolean,
    expanded: Boolean,
    onToggleDetails: () -> Unit,
    guidance: String? = null,
) {
    val detailsLabel = stringResource(R.string.guide_permission_details)
    BasicComponent(
        startAction = { SemanticLeadingIcon(iconRes = iconRes) },
        endActions = {
            if (checked) {
                Icon(
                    imageVector = MiuixIcons.Basic.Check,
                    contentDescription = status,
                    modifier = Modifier.size(26.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = status,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.onBackground,
            )
            TooltipBox(text = detailsLabel) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = onToggleDetails,
                        modifier = Modifier.size(32.dp),
                        minWidth = 32.dp,
                        minHeight = 32.dp,
                        cornerRadius = 16.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Help,
                            contentDescription = detailsLabel,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    }
                }
            }
        }
    }
    GuidePermissionDetails(expanded = expanded, purpose = purpose, guidance = guidance)
}

@Composable
private fun GuidePermissionDetails(
    expanded: Boolean,
    purpose: String,
    guidance: String?,
) {
    if (expanded) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.guide_purpose_label),
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = purpose,
                    style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.4f.em),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                if (guidance != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.guide_guidance_label),
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = guidance,
                        style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.4f.em),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
