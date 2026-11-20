package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AgreementReadingRequirements
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
internal fun ColumnScope.GuidePrivacyStage(
    requirements: AgreementReadingRequirements,
    importantScroll: ScrollState,
    termsViewed: Boolean,
    privacyViewed: Boolean,
    onOpen: (AgreementDocument) -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
            .weight(1f).padding(horizontal = 4.dp),
    ) {
        GuideAgreementCard(
            titleRes = R.string.terms_title,
            summaryRes = R.string.about_terms_summary,
            iconRes = R.drawable.ic_material_symbol_contract,
            required = requirements.terms && !termsViewed,
            onClick = { onOpen(AgreementDocument.Terms) },
        )
        Spacer(Modifier.height(12.dp))
        GuideAgreementCard(
            titleRes = R.string.privacy_title,
            summaryRes = R.string.about_privacy_summary,
            iconRes = R.drawable.ic_material_symbol_policy,
            required = requirements.privacy && !privacyViewed,
            onClick = { onOpen(AgreementDocument.Privacy) },
        )
        GuidePrivacyNotes(importantScroll)
    }
}

@Composable
internal fun ColumnScope.GuideAppSettingsStage(
    config: OnboardingConfig,
    handlers: OnboardingHandlers,
    entryState: AppSettingsEntryState,
) {
    SmallTitle(text = stringResource(R.string.section_app))
    Card(modifier = Modifier.fillMaxWidth()) {
        AppSettingsRows(
            lang = config.lang,
            iconHidden = config.iconHidden,
            onLangChange = handlers.onLangChange,
            onIconHiddenChange = handlers.onIconHiddenChange,
            entryState = entryState,
        )
    }
}
