package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import com.chaners.guiyuan.R

internal object FeatureSearchIds {
    val display = intArrayOf(
        R.string.keyguard_combined_status_title,
        R.string.aod_combined_status_title,
        R.string.control_center_tint_transition,
        R.string.battery_fill_follow_retract,
    )
    val layout = intArrayOf(
        R.string.content_layout_title,
        R.string.dual_signal_title,
        R.string.single_row_signal_dot_arrangement_title,
        R.string.dual_row_signal_dot_arrangement_title,
        R.string.both_sides_activity_title,
        R.string.both_sides_idle_activity_title,
        R.string.both_sides_charging_title,
        R.string.both_sides_activity_size,
        R.string.both_sides_activity_vertical_offset,
        R.string.both_sides_charging_vertical_offset,
        R.string.mobile_type_bottom_offset,
        R.string.curved_signal_dot_vertical_offset,
        R.string.horizontal_signal_dot_vertical_offset,
        R.string.battery_lower_opening_title,
        R.string.combined_size,
        R.string.outer_weight,
        R.string.wifi_vertical_offset,
        R.string.mobile_type_vertical_offset,
        R.string.top_info_vertical_offset,
    )
    val network = intArrayOf(
        R.string.wifi_size,
        R.string.wifi_weight,
        R.string.airplane_size,
        R.string.airplane_weight,
        R.string.no_sim_size,
        R.string.mobile_type_size,
        R.string.mobile_type_weight,
        R.string.mobile_follow_battery_color,
        R.string.center_follow_battery_color,
    )
    val battery = intArrayOf(
        R.string.battery_colors,
        R.string.low_battery_threshold,
        R.string.battery_top_readout,
        R.string.battery_top_text_size,
        R.string.battery_top_text_weight,
        R.string.battery_text_follow_battery_color,
        R.string.battery_charging_icon,
        R.string.battery_top_charging_icon_size,
        R.string.charging_icon_follow_battery_color,
    )
    val management = intArrayOf(
        R.string.config_backup_title,
        R.string.config_restore_title,
        R.string.restore_feature_defaults,
    )
    val all = display + layout + network + battery + management
}

private val cellularTerms = listOf(
    "移动网络", "移动数据", "手机流量", "流量", "蜂窝", "蜂窝网络",
    "蜂窝数据", "蜂窝移动", "移动通信", "4G", "LTE", "5G", "5GA",
)

internal fun searchAliases(@StringRes title: Int): List<String> = when (title) {
    R.string.keyguard_combined_status_title -> listOf("锁屏状态栏", "锁屏图标")
    R.string.aod_combined_status_title -> listOf("息屏显示", "熄屏显示", "常亮显示", "AOD")
    R.string.control_center_tint_transition -> listOf("控制中心变色", "下拉颜色过渡")
    R.string.battery_fill_follow_retract -> listOf("电池段收口", "电量环回收", "电量段动画")
    R.string.content_layout_title -> listOf(
        "排列", "网络居中", "网络信息居中", "电量居中",
        "移动信号居中", "信号强度居中", "信号点居中", "网络制式下方", "三种布局",
    )
    R.string.dual_signal_title -> listOf("双卡", "双 SIM", "双卡信号点", "双排信号", "双排信号点", "单卡显示")
    R.string.single_row_signal_dot_arrangement_title -> listOf(
        "单排", "单排信号", "单排信号点", "单卡", "单卡信号点", "单SIM", "单排样式", "弧形排列", "横向排列",
        "Single row signal", "Single-row dots", "Single-SIM dots", "Curved arrangement", "Horizontal arrangement",
    )
    R.string.dual_row_signal_dot_arrangement_title -> listOf(
        "双排", "双排信号", "双排信号点", "双卡", "双卡信号点", "双卡样式", "Duo", "八点", "双排样式", "弧形排列", "横向排列",
        "Dual-row dots", "Dual-SIM dots", "Curved arrangement", "Horizontal arrangement",
    )
    R.string.curved_signal_dot_vertical_offset -> listOf("弧形偏移", "弧形上下位置", "Curved dot offset")
    R.string.horizontal_signal_dot_vertical_offset -> listOf("横向偏移", "横排上下位置", "Horizontal dot offset")
    R.string.both_sides_activity_title -> listOf("上传下载", "网络活动", "流量指示器", "activity indicator")
    R.string.both_sides_idle_activity_title -> listOf("空闲箭头", "保留活动箭头", "隐藏活动箭头", "空闲隐藏", "隐藏空闲网络指示器", "空闲网络活动指示器", "Hide network activity when idle")
    R.string.both_sides_activity_size -> listOf("活动箭头大小", "流量图标大小")
    R.string.both_sides_activity_vertical_offset -> listOf("活动箭头位置", "网络活动上下偏移")
    R.string.both_sides_charging_title -> listOf("底部闪电", "充电图标")
    R.string.both_sides_charging_vertical_offset -> listOf("底部闪电位置", "充电闪电上下偏移")
    R.string.battery_lower_opening_title -> listOf("开口", "电量环开口", "弧形开口", "横排开口", "固定开口", "跟随位置", "避让", "Lower opening", "Follow position", "Fixed opening")
    R.string.combined_size -> listOf("整体缩放", "总大小", "三合一大小", "三合一缩放")
    R.string.outer_weight -> listOf("环形粗细", "圆环粗细", "外环宽度", "电量环粗细")
    R.string.wifi_vertical_offset -> listOf("WiFi 位置", "WiFi 垂直位置", "无线位置")
    R.string.mobile_type_vertical_offset -> cellularTerms + listOf("5G 位置", "制式位置", "移动网络位置")
    R.string.top_info_vertical_offset -> listOf("顶部位置", "信息位置")
    R.string.wifi_size -> listOf("wifi", "wlan", "无线", "wifi缩放", "无线图标尺寸")
    R.string.wifi_weight -> listOf("wifi", "wlan", "无线", "无线图标粗细", "wifi线条")
    R.string.airplane_size -> listOf("飞机图标大小", "飞行图标缩放")
    R.string.airplane_weight -> listOf("飞机图标粗细", "飞行图标粗细", "线条粗细")
    R.string.no_sim_size -> listOf("无卡图标", "无卡大小", "SIM")
    R.string.mobile_type_size -> cellularTerms + listOf("网络制式", "5G 大小", "5GA 大小", "网络文字大小", "蜂窝制式大小")
    R.string.mobile_type_weight -> cellularTerms + listOf("网络制式", "5G 字重", "5GA 字重", "5G 粗细", "字体粗细", "数字粗细", "蜂窝制式粗细")
    R.string.mobile_follow_battery_color -> cellularTerms + listOf("移动信号颜色", "蜂窝信号颜色", "信号点颜色", "蜂窝信号", "流量信号")
    R.string.center_follow_battery_color -> listOf("网络图标颜色", "WiFi 颜色", "5G 颜色", "蜂窝制式颜色", "飞行模式颜色")
    R.string.battery_colors -> listOf("电池颜色", "电量环配色", "电池配色", "配色方案", "电量配色")
    R.string.low_battery_threshold -> listOf("低电量警告", "电池阈值", "低电量门限", "低电量提醒")
    R.string.battery_top_readout -> listOf("电量数值", "电池数字", "电量百分比", "显示百分比", "电量显示")
    R.string.battery_top_text_size -> listOf("电量数字大小", "电池数字大小")
    R.string.battery_top_text_weight -> listOf("电量数字粗细", "电池数字粗细", "电量数字字重", "字体粗细", "数字粗细")
    R.string.battery_text_follow_battery_color -> listOf("电量数字颜色", "数字跟随电池颜色")
    R.string.battery_charging_icon -> listOf("充电闪电", "充电标志", "充电图标显示")
    R.string.battery_top_charging_icon_size -> listOf("充电图标大小", "闪电大小")
    R.string.charging_icon_follow_battery_color -> listOf("闪电颜色", "充电图标颜色")
    R.string.config_backup_title -> listOf("导出配置", "保存设置", "备份设置", "配置导出", "保存配置", "Export configuration", "Export backup")
    R.string.config_restore_title -> listOf("导入配置", "读取备份", "恢复备份", "还原配置", "配置还原", "还原备份", "配置导入", "导入备份", "Import configuration", "Import backup")
    R.string.restore_feature_defaults -> listOf("重置设置", "重置功能", "恢复功能默认值", "还原默认设置", "还原设置", "重置配置", "Reset defaults", "Reset settings")
    else -> emptyList()
}

