package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTermAliasesTest {
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
    fun simSignalArrangementSearchRetainsOldAndNewWords() {
        val single = searchAliases(R.string.single_row_signal_dot_arrangement_title)
        val dual = searchAliases(R.string.dual_row_signal_dot_arrangement_title)
        for (word in listOf("单排", "单卡", "单排信号点", "单卡信号点")) {
            assertTrue("Single-SIM alias missing: $word", single.any { it.contains(word) })
        }
        for (word in listOf("双排", "双卡", "双排信号点", "双卡信号点")) {
            assertTrue("Dual-SIM alias missing: $word", dual.any { it.contains(word) })
        }
        assertFalse(single.any { it == "双排" || it == "双卡" })
        assertFalse(dual.any { it == "单排" || it == "单卡" })
    }

    @Test
    fun highConfidenceAliasesStayTargeted() {
        val pairs = listOf(
            R.string.aod_combined_status_title to "AOD",
            R.string.dual_signal_title to "双排信号点",
            R.string.single_row_signal_dot_arrangement_title to "单排信号",
            R.string.dual_row_signal_dot_arrangement_title to "双排样式",
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
