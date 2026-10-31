package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.sp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class PolicySection(
    @StringRes val title: Int,
    @StringRes val paragraphs: List<Int>,
)

private val policySections = listOf(
    PolicySection(R.string.privacy_local_title, listOf(R.string.privacy_local_body)),
    PolicySection(
        R.string.privacy_diagnostics_title,
        listOf(R.string.privacy_diagnostics_body, R.string.privacy_root_body, R.string.privacy_clear_body),
    ),
    PolicySection(
        R.string.privacy_export_title,
        listOf(R.string.privacy_export_body, R.string.privacy_export_caution),
    ),
    PolicySection(
        R.string.privacy_feedback_title,
        listOf(R.string.privacy_feedback_body, R.string.privacy_security_body),
    ),
)

@Composable
internal fun PrivacyPolicyScreen(onBack: () -> Unit) {
    SettingsPage(
        title = stringResource(R.string.privacy_title),
        onBack = onBack,
    ) {
        item {
            PrivacyPolicyArticle()
        }
    }
}

@Composable
internal fun PrivacyPolicySheetBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        PrivacyPolicyArticle()
    }
}

@Composable
private fun PrivacyPolicyArticle() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 16.dp),
    ) {
        PolicyParagraph(R.string.privacy_intro)
        policySections.forEach { section ->
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(section.title),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                section.paragraphs.forEach { PolicyParagraph(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PolicyParagraph(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.fillMaxWidth(),
        style = MiuixTheme.textStyles.body2.copy(lineHeight = 26.sp),
        color = MiuixTheme.colorScheme.onSurface,
    )
}
