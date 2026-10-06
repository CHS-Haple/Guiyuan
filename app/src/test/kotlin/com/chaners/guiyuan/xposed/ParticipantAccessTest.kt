package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParticipantAccessTest {
    @Test
    fun classifiesHyperOsContentSlotResourceSetter() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "setIcon",
                CharSequence::class.java,
                String::class.java,
                requireNotNull(Int::class.javaPrimitiveType),
            )

        assertEquals(
            ParticipantAccess.ResourceSetIconMode.CONTENT_SLOT_RES,
            ParticipantAccess.classifyResourceSetIcon(method),
        )
    }

    @Test
    fun classifiesAospSlotResourceContentSetter() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "setIcon",
                String::class.java,
                requireNotNull(Int::class.javaPrimitiveType),
                CharSequence::class.java,
            )

        assertEquals(
            ParticipantAccess.ResourceSetIconMode.SLOT_RES_CONTENT,
            ParticipantAccess.classifyResourceSetIcon(method),
        )
    }

    @Test
    fun classifiesHyperOsRemoveAllWithPipelineFlag() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "removeAllIconsForSlot",
                String::class.java,
                requireNotNull(Boolean::class.javaPrimitiveType),
            )

        assertEquals(
            ParticipantAccess.RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG,
            ParticipantAccess.classifyRemoval(method),
        )
    }

    @Test
    fun classifiesTaggedRemovalFallback() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "removeIcon",
                String::class.java,
                requireNotNull(Int::class.javaPrimitiveType),
            )

        assertEquals(
            ParticipantAccess.RemovalMode.REMOVE_TAGGED,
            ParticipantAccess.classifyRemoval(method),
        )
    }

    @Test
    fun rejectsUnrelatedMethods() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "other",
                String::class.java,
            )

        assertNull(
            ParticipantAccess.classifyResourceSetIcon(method),
        )
        assertNull(
            ParticipantAccess.classifyRemoval(method),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private class Fixture {
        fun setIcon(
            contentDescription: CharSequence,
            slot: String,
            resourceId: Int,
        ) = Unit

        fun setIcon(
            slot: String,
            resourceId: Int,
            contentDescription: CharSequence,
        ) = Unit

        fun removeAllIconsForSlot(
            slot: String,
            fromNewPipeline: Boolean,
        ) = Unit

        fun removeIcon(
            slot: String,
            tag: Int,
        ) = Unit

        fun other(slot: String) = Unit
    }
}
