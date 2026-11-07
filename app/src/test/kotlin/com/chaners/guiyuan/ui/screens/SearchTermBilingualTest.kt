package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTermBilingualTest {
    @Test
    fun translatedTitlesMatchInEitherInterfaceLanguage() {
        val localized = listOf(
            SearchTerm("电量颜色", "dian liang yan se"),
            SearchTerm("Battery colors", "Battery colors"),
        )
        for (word in listOf("电量", "Battery", "colors", "dlys")) {
            assertTrue("Translated title missing $word", localized.any { it.matches(word) })
        }
        assertFalse(localized.any { it.matches("Wi-Fi") })
        assertFalse(localized.any { it.matches("Reset") })

        val restore = listOf(
            SearchTerm("恢复配置", "hui fu pei zhi"),
            SearchTerm("Restore configuration", "Restore configuration"),
        )
        assertTrue(restore.any { it.matches("恢复") })
        assertTrue(restore.any { it.matches("Restore") })
        assertTrue(restore.any { it.matches("configuration") })
        assertFalse(restore.any { it.matches("Backup") })
    }

    @Test
    fun nativeInitialsAreNotConfusedWithForeignLatinInitials() {
        val native = SearchTerm("Wi-Fi 上下偏移", "Wi-Fi shang xia pian yi")
        val foreign = SearchTerm("Wi-Fi size", "Wi-Fi size", foreign = true)
        assertTrue(native.matches("wfs"))
        assertFalse(foreign.matchesInitials("wfs"))
        assertFalse(foreign.matches("wfs"))
        assertTrue(foreign.matches("WiFi"))
        assertTrue(foreign.matches("wifisize"))
    }

    @Test
    fun officialTranslatedNamesRemainSearchableAlongsideNativeMatches() {
        val ids = intArrayOf(1, 2)
        val query = "wifi"
        val native = mapOf(
            1 to SearchTerm("Wi-Fi 大小", "Wi-Fi da xiao"),
            2 to SearchTerm("网络图标颜色", "wang luo tu biao yan se"),
        )
        val foreign = mapOf(
            1 to SearchTerm("Wi-Fi size", "Wi-Fi size", foreign = true),
            2 to SearchTerm("Wi-Fi icon color", "Wi-Fi icon color", foreign = true),
        )
        val matches = SearchMatches(
            ids,
            { native[it]?.matches(query) == true },
            { false },
            { foreign[it]?.matches(query, includeInitials = false) == true },
            { foreign[it]?.matchesInitials(query) == true },
        )
        assertTrue(matches.matches(1))
        assertTrue("Another translated official title must not be suppressed", matches.matches(2))
    }

    @Test
    fun translatedHanPinyinInitialsAreAvailableAsFallback() {
        val ids = intArrayOf(1, 2)
        val native = mapOf(
            1 to SearchTerm("Battery colors", "Battery colors"),
            2 to SearchTerm("Content layout", "Content layout"),
        )
        val foreign = mapOf(
            1 to SearchTerm("电量颜色", "dian liang yan se", foreign = true),
            2 to SearchTerm("内容布局", "nei rong bu ju", foreign = true),
        )
        val query = "dl"
        val result = SearchMatches(
            ids,
            { native[it]?.matches(query) == true },
            { false },
            { foreign[it]?.matches(query, includeInitials = false) == true },
            { foreign[it]?.matchesInitials(query) == true },
        )
        assertTrue(foreign.getValue(1).matchesInitials(query))
        assertTrue(result.matches(1))
        assertFalse(result.matches(2))
    }

    @Test
    fun nativeInitialismOutranksAmbiguousForeignPinyinInitials() {
        val query = "wfs"
        val native = mapOf(
            1 to SearchTerm("Wi-Fi size", "Wi-Fi size"),
            2 to SearchTerm("Wi-Fi vertical offset", "Wi-Fi vertical offset"),
        )
        val foreign = mapOf(
            1 to SearchTerm("Wi-Fi 大小", "Wi-Fi da xiao", foreign = true),
            2 to SearchTerm("Wi-Fi 上下偏移", "Wi-Fi shang xia pian yi", foreign = true),
        )
        val result = SearchMatches(
            intArrayOf(1, 2),
            { native[it]?.matches(query) == true },
            { false },
            { foreign[it]?.matches(query, includeInitials = false) == true },
            { foreign[it]?.matchesInitials(query) == true },
        )
        assertTrue(native.getValue(1).matchesInitials(query))
        assertTrue(foreign.getValue(2).matchesInitials(query))
        assertTrue(result.matches(1))
        assertFalse("Chinese pinyin fallback must not steal an English native acronym",
            result.matches(2))
    }

    @Test
    fun curatedAliasOutranksUnrelatedForeignInitials() {
        val query = "wl"
        val aliases = SearchTerm("网络", "wang luo")
        val foreign = SearchTerm("未来", "wei lai", foreign = true)
        val result = SearchMatches(
            intArrayOf(1, 2),
            { false },
            { it == 1 && aliases.matches(query) },
            { false },
            { it == 2 && foreign.matchesInitials(query) },
        )
        assertTrue(aliases.matches(query))
        assertTrue(foreign.matchesInitials(query))
        assertTrue(result.matches(1))
        assertFalse("An exact curated alias must suppress unrelated foreign initials", result.matches(2))
    }

    @Test
    fun bilingualWordSearchWorksInBothDirections() {
        val batteryZh = SearchTerm("电量颜色", "dian liang yan se")
        val batteryEn = SearchTerm("Battery colors", "Battery colors", foreign = true)
        val restoreZh = SearchTerm("恢复配置", "hui fu pei zhi")
        val restoreEn = SearchTerm("Restore configuration", "Restore configuration", foreign = true)
        for (word in listOf("电量", "dianliang", "dlys")) {
            assertTrue(batteryZh.matches(word))
        }
        for (word in listOf("Battery", "colors", "batterycolors")) {
            assertTrue(batteryEn.matches(word))
        }
        assertTrue(restoreZh.matches("恢复"))
        assertTrue(restoreEn.matches("Restore"))
        assertTrue(restoreEn.matches("configuration"))
        assertFalse(batteryEn.matches("wifi"))
        assertFalse(restoreEn.matches("Backup"))
    }

    @Test
    fun aliasesRemainAvailableWithoutMaskingOtherLanguageTitles() {
        val query = "信号"
        val result = SearchMatches(
            intArrayOf(1, 2),
            { false },
            { it == 1 && SearchTerm("双卡信号", "shuang ka xin hao").matches(query) },
            { it == 2 && SearchTerm("信号点上下偏移", "xin hao dian shang xia pian yi",
                foreign = true).matches(query, includeInitials = false) },
            { false },
        )
        assertTrue("Curated alias must still match", result.matches(1))
        assertTrue("A translated official name must not be masked by aliases", result.matches(2))
    }
}
