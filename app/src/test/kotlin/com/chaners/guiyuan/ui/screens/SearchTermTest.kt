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
        assertFalse(isSearchFiltering(true, "---"))
        assertFalse(isSearchFiltering(true, "。！？"))
        assertFalse(isSearchFiltering(true, "🙂"))
        assertTrue(isSearchFiltering(true, "5G-"))
        assertFalse(isSearchFiltering(false, "5G"))
        assertTrue(isSearchFiltering(true, "5G"))
        assertTrue(isSearchFiltering(true, "电量"))
    }

    @Test
    fun preFoldedQueriesPreserveRegularAndInitialsMatches() {
        val terms = listOf(
            SearchTerm("网络制式", "wang luo zhi shi"),
            SearchTerm("Wi-Fi size", "Wi-Fi size", foreign = true),
            SearchTerm("电量颜色", "dian liang yan se", foreign = true),
        )
        val queries = listOf("", "-  ", "5G", "Wi-FI", "wlzs", "wfs", "电量", "DLYS", "颜色")
        for (term in terms) {
            for (query in queries) {
                val folded = query.lowercase(java.util.Locale.ROOT).filter(Char::isLetterOrDigit)
                assertTrue("Regular match differed for '$query'",
                    term.matches(query) == term.matchesFolded(folded))
                assertTrue("Exact-only match differed for '$query'",
                    term.matches(query, includeInitials = false) ==
                        term.matchesFolded(folded, includeInitials = false))
                assertTrue("Initials match differed for '$query'",
                    term.matchesInitials(query) == term.matchesInitialsFolded(folded))
            }
        }
        assertTrue(terms[0].matchesFolded("wlzs"))
        assertTrue(terms[1].matchesFolded("wifi", includeInitials = false))
        assertFalse(terms[1].matchesInitialsFolded("wfs"))
        assertTrue(terms[2].matchesInitialsFolded("dlys"))
        assertFalse(terms[2].matchesFolded("wifi"))
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
    fun groupsAndSearchScopeShareOneRegistry() {
        val grouped = FeatureSearchIds.display +
            FeatureSearchIds.layout +
            FeatureSearchIds.network +
            FeatureSearchIds.battery +
            FeatureSearchIds.management
        assertTrue(grouped.isNotEmpty())
        assertTrue(grouped.contentEquals(FeatureSearchIds.all))
        assertTrue(grouped.toSet().size == grouped.size)
        assertTrue(R.string.battery_top_readout in grouped)
        assertTrue(R.string.config_restore_title in grouped)
        assertTrue(R.string.restore_feature_defaults in grouped)
    }
}
