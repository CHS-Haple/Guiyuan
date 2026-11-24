package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeStatusSlotsTest {
    @Test
    fun classifiesHyperOsRemoveAllWithPipelineFlag() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "removeAllIconsForSlot",
                String::class.java,
                requireNotNull(Boolean::class.javaPrimitiveType),
            )

        assertEquals(
            NativeStatusSlots.RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG,
            NativeStatusSlots.classifyRemoval(method),
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
            NativeStatusSlots.RemovalMode.REMOVE_TAGGED,
            NativeStatusSlots.classifyRemoval(method),
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
            NativeStatusSlots.RemovalMode.REMOVE_ALL_SLOT,
            NativeStatusSlots.classifyRemoval(removeAll),
        )
        assertEquals(
            NativeStatusSlots.RemovalMode.REMOVE_SLOT,
            NativeStatusSlots.classifyRemoval(removeSlot),
        )
    }

    @Test
    fun selectsNativePipelineRemovalBeforeFallbacks() {
        assertEquals(
            NativeStatusSlots.RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG,
            NativeStatusSlots.removal(Fixture::class.java)?.mode,
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
            NativeStatusSlots.classifyRemoval(method),
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
