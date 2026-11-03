package com.chaners.guiyuan.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Share

// The published MIUIX baseline has no Glass icon module. Preserve legacy fallbacks.
// The dedicated native source set maps Export to Glass.Regular.Save instead.
internal val Os4GlassExport: ImageVector get() = MiuixIcons.Share
internal val Os4GlassEdit: ImageVector get() = MiuixIcons.Edit
internal val Os4GlassCopy: ImageVector get() = MiuixIcons.Copy
internal val Os4GlassDelete: ImageVector get() = MiuixIcons.Delete
