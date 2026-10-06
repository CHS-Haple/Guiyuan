package com.chaners.guiyuan.xposed

internal data class RuntimeRenderTrace(
    val id: Long,
    val source: String,
    val sourceNanos: Long,
    val stateCommittedNanos: Long? = null,
    val presentationCommittedNanos: Long? = null,
) {
    fun withStateCommitted(atNanos: Long): RuntimeRenderTrace =
        if (stateCommittedNanos != null) {
            this
        } else {
            copy(stateCommittedNanos = atNanos)
        }

    fun withPresentationCommitted(atNanos: Long): RuntimeRenderTrace =
        if (presentationCommittedNanos != null) {
            this
        } else {
            copy(presentationCommittedNanos = atNanos)
        }
}

internal data class RenderLatencySample(
    val traceId: Long,
    val source: String,
    val sourceToStateUs: Long?,
    val sourceToPresentationUs: Long?,
    val stateToPresentationUs: Long?,
    val stateToModelUs: Long?,
    val presentationToModelUs: Long?,
    val modelToDrawUs: Long,
    val sourceToDrawUs: Long,
    val committedOnMainThread: Boolean,
) {
    companion object {
        fun from(
            trace: RuntimeRenderTrace,
            modelCommittedNanos: Long,
            drawNanos: Long,
            committedOnMainThread: Boolean,
        ): RenderLatencySample =
            RenderLatencySample(
                traceId = trace.id,
                source = trace.source,
                sourceToStateUs =
                    trace.stateCommittedNanos?.let { stateNanos ->
                        durationUs(trace.sourceNanos, stateNanos)
                    },
                sourceToPresentationUs =
                    trace.presentationCommittedNanos?.let { presentationNanos ->
                        durationUs(trace.sourceNanos, presentationNanos)
                    },
                stateToPresentationUs =
                    trace.stateCommittedNanos?.let { stateNanos ->
                        trace.presentationCommittedNanos?.let { presentationNanos ->
                            durationUs(stateNanos, presentationNanos)
                        }
                    },
                stateToModelUs =
                    trace.stateCommittedNanos?.let { stateNanos ->
                        durationUs(stateNanos, modelCommittedNanos)
                    },
                presentationToModelUs =
                    trace.presentationCommittedNanos?.let { presentationNanos ->
                        durationUs(presentationNanos, modelCommittedNanos)
                    },
                modelToDrawUs = durationUs(modelCommittedNanos, drawNanos),
                sourceToDrawUs = durationUs(trace.sourceNanos, drawNanos),
                committedOnMainThread = committedOnMainThread,
            )

        private fun durationUs(
            startNanos: Long,
            endNanos: Long,
        ): Long =
            (endNanos - startNanos)
                .coerceAtLeast(0L) / NANOS_PER_MICROSECOND

        private const val NANOS_PER_MICROSECOND = 1_000L
    }
}
