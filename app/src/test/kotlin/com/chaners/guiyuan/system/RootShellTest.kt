package com.chaners.guiyuan.system

import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RootShellTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun childHoldingStandardOutputCannotBlockCompletedCommand() = runBlocking {
        val dir = temp.newFolder()
        val result =
            withTimeout(2_500) {
                RootShell.execute(
                    ProcessBuilder("sh", "-c", "printf 'ready\\n'; sleep 4 &"),
                    timeoutSeconds = 1,
                    cacheDir = dir,
                )
            }

        assertTrue(result.isSuccess)
        assertEquals("ready\n", result.output)
        assertTrue(dir.listFiles().isNullOrEmpty())
    }

    @Test
    fun timeoutCapturesPartialOutputAndRemovesFile() = runBlocking {
        val dir = temp.newFolder()
        val result =
            withTimeout(5_000) {
                RootShell.execute(
                    ProcessBuilder("sh", "-c", "printf 'started\\n'; sleep 8"),
                    timeoutSeconds = 1,
                    cacheDir = dir,
                )
            }

        assertTrue(result.timedOut)
        assertFalse(result.isSuccess)
        assertTrue(result.output.contains("started"))
        assertTrue(dir.listFiles().isNullOrEmpty())
    }

    @Test
    fun cancellingCommandStopsItsProcessAndRemovesFile() = runBlocking {
        val dir = temp.newFolder()
        val pidFile = File(dir, "pid")
        val job =
            async {
                RootShell.execute(
                    ProcessBuilder(
                        "sh",
                        "-c",
                        "printf '%s' \$\$ > '${pidFile.absolutePath}'; exec sleep 12",
                    ),
                    timeoutSeconds = 12,
                    cacheDir = dir,
                )
            }

        val pid =
            withTimeout(3_000) {
                while (!pidFile.isFile) delay(20)
                pidFile.readText().trim().toLong()
            }
        withTimeout(3_000) { job.cancelAndJoin() }

        assertFalse(ProcessHandle.of(pid).map { it.isAlive }.orElse(false))
        assertTrue(dir.listFiles()?.none { it.name.startsWith("guiyuan-root-") } == true)
    }
}
