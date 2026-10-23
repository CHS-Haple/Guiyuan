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
        runCatching {
            val ids = Transliterator.getAvailableIDs()
            var han = false
            var latinAscii = false
            while (ids.hasMoreElements()) {
                when (ids.nextElement()) {
                    "Han-Latin" -> han = true
                    "Latin-ASCII" -> latinAscii = true
                }
            }
            if (han && latinAscii) Transliterator.getInstance("Han-Latin; Latin-ASCII")
            else null
        }.getOrNull()
    }
    private val cache = mutableMapOf<Int, SearchTerm>()
    private val aliasCache = mutableMapOf<Int, List<SearchTerm>>()

    fun open() { active = true }

    fun close() {
        active = false
        query = ""
    }

    fun update(value: String) { query = value.take(80) }

    fun matches(@StringRes title: Int): Boolean {
        if (!active || query.isBlank()) return true
        return matchesId(title) ||
            aliasCache.getOrPut(title) { searchAliases(title).map(::makeTerm) }
                .any { it.matches(query) }
    }

    fun any(vararg ids: Int): Boolean =
        !active || query.isBlank() || ids.any(::matches)

    private fun matchesId(@StringRes id: Int): Boolean =
        cache.getOrPut(id) { makeTerm(context.getString(id)) }.matches(query)

    private fun makeTerm(text: String): SearchTerm =
        SearchTerm(text, runCatching { romanizer?.transliterate(text) }.getOrNull() ?: text)

    // Curated task vocabulary has lower recall than free-text descriptions but avoids
    // pulling unrelated controls into searches through incidental summary words.
    private fun searchAliases(@StringRes title: Int): List<String> = when (title) {
        R.string.keyguard_combined_status_title -> listOf("锁屏状态栏", "锁屏图标")
        R.string.aod_combined_status_title -> listOf("息屏显示", "熄屏显示", "常亮显示")
        R.string.control_center_tint_transition -> listOf("控制中心变色", "下拉颜色过渡")
        R.string.battery_fill_follow_retract -> listOf("电池段收口", "电量环回收", "电量段动画")
        R.string.content_layout_title -> listOf("排列", "网络居中", "电量居中")
        R.string.dual_signal_title -> listOf("双卡", "双 SIM", "双卡信号")
        R.string.dual_dot_style_title -> listOf("双卡样式", "Duo", "八点")
        R.string.dual_mobile_vertical_offset -> listOf("双卡偏移", "双排位置", "双排上下偏移")
        R.string.combined_size -> listOf("整体缩放", "总大小")
        R.string.outer_weight -> listOf("环形粗细", "圆环粗细", "外环宽度")
        R.string.wifi_vertical_offset -> listOf("WiFi 位置", "WiFi 垂直位置", "无线位置")
        R.string.mobile_type_vertical_offset -> listOf("5G 位置", "制式位置")
        R.string.top_info_vertical_offset -> listOf("顶部位置", "信息位置")
        R.string.wifi_size -> listOf("wifi", "wlan", "无线", "wifi缩放")
        R.string.wifi_weight -> listOf("wifi", "wlan", "无线", "无线图标粗细", "wifi线条")
        R.string.airplane_size -> listOf("飞机图标大小", "飞行图标缩放")
        R.string.airplane_weight -> listOf("飞机图标粗细", "飞行图标粗细", "线条粗细")
        R.string.no_sim_size -> listOf("无卡图标", "无卡大小", "SIM")
        R.string.mobile_type_size -> listOf("网络制式", "5G 大小", "5GA 大小", "网络文字大小")
        R.string.mobile_type_weight -> listOf("网络制式", "5G 字重", "5GA 字重", "5G 粗细", "字体粗细", "数字粗细")
        R.string.mobile_follow_battery_color -> listOf("移动信号颜色", "信号点颜色")
        R.string.center_follow_battery_color -> listOf("网络图标颜色", "WiFi 颜色", "5G 颜色", "飞行模式颜色")
        R.string.battery_colors -> listOf("电池颜色", "电量环配色", "电池配色", "配色方案")
        R.string.low_battery_threshold -> listOf("低电量警告", "电池阈值", "低电量门限")
        R.string.battery_top_readout -> listOf("电量数值", "电池数字", "电量百分比")
        R.string.battery_top_text_size -> listOf("电量数字大小", "电池数字大小")
        R.string.battery_top_text_weight -> listOf("电量数字粗细", "电池数字粗细", "电量数字字重", "字体粗细", "数字粗细")
        R.string.battery_text_follow_battery_color -> listOf("电量数字颜色", "数字跟随电池颜色")
        R.string.battery_charging_icon -> listOf("充电闪电", "充电标志", "充电图标显示")
        R.string.battery_top_charging_icon_size -> listOf("充电图标大小", "闪电大小")
        R.string.charging_icon_follow_battery_color -> listOf("闪电颜色", "充电图标颜色")
        R.string.config_backup_title -> listOf("导出配置", "保存设置", "备份设置")
        R.string.config_restore_title -> listOf("导入配置", "读取备份", "恢复备份")
        R.string.restore_feature_defaults -> listOf("重置设置", "重置功能", "恢复功能默认值")
        else -> emptyList()
    }
}

@Composable
internal fun rememberPageSearch(): PageSearch {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    return remember(context, config) { PageSearch(context) }
}
