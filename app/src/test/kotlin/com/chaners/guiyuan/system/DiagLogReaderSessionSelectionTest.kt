package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagLogReaderSessionSelectionTest {
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
    fun lsposedExportPrefersSystemUiOverNewerSettingsRemoteSession() {
        fun event(process: String, pid: Int, second: Int, kind: String, session: String) =
            "[ 2026-11-06T16:35:${second.toString().padStart(2, '0')}.000 " +
                "1000: $pid: $pid I/LSPosedFramework ] " +
                "($process) [com.chaners.guiyuan,CombinedStatus,876,0,1] " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind == "module.loaded") "module" else "network",
                    fields = mapOf("sessionId" to session),
                )

        val nativeStart = event("com.android.systemui", 2200, 30, "module.loaded", "native")
        val nativeNetwork = event("com.android.systemui", 2200, 31, "source.install", "native")
        val remoteStart = event("com.android.settings:remote", 3300, 38, "module.loaded", "remote")
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
                "module.loaded"

        assertEquals(
            listOf(structured, lspModule),
            DiagLogReader.filterLogcatLines(
                listOf(structured, unrelatedTag, unstructured, lspModule).joinToString("\n"),
            ),
        )
        assertEquals(listOf(lspModule), DiagLogReader.filterModuleLines(lspModule))
    }

    @Test
    fun logcatSessionKeepsOnlyCurrentProcessAfterInterleavedRestart() {
        fun event(pid: Int, second: Int, session: String): String =
            "10-10 21:53:${second.toString().padStart(2, '0')}.000 $pid $pid I CombinedStatus: " +
                DiagProtocol.format(
                    event = "module.loaded",
                    component = "module",
                    fields = mapOf("sessionId" to session),
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
    fun lsposedSessionStillKeepsOnlyMatchingProcess() {
        fun event(pid: Int, second: Int, session: String): String =
            "[ 2026-10-10T21:53:${second.toString().padStart(2, '0')}.000 " +
                "1000: $pid: $pid I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,876,0,1] " +
                DiagProtocol.format(
                    event = "module.loaded",
                    component = "module",
                    fields = mapOf("sessionId" to session),
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
