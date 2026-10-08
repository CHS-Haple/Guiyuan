package com.chaners.guiyuan.system

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal object RootShell {
    suspend fun execute(
        command: String,
        timeoutSeconds: Long,
    ): Result = withContext(Dispatchers.IO) {
        coroutineScope {
            var process: Process? = null
            try {
                val running = ProcessBuilder(
                    "su",
                    "-c",
                    command,
                )
                    .redirectErrorStream(true)
                    .start()
                process = running

                val output = async(Dispatchers.IO) {
                    running.inputStream.bufferedReader().use { it.readText() }
                }
                val finished = async(Dispatchers.IO) {
                    running.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                }

                if (!finished.await()) {
                    running.destroy()
                    if (!running.waitFor(1, TimeUnit.SECONDS)) {
                        running.destroyForcibly()
                    }
                    val captured =
                        try {
                            output.await()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            ""
                        }
                    return@coroutineScope Result(
                        exitCode = null,
                        output = captured,
                        timedOut = true,
                        error = null,
                    )
                }

                Result(
                    exitCode = running.exitValue(),
                    output = output.await(),
                    timedOut = false,
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result(
                    exitCode = null,
                    output = "",
                    timedOut = false,
                    error = error.javaClass.simpleName,
                )
            } finally {
                // Release the root process when diagnostics/restart fails or is cancelled.
                process?.let { running ->
                    if (running.isAlive) running.destroyForcibly()
                }
            }
        }
    }

    internal data class Result(
        val exitCode: Int?,
        val output: String,
        val timedOut: Boolean,
        val error: String?,
    ) {
        val isSuccess: Boolean
            get() = exitCode == 0 && !timedOut && error == null
    }
}
