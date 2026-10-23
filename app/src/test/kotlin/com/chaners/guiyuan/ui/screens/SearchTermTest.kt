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
}
