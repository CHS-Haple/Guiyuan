package com.chaners.guiyuan.ui.screens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CancellableUiResultTest {
    @Test
    fun successPreservesValue() {
        val result = runBlocking { runCatchingUiOperation { 42 } }
        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun ordinaryIofailureRemainsAFeedbackResult() {
        val failure = java.io.IOException("document denied")
        val result = runBlocking { runCatchingUiOperation<Unit> { throw failure } }
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingFailedExport() {
        val cancelled = CancellationException("screen disposed")
        val thrown = runCatching {
            runBlocking { runCatchingUiOperation<Unit> { throw cancelled } }
        }.exceptionOrNull()
        assertSame(cancelled, thrown)
    }

    @Test
    fun jvmFatalErrorIsNotShownAsUserOperationFailure() {
        val fatal = LinkageError("incompatible runtime")
        val thrown = runCatching {
            runBlocking { runCatchingUiOperation<Unit> { throw fatal } }
        }.exceptionOrNull()
        assertSame(fatal, thrown)
    }
}
