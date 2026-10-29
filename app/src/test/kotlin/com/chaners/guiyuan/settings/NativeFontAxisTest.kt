package com.chaners.guiyuan.settings

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NativeFontAxisTest {
    @Test
    fun fatalPreferenceReadErrorsPreserveOriginalCause() {
        val vm = OutOfMemoryError("native-axis")
        val wrapped = runCatching {
            NativeFontAxis.rethrowFatal(InvocationTargetException(vm))
        }.exceptionOrNull()
        assertSame(vm, wrapped)
        val thread = ThreadDeath()
        assertSame(thread, runCatching { NativeFontAxis.rethrowFatal(thread) }.exceptionOrNull())
        NativeFontAxis.rethrowFatal(IllegalStateException("ordinary-unavailable"))
    }

    @Test
    fun acceptsActualVariableAxisRange() {
        val axis = FontWeightAxis(150f, 330f, 700f)
        assertEquals(axis, NativeFontAxis.checked(axis))
        assertEquals(FontWeightAxis(100f, 400f, 1000f),
            NativeFontAxis.checked(FontWeightAxis(100f, 400f, 1000f)))
    }

    @Test
    fun fontReceiptDiagnosticsSeparateMissingStaleAndUnknownBoot() {
        assertEquals("boot-unavailable", NativeFontAxis.receiptStatus(-1, -1, false))
        assertEquals("not-received", NativeFontAxis.receiptStatus(4, -1, false))
        assertEquals("not-received", NativeFontAxis.receiptStatus(4, 4, false))
        assertEquals("stale-boot", NativeFontAxis.receiptStatus(5, 4, true))
        assertEquals("received", NativeFontAxis.receiptStatus(5, 5, true))
    }

    @Test
    fun distinguishesPendingAndUnavailableReports() {
        val axis = FontWeightAxis(150f, 330f, 700f)
        assertEquals(NativeFontAxis.State.Pending, NativeFontAxis.state(false, null))
        assertEquals(NativeFontAxis.State.Pending, NativeFontAxis.state(false, axis))
        assertEquals(NativeFontAxis.State.Unavailable, NativeFontAxis.state(true, null))
        assertEquals(
            NativeFontAxis.State.Variable(axis),
            NativeFontAxis.state(true, axis),
        )
        assertEquals(
            NativeFontAxis.State.Unavailable,
            NativeFontAxis.state(true, FontWeightAxis(0f, 330f, 700f)),
        )
    }

    @Test
    fun receiverAuthorizationRequiresSystemUidPackageAndAction() {
        val action = NativeFontAxis.ACTION
        assertNull(NativeFontAxis.receiverReject(37, android.os.Process.SYSTEM_UID, "com.android.systemui", action))
        assertEquals(NativeFontAxis.ReceiverReject.API,
            NativeFontAxis.receiverReject(33, android.os.Process.SYSTEM_UID, "com.android.systemui", action))
        assertEquals(NativeFontAxis.ReceiverReject.UID,
            NativeFontAxis.receiverReject(37, -1, "com.android.systemui", action))
        assertEquals(NativeFontAxis.ReceiverReject.PACKAGE,
            NativeFontAxis.receiverReject(37, android.os.Process.SYSTEM_UID, null, action))
        assertEquals(NativeFontAxis.ReceiverReject.ACTION,
            NativeFontAxis.receiverReject(37, android.os.Process.SYSTEM_UID, "com.android.systemui", "wrong"))
    }

    @Test
    fun receiverEvidenceIsBoundedToTwentyEvents() {
        val events = (1..28).fold("") { history, n ->
            NativeFontAxis.appendTransportEvent(history, "event-$n")
        }.lines()
        assertEquals(20, events.size)
        assertEquals("event-9", events.first())
        assertEquals("event-28", events.last())
    }

    @Test
    fun rejectsUnverifiedAndUnsafeAxis() {
        assertNull(NativeFontAxis.checked(null))
        assertNull(NativeFontAxis.checked(FontWeightAxis(Float.NaN, 330f, 700f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(0f, 330f, 700f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(150f, 800f, 700f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(150f, 330f, 1100f)))
        assertNull(NativeFontAxis.checked(FontWeightAxis(700f, 700f, 700f)))
    }
}
