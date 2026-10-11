package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeParticipantAccessTest {
    @Test
    fun classifiesHyperOsRemoveAllWithPipelineFlag() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "removeAllIconsForSlot",
                String::class.java,
                requireNotNull(Boolean::class.javaPrimitiveType),
            )

        assertEquals(
            NativeParticipantAccess.RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG,
            NativeParticipantAccess.classifyRemoval(method),
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
            NativeParticipantAccess.RemovalMode.REMOVE_TAGGED,
            NativeParticipantAccess.classifyRemoval(method),
        )
    }

    @Test
    fun classifiesUntaggedRemoveAllAndRemoveSlotFallbacks() {
        val removeAll = Fixture::class.java.getDeclaredMethod(
            "removeAllIconsForSlot",
            String::class.java,
        )
        val removeSlot = Fixture::class.java.getDeclaredMethod(
            "removeIcon",
            String::class.java,
        )
        assertEquals(
            NativeParticipantAccess.RemovalMode.REMOVE_ALL_SLOT,
            NativeParticipantAccess.classifyRemoval(removeAll),
        )
        assertEquals(
            NativeParticipantAccess.RemovalMode.REMOVE_SLOT,
            NativeParticipantAccess.classifyRemoval(removeSlot),
        )
    }

    @Test
    fun selectsNativePipelineRemovalBeforeFallbacks() {
        assertEquals(
            NativeParticipantAccess.RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG,
            NativeParticipantAccess.removal(Fixture::class.java)?.mode,
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
            NativeParticipantAccess.classifyRemoval(method),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private class Fixture {
        fun removeAllIconsForSlot(
            slot: String,
            fromNewPipeline: Boolean,
        ) = Unit

        fun removeAllIconsForSlot(slot: String) = Unit

        fun removeIcon(
            slot: String,
            tag: Int,
        ) = Unit

        fun removeIcon(slot: String) = Unit

        fun other(slot: String) = Unit
    }
}
