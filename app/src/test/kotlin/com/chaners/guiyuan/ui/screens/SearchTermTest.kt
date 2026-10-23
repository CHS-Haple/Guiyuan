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
    fun currentLanguagePinyinTakesPrecedenceOverTranslatedInitials() {
        val native = mapOf(
            1 to SearchTerm("电量数字", "dian liang shu zi"),
            2 to SearchTerm("内容布局", "nei rong bu ju"),
        )
        val foreign = mapOf(
            1 to SearchTerm("Show battery level", "Show battery level"),
            2 to SearchTerm("Display layout", "Display layout"),
        )
        val all = intArrayOf(1, 2)
        val result = SearchMatches(
            all,
            { native[it]?.matches("dl") == true },
            { false },
            { foreign[it]?.matches("dl") == true },
        )
        assertTrue(result.matches(1))
        assertFalse("Translated English initials must not pollute dl results", result.matches(2))
        assertTrue(foreign.getValue(2).matches("dl"))
    }

    @Test
    fun foreignNamesAreConsideredOnlyWhenNoCurrentLanguageResultExists() {
        val ids = intArrayOf(1, 2, 3)
        val current = mapOf(
            1 to SearchTerm("电量颜色", "dian liang yan se"),
            2 to SearchTerm("内容布局", "nei rong bu ju"),
            3 to SearchTerm("恢复配置", "hui fu pei zhi"),
        )
        val translated = mapOf(
            1 to SearchTerm("Battery colors", "Battery colors"),
            2 to SearchTerm("Content layout", "Content layout"),
            3 to SearchTerm("Restore configuration", "Restore configuration"),
        )
        val byBattery = SearchMatches(
            ids,
            { current[it]?.matches("Battery") == true },
            { false },
            { translated[it]?.matches("Battery") == true },
        )
        assertTrue(byBattery.matches(1))
        assertFalse(byBattery.matches(2))
        assertFalse(byBattery.matches(3))

        val byChinese = SearchMatches(
            ids,
            { current[it]?.matches("电量") == true },
            { false },
            { translated[it]?.matches("电量") == true },
        )
        assertTrue(byChinese.matches(1))
        assertFalse(byChinese.matches(2))
        assertFalse(byChinese.matches(3))

        val byRestore = SearchMatches(
            ids,
            { current[it]?.matches("Restore") == true },
            { false },
            { translated[it]?.matches("Restore") == true },
        )
        assertTrue(byRestore.matches(3))
        assertFalse(byRestore.matches(1))
    }

    @Test
    fun aliasesRemainAvailableWithoutMaskingOtherLanguageTitles() {
        val ids = intArrayOf(1, 2)
        val original = mapOf(
            1 to SearchTerm("Dual-row signal", "Dual-row signal"),
            2 to SearchTerm("Signal dots vertical offset", "Signal dots vertical offset"),
        )
        val aliases = mapOf(
            1 to SearchTerm("双卡信号", "shuang ka xin hao"),
        )
        val chinese = mapOf(
            1 to SearchTerm("双排信号", "shuang pai xin hao"),
            2 to SearchTerm("信号点上下偏移", "xin hao dian shang xia pian yi"),
        )
        val query = "信号"
        val results = SearchMatches(
            ids,
            { original[it]?.matches(query) == true },
            { aliases[it]?.matches(query) == true },
            { chinese[it]?.matches(query) == true },
        )
        assertTrue("Curated alias is still visible", results.matches(1))
        assertTrue("Alias must not hide another translated official title", results.matches(2))
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
