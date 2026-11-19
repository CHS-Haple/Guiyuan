package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideAgreementCard(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    @DrawableRes iconRes: Int,
    required: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = stringResource(titleRes),
            summary = stringResource(summaryRes),
            startAction = { SemanticLeadingIcon(iconRes = iconRes) },
            endActions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (required) Badge(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = MiuixIcons.Regular.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            },
            onClickLabel = if (required) stringResource(R.string.guide_open_required_document) else null,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun ColumnScope.GuidePrivacyNotes(importantScroll: ScrollState) {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.guide_important_note),
        modifier = Modifier.padding(horizontal = 12.dp)
            .semantics { heading() },
        style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.SemiBold),
        color = MiuixTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(importantScroll)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(
                R.string.guide_privacy_brief,
                R.string.guide_privacy_note_scope,
                R.string.guide_privacy_note_root,
                R.string.guide_privacy_note_diagnostics,
                R.string.guide_privacy_note_sharing,
            ).forEach { note ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "•",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = stringResource(note),
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.5f.em),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        // The accepted MIUIX scrollbar uses this ScrollState,
        // appears during a scroll and auto-fades afterwards.
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(importantScroll),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
internal fun GuidePrivacyAcknowledgment(
    accepted: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenDocument: (AgreementDocument) -> Unit,
) {
    Row(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
            .toggleable(
                value = accepted,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            state = ToggleableState(accepted),
            onClick = null,
        )
        val acknowledgment = stringResource(R.string.guide_privacy_ack)
        val terms = stringResource(R.string.guide_terms_link_text)
        val privacy = stringResource(R.string.guide_policy_link_text)
        val linkedAcknowledgment = buildAnnotatedString {
            append(acknowledgment)
            listOf(
                Triple(terms, AgreementDocument.Terms, "terms"),
                Triple(privacy, AgreementDocument.Privacy, "privacy"),
            ).forEach { (label, document, tag) ->
                val start = acknowledgment.indexOf(label)
                if (start >= 0) {
                    addLink(
                        LinkAnnotation.Clickable(
                            tag = tag,
                            linkInteractionListener = LinkInteractionListener {
                                onOpenDocument(document)
                            },
                        ),
                        start,
                        start + label.length,
                    )
                }
            }
        }
        Text(
            text = linkedAcknowledgment,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.4f.em),
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
    Spacer(Modifier.height(8.dp))
}
