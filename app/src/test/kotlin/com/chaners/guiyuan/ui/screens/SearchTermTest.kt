package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTermTest {
    @Test
    fun blankInputDoesNotEnterFilteredMode() {
        assertFalse(isSearchFiltering(false, ""))
        assertFalse(isSearchFiltering(true, ""))
        assertFalse(isSearchFiltering(true, "  \t"))
        assertFalse(isSearchFiltering(false, "5G"))
        assertTrue(isSearchFiltering(true, "5G"))
        assertTrue(isSearchFiltering(true, "电量"))
    }

    @Test
    fun chineseAndPinyinPrefixes() {
        val term = SearchTerm("电量数字", "dian liang shu zi")
        assertTrue(term.matches("电量"))
        assertTrue(term.matches("数字"))
        assertTrue(term.matches("dl"))
        assertTrue(term.matches("dlsz"))
        assertTrue(term.matches("dianliangshuzi"))
        assertFalse(term.matches("spxh"))
    }

    @Test
    fun chineseInitialsAndLatinNames() {
        val signal = SearchTerm("双排信号", "shuang pai xin hao")
        assertTrue(signal.matches("spxh"))
        assertTrue(signal.matches("shuangpai"))
        val wifi = SearchTerm("Wi-Fi 大小", "Wi-Fi da xiao")
        assertTrue(wifi.matches("WIFI"))
        assertTrue(wifi.matches("wifi"))
        assertFalse(wifi.matches("电量"))
    }

    @Test
    fun blankAndPunctuationOnlyQueriesDoNotMatch() {
        val term = SearchTerm("网络制式", "wang luo zhi shi")
        assertFalse(term.matches(""))
        assertFalse(term.matches(" - "))
        assertTrue(term.matches("wlzs"))
        assertTrue(term.matches("wangluozhishi"))
    }

    @Test
    fun colorIsNotAWeightOrSizeMatch() {
        val color = SearchTerm("电量颜色", "dian liang yan se")
        assertTrue(color.matches("颜色"))
        assertTrue(color.matches("电量颜色"))
        assertFalse(color.matches("粗细"))
        assertFalse(color.matches("字重"))
        assertFalse(color.matches("大小"))
        assertFalse(color.matches("cx"))
    }

    @Test
    fun specificSynonymsDoNotIncludeUnrelatedControls() {
        val weight = SearchTerm("数字字重", "shu zi zi zhong")
        val weightAlias = SearchTerm("电量数字粗细", "dian liang shu zi cu xi")
        assertTrue(weight.matches("字重"))
        assertTrue(weightAlias.matches("粗细"))
        assertFalse(weight.matches("颜色"))
        assertFalse(weightAlias.matches("颜色"))

        val color = SearchTerm("电池颜色", "dian chi yan se")
        assertTrue(color.matches("电池颜色"))
        assertFalse(color.matches("字重"))
    }

    @Test
    fun cellularSynonymsCoverOnlyRelatedFunctions() {
        val cellularEntries = listOf(
            R.string.mobile_type_size,
            R.string.mobile_type_weight,
            R.string.mobile_type_vertical_offset,
            R.string.mobile_follow_battery_color,
        )
        for (id in cellularEntries) {
            val words = searchAliases(id)
            for (word in listOf("5G", "流量", "蜂窝", "移动数据", "移动网络", "LTE")) {
                assertTrue("Missing $word for $id", words.any { SearchTerm(it, it).matches(word) })
            }
        }

        for (id in listOf(
            R.string.battery_colors, R.string.battery_top_text_weight,
            R.string.wifi_weight, R.string.battery_charging_icon,
        )) {
            val words = searchAliases(id)
            for (word in listOf("流量", "蜂窝", "5G")) {
                assertFalse("Unrelated $word in $id", words.any { SearchTerm(it, it).matches(word) })
            }
        }
    }

    @Test
    fun batteryAndWeightSynonymsRetainMeaning() {
        val weight = searchAliases(R.string.mobile_type_weight)
        assertTrue(weight.any { it.contains("粗细") })
        assertTrue(weight.any { it.contains("字重") })
        val battery = SearchTerm("低电量阈值", "di dian liang yu zhi")
        assertTrue(battery.matches("电量"))
        assertFalse(searchAliases(R.string.battery_colors).any {
            SearchTerm(it, it).matches("粗细")
        })
    }
}
