package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.NavContent
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarDefaults
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun NavContentItem(
    content: NavContent,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    if (content == NavContent.IconOnly) {
        FloatingNavigationBarItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
            modifier = modifier,
        )
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val baseColor = MiuixTheme.colorScheme.onSurfaceContainer
    val stateAlpha =
        when {
            isPressed && selected -> NavigationBarDefaults.SelectedPressedAlpha
            isPressed -> NavigationBarDefaults.UnselectedPressedAlpha
            selected -> 1f
            else -> NavigationBarDefaults.UnselectedAlpha
        }
    val contentColor = baseColor.copy(alpha = baseColor.alpha * stateAlpha)

    Column(
        modifier =
            modifier.selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier =
                Modifier
                    .padding(start = 8.dp, top = 6.dp, end = 8.dp)
                    .size(FloatingNavigationBarDefaults.IconSize),
            tint = contentColor,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
            color = contentColor,
            textAlign = TextAlign.Center,
            fontSize = NavigationBarDefaults.LabelFontSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
