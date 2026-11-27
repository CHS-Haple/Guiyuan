package com.chaners.guiyuan.system

import kotlinx.coroutines.CancellationException

/** A user-requested IO failure can be reported; cancellation and JVM fatal errors cannot. */
internal fun rethrowUserOperationFatal(failure: Throwable) {
    if (failure is CancellationException || failure is Error) throw failure
}
