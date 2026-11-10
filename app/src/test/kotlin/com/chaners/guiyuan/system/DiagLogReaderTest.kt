package com.chaners.guiyuan.system

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

private const val EVENT_MODULE_LOADED = "module.loaded"
private const val EVENT_MODULE_RELOADED = "module.reloaded"
private const val EVENT_HOT_RELOAD_COMPLETE = "hotReload.complete"
private const val EVENT_RUNTIME_TEARDOWN = "runtime.teardown"
private const val FIELD_SESSION_ID = "sessionId"
private const val EVENT_DIAGNOSTICS_BIND = "diagnostics.bind"
private const val EVENT_MODULE_PREFIX = "module."
private const val COMPONENT_MODULE = "module"
private const val COMPONENT_HOT_RELOAD = "hotReload"

class DiagLogReaderTest {
    @Test
    fun legacySessionSelectionKeepsLatestNativeStartAndMatchingPid() {
        fun line(time: String, pid: Int, message: String) =
            "11-06 08:49:$time.000 $pid $pid I CombinedStatus: $message"

        val oldStart = line("10", 1100, "Module loaded in com.android.systemui")
        val oldUpdate = line("11", 1100, "battery.reconcile old")
        val currentStart = line("12", 2200, "Module loaded in com.android.systemui")
        val delayedOld = line("13", 1100, "battery.reconcile stale")
        val currentUpdate = line("14", 2200, "battery.reconcile current")

        assertEquals(
            listOf(currentStart, currentUpdate),
            DiagLogReader.latestSession(
                listOf(oldStart, oldUpdate, currentStart, delayedOld, currentUpdate),
            ),
        )
    }

    @Test
    fun clearedHistoryRetainsOnlyNewLsposedLogs() {
        fun line(time: String, event: String) =
            "[ 2026-10-30T23:00:$time 1000: 6303: 6303 I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,1,0,1] " +
                "diag schema=2 event=$event component=diagnostics"

        val before = line("09.900", EVENT_DIAGNOSTICS_BIND)
        val atBoundary = line("10.000", "diagnostics.snapshot")
        val after = line("10.001", "presentation.resolve")
        val logs = listOf(before, atBoundary, after)
        val boundary = LocalDateTime.parse("2026-10-30T23:00:10")

        assertEquals(logs, DiagLogReader.afterHistoryClear(logs, null))
        assertEquals(listOf(after), DiagLogReader.afterHistoryClear(logs, boundary))
    }

    @Test
    fun clearedHistoryRetainsRecentLogcatAfterYearRollover() {
        val older = "12-31 23:59:59.900 6303 6303 I CombinedStatus: " +
            "diag schema=2 event=module.loaded component=module"
        val after = "01-01 00:00:00.100 6303 6303 I CombinedStatus: " +
            "diag schema=2 event=module.reloaded component=module"
        val unknown = "CombinedStatus: no envelope"
        val boundary = LocalDateTime.parse("2026-12-31T23:59:59.950")
        val now = LocalDateTime.parse("2027-01-01T00:01:00")

        assertEquals(
            listOf(after),
            DiagLogReader.afterHistoryClear(listOf(older, after, unknown), boundary, now),
        )
    }

    @Test
    fun clearedHistoryDoesNotExposeUnstampedEarlierLogs() {
        val boundary = LocalDateTime.parse("2026-10-30T23:00:10")
        assertEquals(
            emptyList<String>(),
            DiagLogReader.afterHistoryClear(listOf("CombinedStatus old raw entry"), boundary),
        )
    }

    @Test
    fun lsposedExportPrefersSystemUiOverNewerSettingsRemoteSession() {
        fun event(process: String, pid: Int, second: Int, kind: String, session: String) =
            "[ 2026-11-06T16:35:${second.toString().padStart(2, '0')}.000 " +
                "1000: $pid: $pid I/LSPosedFramework ] " +
                "($process) [com.chaners.guiyuan,CombinedStatus,876,0,1] " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind == EVENT_MODULE_LOADED) COMPONENT_MODULE else "network",
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val nativeStart = event("com.android.systemui", 2200, 30, EVENT_MODULE_LOADED, "native")
        val nativeNetwork = event("com.android.systemui", 2200, 31, "source.install", "native")
        val remoteStart = event("com.android.settings:remote", 3300, 38, EVENT_MODULE_LOADED, "remote")
        val mixed = listOf(nativeStart, nativeNetwork, remoteStart).joinToString("\n")

        val selected = DiagLogReader.filterModuleLines(mixed)
        assertEquals(listOf(nativeStart, nativeNetwork), selected)
        assertEquals(listOf(nativeStart, nativeNetwork), DiagLogReader.latestSession(selected))
        assertEquals(listOf(remoteStart), DiagLogReader.filterModuleLines(remoteStart))
    }

    @Test
    fun logcatFallbackKeepsStructuredModuleTagWithoutPackageName() {
        val structured =
            "10-04 12:38:57.185 17495 17495 I CombinedStatus: " +
                "diag schema=2 event=module.loaded component=module state=ready sessionId=current"
        val unrelatedTag =
            "10-04 12:38:57.185 17495 17495 I OtherTag: " +
                "diag schema=2 event=module.loaded component=module state=ready note=CombinedStatus"
        val unstructured =
            "10-04 12:38:57.185 17495 17495 I CombinedStatus: ordinary message"
        val lspModule =
            "[ 2026-10-04T12:38:57.185 1000: 17495: 17495 I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,817,0,1] " +
                EVENT_MODULE_LOADED

        assertEquals(
            listOf(structured, lspModule),
            DiagLogReader.filterLogcatLines(
                listOf(structured, unrelatedTag, unstructured, lspModule).joinToString("\n"),
            ),
        )
        assertEquals(listOf(lspModule), DiagLogReader.filterModuleLines(lspModule))
    }

}

class DiagLogReaderSessionTest {
    @Test
    fun logcatSessionKeepsOnlyCurrentProcessAfterInterleavedRestart() {
        fun event(pid: Int, second: Int, session: String): String =
            "10-10 21:53:${second.toString().padStart(2, '0')}.000 $pid $pid I CombinedStatus: " +
                DiagProtocol.format(
                    event = EVENT_MODULE_LOADED,
                    component = COMPONENT_MODULE,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val previous = event(1100, 30, "old")
        val current = event(2200, 31, "new")
        val delayedPrevious = event(1100, 32, "old")
        val currentWithoutSession =
            "10-10 21:53:33.000 2200 2200 I CombinedStatus: " +
                DiagProtocol.format(event = "pipeline.ready", component = "network")
        val latest = event(2200, 34, "new")

        assertEquals(
            listOf(current, currentWithoutSession, latest),
            DiagLogReader.latestSession(
                listOf(previous, current, delayedPrevious, currentWithoutSession, latest),
            ),
        )
    }

    @Test
    fun hotReloadKeepsNewestStartedSessionDespiteTrailingOldGenerationEvents() {
        fun event(second: Int, kind: String, session: String): String =
            "[ 2026-10-11T19:15:${second.toString().padStart(2, '0')}.000 " +
                "1000: 2200: 2200 I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,876,0,1] " +
                DiagProtocol.format(
                    event = kind,
                    component =
                        when {
                            kind.startsWith(EVENT_MODULE_PREFIX) -> COMPONENT_MODULE
                            kind == EVENT_RUNTIME_TEARDOWN -> "runtimeSession"
                            else -> COMPONENT_HOT_RELOAD
                        },
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val firstStart = event(30, EVENT_MODULE_LOADED, "old")
        val newStart = event(31, EVENT_MODULE_RELOADED, "new")
        val oldTeardown = event(32, EVENT_RUNTIME_TEARDOWN, "old")
        val newComplete = event(33, EVENT_HOT_RELOAD_COMPLETE, "new")
        val delayedOld = event(34, EVENT_HOT_RELOAD_COMPLETE, "old")

        assertEquals(
            listOf(newStart, newComplete),
            DiagLogReader.latestSession(
                listOf(firstStart, newStart, oldTeardown, newComplete, delayedOld),
            ),
        )
    }

    @Test
    fun logcatHotReloadPreservesUntaggedEventsButDropsTaggedOldSession() {
        fun event(second: Int, kind: String, session: String): String =
            "10-11 19:15:${second.toString().padStart(2, '0')}.000 2200 2200 I CombinedStatus: " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind.startsWith(EVENT_MODULE_PREFIX)) COMPONENT_MODULE else COMPONENT_HOT_RELOAD,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val oldStart = event(30, EVENT_MODULE_LOADED, "old")
        val newStart = event(31, EVENT_MODULE_RELOADED, "new")
        val oldEvent = event(32, EVENT_RUNTIME_TEARDOWN, "old")
        val legacy =
            "10-11 19:15:33.000 2200 2200 I CombinedStatus: " +
                "com.chaners.guiyuan legacy event"
        val newEvent = event(34, EVENT_HOT_RELOAD_COMPLETE, "new")

        assertEquals(
            listOf(newStart, legacy, newEvent),
            DiagLogReader.latestSession(
                DiagLogReader.filterLogcatLines(
                    listOf(oldStart, newStart, oldEvent, legacy, newEvent).joinToString("\n"),
                ),
            ),
        )
    }

    @Test
    fun sessionIdStillScopesWhenPidEnvelopeIsUnavailable() {
        fun event(kind: String, session: String): String =
            DiagProtocol.format(
                event = kind,
                component = COMPONENT_MODULE,
                fields = mapOf(FIELD_SESSION_ID to session),
            )

        val oldStart = event(EVENT_MODULE_LOADED, "old")
        val newStart = event(EVENT_MODULE_RELOADED, "new")
        val lateOld = event(EVENT_RUNTIME_TEARDOWN, "old")
        val untagged = "untagged legacy log line"

        assertEquals(
            listOf(newStart, untagged),
            DiagLogReader.latestSession(listOf(oldStart, newStart, lateOld, untagged)),
        )
    }

    @Test
    fun failedHotReloadBeforeModuleReloadedStillSelectsNewGeneration() {
        fun event(second: Int, kind: String, state: String?, session: String): String =
            "10-11 20:15:${second.toString().padStart(2, '0')}.000 2200 2200 I CombinedStatus: " +
                DiagProtocol.format(
                    event = kind,
                    component =
                        when {
                            kind.startsWith(EVENT_MODULE_PREFIX) -> COMPONENT_MODULE
                            kind == EVENT_DIAGNOSTICS_BIND -> "diagnostics"
                            kind == EVENT_RUNTIME_TEARDOWN -> "runtimeSession"
                            else -> COMPONENT_HOT_RELOAD
                        },
                    state = state,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val oldStart = event(30, EVENT_MODULE_LOADED, "ready", "old")
        val newBind = event(31, EVENT_DIAGNOSTICS_BIND, "ready", "new")
        val failedTakeover = event(32, EVENT_HOT_RELOAD_COMPLETE, "error", "new")
        val delayedOld = event(33, EVENT_RUNTIME_TEARDOWN, "released", "old")

        assertEquals(
            listOf(newBind, failedTakeover),
            DiagLogReader.latestSession(listOf(oldStart, newBind, failedTakeover, delayedOld)),
        )
    }

    @Test
    fun unavailableHotReloadBeforeModuleReloadedSelectsNewGeneration() {
        fun event(second: Int, kind: String, state: String?, session: String): String =
            "10-11 20:17:${second.toString().padStart(2, '0')}.000 2200 2200 I CombinedStatus: " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind.startsWith(EVENT_MODULE_PREFIX)) COMPONENT_MODULE else COMPONENT_HOT_RELOAD,
                    state = state,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val oldStart = event(30, EVENT_MODULE_LOADED, "ready", "old")
        val unavailable = event(31, EVENT_HOT_RELOAD_COMPLETE, "unavailable", "new")
        val delayedOld = event(32, EVENT_RUNTIME_TEARDOWN, "released", "old")

        assertEquals(
            listOf(unavailable),
            DiagLogReader.latestSession(listOf(oldStart, unavailable, delayedOld)),
        )
    }

    @Test
    fun delayedOldUnavailableReloadCannotOverrideNewSuccessfulStart() {
        fun event(second: Int, kind: String, state: String?, session: String): String =
            "10-11 20:18:${second.toString().padStart(2, '0')}.000 2200 2200 I CombinedStatus: " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind.startsWith(EVENT_MODULE_PREFIX)) COMPONENT_MODULE else COMPONENT_HOT_RELOAD,
                    state = state,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val oldStart = event(30, EVENT_MODULE_LOADED, "ready", "old")
        val oldUnavailable = event(31, EVENT_HOT_RELOAD_COMPLETE, "unavailable", "old")
        val newStart = event(32, EVENT_MODULE_RELOADED, "ready", "new")
        val delayedOldUnavailable = event(33, EVENT_HOT_RELOAD_COMPLETE, "unavailable", "old")
        val newComplete = event(34, EVENT_HOT_RELOAD_COMPLETE, "ready", "new")

        assertEquals(
            listOf(newStart, newComplete),
            DiagLogReader.latestSession(
                listOf(oldStart, oldUnavailable, newStart, delayedOldUnavailable, newComplete),
            ),
        )
    }

    @Test
    fun lateOldGenerationFailureCannotOverrideNewSuccessfulStart() {
        fun event(second: Int, kind: String, state: String?, session: String): String =
            "10-11 20:16:${second.toString().padStart(2, '0')}.000 2200 2200 I CombinedStatus: " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind.startsWith(EVENT_MODULE_PREFIX)) COMPONENT_MODULE else COMPONENT_HOT_RELOAD,
                    state = state,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val oldStart = event(30, EVENT_MODULE_LOADED, "ready", "old")
        val oldError = event(31, EVENT_HOT_RELOAD_COMPLETE, "error", "old")
        val newStart = event(32, EVENT_MODULE_RELOADED, "ready", "new")
        val delayedOldError = event(33, EVENT_HOT_RELOAD_COMPLETE, "error", "old")
        val newComplete = event(34, EVENT_HOT_RELOAD_COMPLETE, "ready", "new")

        assertEquals(
            listOf(newStart, newComplete),
            DiagLogReader.latestSession(
                listOf(oldStart, oldError, newStart, delayedOldError, newComplete),
            ),
        )
    }

    @Test
    fun lsposedSessionStillKeepsOnlyMatchingProcess() {
        fun event(pid: Int, second: Int, session: String): String =
            "[ 2026-10-10T21:53:${second.toString().padStart(2, '0')}.000 " +
                "1000: $pid: $pid I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,876,0,1] " +
                DiagProtocol.format(
                    event = EVENT_MODULE_LOADED,
                    component = COMPONENT_MODULE,
                    fields = mapOf(FIELD_SESSION_ID to session),
                )

        val old = event(1100, 30, "old")
        val current = event(2200, 31, "new")
        val delayedOld = event(1100, 32, "old")
        val latest = event(2200, 33, "new")

        assertEquals(
            listOf(current, latest),
            DiagLogReader.latestSession(listOf(old, current, delayedOld, latest)),
        )
    }
}
