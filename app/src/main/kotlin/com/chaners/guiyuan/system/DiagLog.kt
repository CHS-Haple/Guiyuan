package com.chaners.guiyuan.system

internal enum class LogLevel {
    Verbose,
    Debug,
    Info,
    Warning,
    Error,
    Fatal,
    Unknown,
}

internal enum class LogCategory {
    Module,
    Network,
    Display,
    Native,
    Transition,
    Performance,
    Settings,
    Other,
}

internal data class LogEntry(
    val raw: String,
    val timestamp: String?,
    val time: String?,
    val level: LogLevel,
    val uid: String?,
    val pid: String?,
    val tid: String?,
    val framework: String?,
    val hostPkg: String?,
    val modulePkg: String?,
    val tag: String?,
    val message: String,
    val event: String?,
    val component: String?,
    val state: String?,
    val fields: Map<String, String>,
    val category: LogCategory,
    val structured: Boolean,
) {
    val key: String
        get() =
            buildString {
                append(timestamp.orEmpty())
                append(':')
                append(fields["sequence"].orEmpty())
                append(':')
                append(raw.hashCode())
            }

    fun searchText(): String =
        buildString {
            append(raw)
            append(' ')
            append(event.orEmpty())
            append(' ')
            append(component.orEmpty())
            append(' ')
            append(state.orEmpty())
            fields.forEach { (key, value) ->
                append(' ')
                append(key)
                append(' ')
                append(value)
            }
        }
}

internal object DiagLogParser {
    fun parse(line: String): LogEntry {
        val envelope = parseEnvelope(line)
        val structured =
            RuntimeDiagnosticsProtocol.parse(envelope.message)
                ?: RuntimeDiagnosticsProtocol.parse(line)
        val legacy =
            if (structured == null) {
                parseLegacy(envelope.message)
            } else {
                null
            }

        val event = structured?.event ?: legacy?.event
        val component = structured?.component
        val state = structured?.state
        val fields = structured?.fields ?: legacy?.fields.orEmpty()

        return LogEntry(
            raw = line,
            timestamp = envelope.timestamp,
            time = displayTime(envelope.timestamp),
            level = envelope.level,
            uid = envelope.uid,
            pid = envelope.pid,
            tid = envelope.tid,
            framework = envelope.framework,
            hostPkg = envelope.hostPkg,
            modulePkg = envelope.modulePkg,
            tag = envelope.tag,
            message = envelope.message,
            event = event,
            component = component,
            state = state,
            fields = fields,
            category = classify(event = event, component = component),
            structured = structured != null,
        )
    }

    private fun parseEnvelope(line: String): Envelope {
        lspEnvelopeRe.matchEntire(line)?.let { match ->
            val moduleParts =
                match.groupValues[8]
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
            return Envelope(
                timestamp = match.groupValues[1],
                uid = match.groupValues[2],
                pid = match.groupValues[3],
                tid = match.groupValues[4],
                level = levelOf(match.groupValues[5]),
                framework = match.groupValues[6].ifBlank { null },
                hostPkg = match.groupValues[7].ifBlank { null },
                modulePkg = moduleParts.getOrNull(0),
                tag = moduleParts.getOrNull(1),
                message = match.groupValues[9].trim(),
            )
        }

        logcatEnvelopeRe.matchEntire(line)?.let { match ->
            return Envelope(
                timestamp = match.groupValues[1],
                uid = null,
                pid = match.groupValues[2],
                tid = match.groupValues[3],
                level = levelOf(match.groupValues[4]),
                framework = "logcat",
                hostPkg = null,
                modulePkg = null,
                tag = match.groupValues[5].trim(),
                message = match.groupValues[6].trim(),
            )
        }

        return Envelope(
            timestamp = null,
            uid = null,
            pid = null,
            tid = null,
            level = LogLevel.Unknown,
            framework = null,
            hostPkg = null,
            modulePkg = null,
            tag = null,
            message = line.trim(),
        )
    }

    private fun parseLegacy(message: String): LegacyMessage {
        val matches = legacyFieldRe.findAll(message).toList()
        val firstFieldStart = matches.firstOrNull()?.range?.first ?: message.length
        val prefix =
            message
                .substring(0, firstFieldStart)
                .trim()
                .split(Regex("\\s+"))
                .filter(String::isNotBlank)
                .joinToString(".")
                .ifBlank { "legacy" }
        val fields =
            linkedMapOf<String, String>().apply {
                matches.forEach { match ->
                    put(match.groupValues[1], match.groupValues[2])
                }
            }
        return LegacyMessage(event = prefix, fields = fields)
    }

    private fun classify(
        event: String?,
        component: String?,
    ): LogCategory {
        val token = (component.orEmpty() + " " + event.orEmpty()).lowercase()
        return when {
            token.contains("latency") || token.contains("performance") ->
                LogCategory.Performance
            token.contains("network") ||
                token.contains("connectivity") ||
                token.contains("wifi") ||
                token.contains("mobile") ||
                token.contains("subscription") ->
                LogCategory.Network
            token.contains("native") ||
                token.contains("suppression") ||
                token.contains("failnative") ->
                LogCategory.Native
            token.contains("island") ||
                token.contains("paneltransition") ||
                token.contains("projection") ||
                token.contains("handoff") ||
                token.contains("controlcenter") ->
                LogCategory.Transition
            token.contains("renderer") ||
                token.contains("presentation") ||
                token.contains("keyguard") ||
                token.contains("aod") ||
                token.contains("statusicons") ||
                token.contains("tint") ->
                LogCategory.Display
            token.contains("diagnostics") ||
                token.contains("settings") ||
                token.contains("hotreload") ->
                LogCategory.Settings
            token.contains("module") ||
                token.contains("compatibility") ||
                token.contains("hook") ||
                token.contains("runtime") ||
                token.contains("host") ->
                LogCategory.Module
            else -> LogCategory.Other
        }
    }

    private fun displayTime(timestamp: String?): String? {
        if (timestamp.isNullOrBlank()) return null
        val value = timestamp.trim()
        val date =
            if ('T' in value) {
                value.substringBefore('T').takeLast(5)
            } else {
                value.substringBefore(' ').takeLast(5)
            }
        val clock =
            if ('T' in value) {
                value.substringAfter('T').take(8)
            } else {
                value.substringAfter(' ', "").take(8)
            }
        return if (date.length == 5 && clock.length == 8) {
            "$date $clock"
        } else {
            null
        }
    }

    private fun levelOf(token: String): LogLevel =
        when (token.uppercase()) {
            "V" -> LogLevel.Verbose
            "D" -> LogLevel.Debug
            "I" -> LogLevel.Info
            "W" -> LogLevel.Warning
            "E" -> LogLevel.Error
            "F", "A" -> LogLevel.Fatal
            else -> LogLevel.Unknown
        }

    private data class Envelope(
        val timestamp: String?,
        val uid: String?,
        val pid: String?,
        val tid: String?,
        val level: LogLevel,
        val framework: String?,
        val hostPkg: String?,
        val modulePkg: String?,
        val tag: String?,
        val message: String,
    )

    private data class LegacyMessage(
        val event: String,
        val fields: Map<String, String>,
    )

    private val lspEnvelopeRe =
        Regex(
            """^\[\s*(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+)\s+""" +
                """(\d+):\s*(\d+):\s*(\d+)\s+([VDIWEAF])/([^\]]+)\s*\]\s*""" +
                """(?:\(([^)]+)\))?\s*(?:\[([^\]]+)\])?\s*(.*)$""",
        )

    private val logcatEnvelopeRe =
        Regex(
            """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)\s+""" +
                """(\d+)\s+(\d+)\s+([VDIWEAF])\s+([^:]+):\s*(.*)$""",
        )

    private val legacyFieldRe =
        Regex("""(?<!\S)([A-Za-z][A-Za-z0-9_.-]*)=([^\s]+)""")
}
