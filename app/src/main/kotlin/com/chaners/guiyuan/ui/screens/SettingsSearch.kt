package com.chaners.guiyuan.ui.screens

import android.content.Context
import android.icu.text.Transliterator
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.chaners.guiyuan.R
import java.util.Locale

// Index terms, not configuration values. The native ICU table supplies Han pronunciations.
internal class SearchTerm(text: String, romanized: String) {
    private val plain = fold(text)
    private val tokens = romanized.lowercase(Locale.ROOT)
        .split(Regex("[^a-z0-9]+"))
        .filter(String::isNotEmpty)
    private val full = tokens.joinToString("")
    private val initials = tokens.joinToString("") { it.take(1) }

    fun matches(input: String): Boolean {
        val term = fold(input)
        return term.isNotEmpty() &&
            (term in plain || term in full || term in initials)
    }

    private fun fold(text: String) =
        text.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
}

internal class PageSearch(private val context: Context) {
    var active by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set

    private val romanizer by lazy(LazyThreadSafetyMode.NONE) {
        Transliterator.getInstance("Han-Latin; Latin-ASCII")
    }
    private val cache = mutableMapOf<Int, SearchTerm>()
    private val aliasCache = mutableMapOf<Int, List<SearchTerm>>()

    fun open() { active = true }

    fun close() {
        active = false
        query = ""
    }

    fun update(value: String) { query = value.take(80) }

    fun matches(@StringRes title: Int, @StringRes summary: Int? = null): Boolean {
        if (!active || query.isBlank()) return true
        return matchesId(title) || (summary != null && matchesId(summary)) ||
            aliasCache.getOrPut(title) { searchAliases(title).map(::makeTerm) }
                .any { it.matches(query) }
    }

    fun any(vararg ids: Int): Boolean =
        !active || query.isBlank() || ids.any { matches(it) }

    private fun matchesId(@StringRes id: Int): Boolean =
        cache.getOrPut(id) { makeTerm(context.getString(id)) }.matches(query)

    private fun makeTerm(text: String): SearchTerm =
        SearchTerm(text, romanizer.transliterate(text))

    private fun searchAliases(@StringRes title: Int): List<String> = when (title) {
        R.string.dual_signal_title, R.string.dual_dot_style_title,
        R.string.dual_mobile_vertical_offset -> listOf("双卡", "双排")
        R.string.mobile_type_size, R.string.mobile_type_vertical_offset -> listOf("网络制式", "5G")
        R.string.mobile_type_weight -> listOf("网络制式", "5G", "字体粗细", "数字粗细")
        R.string.battery_top_text_weight -> listOf("字体粗细", "数字粗细", "电量数字")
        R.string.battery_top_text_size -> listOf("电量数字", "数字大小")
        R.string.battery_top_readout -> listOf("电量数字", "电池数字")
        R.string.wifi_size, R.string.wifi_weight -> listOf("wifi", "wlan", "无线网络")
        R.string.restart_scope -> listOf("systemui", "重启系统界面")
        else -> emptyList()
    }
}

@Composable
internal fun rememberPageSearch(): PageSearch {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    return remember(context, config) { PageSearch(context) }
}
