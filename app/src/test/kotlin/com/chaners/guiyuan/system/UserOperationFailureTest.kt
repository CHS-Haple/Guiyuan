package com.chaners.guiyuan.system

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Test

class UserOperationFailureTest {
    @Test
    fun cancellationIsNotConvertedToOrdinaryIoFailure() {
        val cancelled = CancellationException("scope disposed")
        val thrown = runCatching { rethrowUserOperationFatal(cancelled) }.exceptionOrNull()
        assertSame(cancelled, thrown)
    }

    @Test
    fun linkageErrorStillEscapesTheIoBoundary() {
        val fatal = LinkageError("runtime error")
        val thrown = runCatching { rethrowUserOperationFatal(fatal) }.exceptionOrNull()
        assertSame(fatal, thrown)
    }

    @Test
    fun recoverableExceptionIsHandledByTheCaller() {
        rethrowUserOperationFatal(java.io.IOException("document provider unavailable"))
    }
}
