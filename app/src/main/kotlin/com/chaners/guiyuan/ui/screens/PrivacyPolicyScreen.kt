package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PrivacyPolicyScreen(onBack: () -> Unit) {
    SettingsPage(
        title = stringResource(R.string.privacy_title),
        onBack = onBack,
    ) {
        item {
            PrivacyParagraph(R.string.privacy_intro)
        }
        Section(R.string.privacy_local_title) {
            PrivacyParagraph(R.string.privacy_local_body)
        }
        Section(R.string.privacy_diagnostics_title) {
            PrivacyParagraph(R.string.privacy_diagnostics_body)
            PrivacyParagraph(R.string.privacy_root_body)
            PrivacyParagraph(R.string.privacy_clear_body)
        }
        Section(R.string.privacy_export_title) {
            PrivacyParagraph(R.string.privacy_export_body)
            PrivacyParagraph(R.string.privacy_export_caution)
        }
        Section(R.string.privacy_feedback_title) {
            PrivacyParagraph(R.string.privacy_feedback_body)
            PrivacyParagraph(R.string.privacy_security_body)
        }
    }
}

@Composable
private fun PrivacyParagraph(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MiuixTheme.textStyles.body2,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}


// The OOBE sheet uses the same offline string resources as the About page.
@Composable
internal fun PrivacyPolicySheetBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        PrivacyParagraph(R.string.privacy_intro)
        for ((heading, bodies) in listOf(
            R.string.privacy_local_title to listOf(R.string.privacy_local_body),
            R.string.privacy_diagnostics_title to listOf(
                R.string.privacy_diagnostics_body,
                R.string.privacy_root_body,
                R.string.privacy_clear_body,
            ),
            R.string.privacy_export_title to listOf(
                R.string.privacy_export_body,
                R.string.privacy_export_caution,
            ),
            R.string.privacy_feedback_title to listOf(
                R.string.privacy_feedback_body,
                R.string.privacy_security_body,
            ),
        )) {
            Text(
                text = stringResource(heading),
                style = MiuixTheme.textStyles.title4,
                modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
            )
            bodies.forEach { PrivacyParagraph(it) }
        }
    }
}
