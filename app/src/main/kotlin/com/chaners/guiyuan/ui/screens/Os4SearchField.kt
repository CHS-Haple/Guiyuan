package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import top.yukonga.miuix.kmp.blur.Backdrop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.icons.Os4GlassSearch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Search field geometry follows PR #423's GlassSearchOverlay; surface belongs to the parent bar. */
@Composable
internal fun Os4SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    active: Boolean,
    backdrop: Backdrop?,
    materialProgress: State<Float>,
    shadowProgress: State<Float>,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        enabled = active,
        singleLine = true,
        textStyle = MiuixTheme.textStyles.body1.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .focusRequester(focusRequester),
        decorationBox = { field ->
            Os4SearchSurface(
                modifier = Modifier.fillMaxSize(),
                backdrop = backdrop,
                materialProgress = materialProgress,
                shadowProgress = shadowProgress,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Os4GlassSearch,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colors.disabledOnSurface,
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.feature_search),
                                style = MiuixTheme.textStyles.body1,
                                color = colors.disabledOnSurface,
                            )
                        }
                        field()
                    }
                    if (query.isNotEmpty()) {
                        TooltipBox(text = stringResource(R.string.feature_search_clear)) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    focusRequester.requestFocus()
                                },
                                modifier = Modifier.size(24.dp),
                                minHeight = 24.dp,
                                minWidth = 24.dp,
                            ) {
                                Icon(
                                    MiuixIcons.Basic.SearchCleanup,
                                    contentDescription = stringResource(R.string.feature_search_clear),
                                    tint = colors.onSurfaceContainerHighest,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
