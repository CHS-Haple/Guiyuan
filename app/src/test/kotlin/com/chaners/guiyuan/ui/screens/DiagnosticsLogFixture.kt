package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.system.DiagLogParser
import com.chaners.guiyuan.system.DiagProtocol

private const val LOGCAT_TEST_PREFIX = "10-29 09:20:12.456 1200 1200 "

internal fun diagTestStructuredEvent(event: String, fields: Map<String, String> = emptyMap()) =
    diagTestLogcat(DiagProtocol.format(event, "diagnostics", fields = fields))

internal fun diagTestLogcat(message: String, level: String = "I") =
    DiagLogParser.parse("${LOGCAT_TEST_PREFIX}$level CombinedStatus: $message")
