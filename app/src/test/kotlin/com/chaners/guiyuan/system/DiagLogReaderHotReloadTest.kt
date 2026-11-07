package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagLogReaderHotReloadTest {
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
                            kind.startsWith("module.") -> "module"
                            kind == "runtime.teardown" -> "runtimeSession"
                            else -> "hotReload"
                        },
                    fields = mapOf("sessionId" to session),
                )

        val firstStart = event(30, "module.loaded", "old")
        val newStart = event(31, "module.reloaded", "new")
        val oldTeardown = event(32, "runtime.teardown", "old")
        val newComplete = event(33, "hotReload.complete", "new")
        val delayedOld = event(34, "hotReload.complete", "old")

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
                    component = if (kind.startsWith("module.")) "module" else "hotReload",
                    fields = mapOf("sessionId" to session),
                )

        val oldStart = event(30, "module.loaded", "old")
        val newStart = event(31, "module.reloaded", "new")
        val oldEvent = event(32, "runtime.teardown", "old")
        val legacy =
            "10-11 19:15:33.000 2200 2200 I CombinedStatus: " +
                "com.chaners.guiyuan legacy event"
        val newEvent = event(34, "hotReload.complete", "new")

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
                component = "module",
                fields = mapOf("sessionId" to session),
            )

        val oldStart = event("module.loaded", "old")
        val newStart = event("module.reloaded", "new")
        val lateOld = event("runtime.teardown", "old")
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
                            kind.startsWith("module.") -> "module"
                            kind == "diagnostics.bind" -> "diagnostics"
                            kind == "runtime.teardown" -> "runtimeSession"
                            else -> "hotReload"
                        },
                    state = state,
                    fields = mapOf("sessionId" to session),
                )

        val oldStart = event(30, "module.loaded", "ready", "old")
        val newBind = event(31, "diagnostics.bind", "ready", "new")
        val failedTakeover = event(32, "hotReload.complete", "error", "new")
        val delayedOld = event(33, "runtime.teardown", "released", "old")

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
                    component = if (kind.startsWith("module.")) "module" else "hotReload",
                    state = state,
                    fields = mapOf("sessionId" to session),
                )

        val oldStart = event(30, "module.loaded", "ready", "old")
        val unavailable = event(31, "hotReload.complete", "unavailable", "new")
        val delayedOld = event(32, "runtime.teardown", "released", "old")

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
                    component = if (kind.startsWith("module.")) "module" else "hotReload",
                    state = state,
                    fields = mapOf("sessionId" to session),
                )

        val oldStart = event(30, "module.loaded", "ready", "old")
        val oldUnavailable = event(31, "hotReload.complete", "unavailable", "old")
        val newStart = event(32, "module.reloaded", "ready", "new")
        val delayedOldUnavailable = event(33, "hotReload.complete", "unavailable", "old")
        val newComplete = event(34, "hotReload.complete", "ready", "new")

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
                    component = if (kind.startsWith("module.")) "module" else "hotReload",
                    state = state,
                    fields = mapOf("sessionId" to session),
                )

        val oldStart = event(30, "module.loaded", "ready", "old")
        val oldError = event(31, "hotReload.complete", "error", "old")
        val newStart = event(32, "module.reloaded", "ready", "new")
        val delayedOldError = event(33, "hotReload.complete", "error", "old")
        val newComplete = event(34, "hotReload.complete", "ready", "new")

        assertEquals(
            listOf(newStart, newComplete),
            DiagLogReader.latestSession(
                listOf(oldStart, oldError, newStart, delayedOldError, newComplete),
            ),
        )
    }
}
