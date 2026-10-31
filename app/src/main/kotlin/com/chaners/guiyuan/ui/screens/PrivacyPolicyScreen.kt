package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class AgreementDocument { Terms, Privacy }

private data class AgreementSection(
    @StringRes val title: Int,
    @StringRes val paragraphs: List<Int>,
)

private val privacySections = listOf(
    AgreementSection(R.string.privacy_local_title, listOf(R.string.privacy_local_body, R.string.privacy_local_body_2)),
    AgreementSection(R.string.privacy_scope_title, listOf(R.string.privacy_scope_body, R.string.privacy_scope_body_2)),
    AgreementSection(R.string.privacy_diagnostics_title, listOf(R.string.privacy_diagnostics_body, R.string.privacy_diagnostics_body_2)),
    AgreementSection(R.string.privacy_retention_title, listOf(R.string.privacy_retention_body, R.string.privacy_retention_body_2)),
    AgreementSection(R.string.privacy_sharing_title, listOf(R.string.privacy_sharing_body, R.string.privacy_sharing_body_2)),
    AgreementSection(R.string.privacy_choice_title, listOf(R.string.privacy_choice_body, R.string.privacy_choice_body_2, R.string.privacy_choice_body_3)),
    AgreementSection(R.string.privacy_contact_title, listOf(R.string.privacy_contact_body)),
)

private val termsSections = listOf(
    AgreementSection(R.string.terms_scope_title, listOf(R.string.terms_scope_body)),
    AgreementSection(R.string.terms_access_title, listOf(R.string.terms_access_body, R.string.terms_access_body_2)),
    AgreementSection(R.string.terms_risk_title, listOf(R.string.terms_risk_body)),
    AgreementSection(R.string.terms_disclaimer_title, listOf(R.string.terms_disclaimer_body, R.string.terms_disclaimer_body_2)),
    AgreementSection(R.string.terms_license_title, listOf(R.string.terms_license_body, R.string.terms_license_body_2)),
    AgreementSection(R.string.terms_conduct_title, listOf(R.string.terms_conduct_body)),
    AgreementSection(R.string.terms_updates_title, listOf(R.string.terms_updates_body)),
)

internal fun AgreementDocument.titleRes(): Int =
    if (this == AgreementDocument.Terms) R.string.terms_title else R.string.privacy_title

@Composable
internal fun PrivacyPolicyScreen(onBack: () -> Unit) {
    AgreementScreen(AgreementDocument.Privacy, onBack)
}

@Composable
internal fun TermsOfUseScreen(onBack: () -> Unit) {
    AgreementScreen(AgreementDocument.Terms, onBack)
}

@Composable
private fun AgreementScreen(document: AgreementDocument, onBack: () -> Unit) {
    SettingsPage(
        title = stringResource(document.titleRes()),
        onBack = onBack,
    ) {
        item { AgreementArticle(document) }
    }
}

@Composable
internal fun AgreementSheetBody(document: AgreementDocument) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        AgreementArticle(document)
    }
}

@Composable
private fun AgreementArticle(document: AgreementDocument) {
    val intro = if (document == AgreementDocument.Terms) R.string.terms_intro else R.string.privacy_intro
    val sections = if (document == AgreementDocument.Terms) termsSections else privacySections
    val lastUpdated = if (document == AgreementDocument.Terms) R.string.terms_last_updated else R.string.privacy_last_updated
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .padding(horizontal = 28.dp).padding(top = 20.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(lastUpdated),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(12.dp))
            AgreementParagraph(intro)
            sections.forEach { section ->
                Spacer(Modifier.height(28.dp))
                Text(
                    text = stringResource(section.title),
                    modifier = Modifier.semantics { heading() },
                    style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.SemiBold),
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    section.paragraphs.forEach { AgreementParagraph(it) }
                }
            }
        }
    }
}

@Composable
private fun AgreementParagraph(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.fillMaxWidth(),
        style = MiuixTheme.textStyles.body1.copy(
            fontWeight = FontWeight.Normal,
            lineHeight = 1.5f.em,
        ),
        color = MiuixTheme.colorScheme.onSurface,
    )
}
