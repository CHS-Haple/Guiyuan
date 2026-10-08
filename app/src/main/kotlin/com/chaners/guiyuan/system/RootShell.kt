package com.chaners.guiyuan.system

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal object RootShell {
    suspend fun execute(
        context: Context,
        command: String,
        timeoutSeconds: Long,
    ): Result =
        execute(
            builder = ProcessBuilder("su", "-c", command),
            timeoutSeconds = timeoutSeconds,
            cacheDir = context.cacheDir,
        )

    internal suspend fun execute(
        builder: ProcessBuilder,
        timeoutSeconds: Long,
        cacheDir: File,
    ): Result = withContext(Dispatchers.IO) {
        coroutineScope {
            var process: Process? = null
            var capture: File? = null
            try {
                // A redirected file cannot be held open as a pipe by a shell descendant.
                val output = File.createTempFile("guiyuan-root-", ".log", cacheDir)
                capture = output
                val running =
                    builder
                        .redirectErrorStream(true)
                        .redirectOutput(output)
                        .start()
                process = running

                val finished = async(Dispatchers.IO) {
                    running.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                }
                if (!finished.await()) {
                    running.destroy()
                    if (!running.waitFor(1, TimeUnit.SECONDS)) {
                        running.destroyForcibly()
                    }
                    return@coroutineScope Result(
                        exitCode = null,
                        output = output.readText(),
                        timedOut = true,
                        error = null,
                    )
                }

                Result(
                    exitCode = running.exitValue(),
                    output = output.readText(),
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
                process?.let { running ->
                    if (running.isAlive) running.destroyForcibly()
                }
                capture?.delete()
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
