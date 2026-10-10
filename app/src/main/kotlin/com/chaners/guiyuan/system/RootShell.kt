package com.chaners.guiyuan.system

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal object RootShell {
    private const val MAX_OUTPUT_BYTES = 2 * 1024 * 1024

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
                    val captured = readOutput(output)
                    return@coroutineScope Result(
                        exitCode = null,
                        output = captured.text,
                        timedOut = true,
                        error = null,
                        truncated = captured.truncated,
                    )
                }

                val captured = readOutput(output)
                Result(
                    exitCode = running.exitValue(),
                    output = captured.text,
                    timedOut = false,
                    error = null,
                    truncated = captured.truncated,
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

    private data class Capture(val text: String, val truncated: Boolean)

    private fun readOutput(file: File): Capture =
        RandomAccessFile(file, "r").use { reader ->
            val size = reader.length()
            val start = (size - MAX_OUTPUT_BYTES).coerceAtLeast(0L)
            val truncated = start > 0L
            val startsOnLine =
                !truncated ||
                    run {
                        reader.seek(start - 1)
                        reader.read() == '\n'.code
                    }

            reader.seek(start)
            val bytes = ByteArray((size - start).toInt())
            reader.readFully(bytes)
            val text = bytes.toString(Charsets.UTF_8)

            Capture(
                text = if (startsOnLine) text else text.substringAfter('\n', ""),
                truncated = truncated,
            )
        }

    internal data class Result(
        val exitCode: Int?,
        val output: String,
        val timedOut: Boolean,
        val error: String?,
        val truncated: Boolean = false,
    ) {
        val isSuccess: Boolean
            get() = exitCode == 0 && !timedOut && error == null
    }
}
