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
    fun ranksExactTitlesAheadOfPartialAndPinyinMatches() {
        val title = SearchTerm("电量颜色", "dian liang yan se")
        val related = SearchTerm("电量数字颜色", "dian liang shu zi yan se")
        assertTrue(title.rank("电量颜色") > related.rank("电量颜色"))
        assertTrue(title.rank("电量") > title.rank("dly"))
        assertTrue(title.rank("dlys") > 0)
        assertFalse(title.rank("字重") > 0)
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
    fun recoverySynonymsAreScopedToRecoveryActions() {
        val restore = searchAliases(R.string.config_restore_title)
        val defaults = searchAliases(R.string.restore_feature_defaults)
        val backup = searchAliases(R.string.config_backup_title)
        for (word in listOf("还原", "还原配置", "配置还原", "还原备份", "导入备份", "配置导入")) {
            assertTrue("Restore configuration: $word", restore.any { SearchTerm(it, it).matches(word) })
        }
        for (word in listOf("还原", "还原设置", "还原默认", "重置配置")) {
            assertTrue("Restore defaults: $word", defaults.any { SearchTerm(it, it).matches(word) })
        }
        for (word in listOf("配置导出", "保存配置")) {
            assertTrue("Backup configuration: $word", backup.any { SearchTerm(it, it).matches(word) })
        }
        for (word in listOf("还原", "重置配置", "导入备份")) {
            assertFalse("Backup should not handle: $word", backup.any { SearchTerm(it, it).matches(word) })
        }
        assertFalse(restore.any { SearchTerm(it, it).matches("导出") })
    }

    @Test
    fun highConfidenceAliasesStayTargeted() {
        val pairs = listOf(
            R.string.aod_combined_status_title to "AOD",
            R.string.dual_signal_title to "单排信号",
            R.string.combined_size to "三合一缩放",
            R.string.outer_weight to "电量环粗细",
            R.string.wifi_size to "无线图标尺寸",
            R.string.battery_colors to "电量配色",
            R.string.low_battery_threshold to "低电量提醒",
            R.string.battery_top_readout to "显示百分比",
        )
        for ((id, word) in pairs) {
            assertTrue("Alias $word missing from $id", searchAliases(id).any {
                SearchTerm(it, it).matches(word)
            })
        }
        for (id in listOf(R.string.wifi_weight, R.string.mobile_type_weight, R.string.airplane_weight)) {
            assertFalse(searchAliases(id).any { SearchTerm(it, it).matches("无线图标尺寸") })
            assertFalse(searchAliases(id).any { SearchTerm(it, it).matches("还原") })
        }
    }

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
    fun EnglishTaskSynonymsRemainScoped() {
        val restore = searchAliases(R.string.config_restore_title)
        val defaults = searchAliases(R.string.restore_feature_defaults)
        val backup = searchAliases(R.string.config_backup_title)
        assertTrue(restore.any { SearchTerm(it, it).matches("Import") })
        assertTrue(backup.any { SearchTerm(it, it).matches("Export") })
        assertTrue(defaults.any { SearchTerm(it, it).matches("Reset") })
        assertFalse(backup.any { SearchTerm(it, it).matches("Restore") })
        assertFalse(restore.any { SearchTerm(it, it).matches("Export") })
        assertFalse(defaults.any { SearchTerm(it, it).matches("Import") })
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
