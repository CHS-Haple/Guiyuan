package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTermTest {
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
}
