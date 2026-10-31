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
import androidx.compose.ui.res.stringResource
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
    AgreementSection(R.string.privacy_choice_title, listOf(R.string.privacy_choice_body, R.string.privacy_choice_body_2)),
    AgreementSection(R.string.privacy_contact_title, listOf(R.string.privacy_contact_body)),
)

private val termsSections = listOf(
    AgreementSection(R.string.terms_scope_title, listOf(R.string.terms_scope_body)),
    AgreementSection(R.string.terms_access_title, listOf(R.string.terms_access_body, R.string.terms_access_body_2)),
    AgreementSection(R.string.terms_risk_title, listOf(R.string.terms_risk_body, R.string.terms_risk_body_2)),
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
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
        AgreementParagraph(intro)
        sections.forEach { section ->
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(section.title),
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                section.paragraphs.forEach { AgreementParagraph(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AgreementParagraph(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.fillMaxWidth(),
        style = MiuixTheme.textStyles.paragraph,
        color = MiuixTheme.colorScheme.onSurface,
    )
}
