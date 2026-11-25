@file:Suppress("unused", "UNUSED_PARAMETER")

package com.chaners.guiyuan.xposed

import com.android.systemui.statusbar.phone.MiuiHomePrivacyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Exact-ROM selector signature exercised both positively and negatively.
private const val ADD_IGNORED_SLOTS = "addIgnoredSlots"

// Minimal native lookalikes: reflection is tested, not any SystemUI behavior.
private open class TestSlotsParent {
    private val ignoredSlots = mutableListOf<String>()
}

private class TestSlotsHost : TestSlotsParent() {
    fun addIgnoredSlots(slots: List<String>) {}

    fun setIgnoredSlots(slots: List<String>) {}

    fun onMeasure(width: Int, height: Int) {}

    fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {}

    fun getIslandShowing(): Boolean = false
}

private open class TestBatteryParent {
    private var mIsHideBattery = false
}

private class TestBatteryHost : TestBatteryParent() {
    private var mPrivacyState: MiuiHomePrivacyController.PrivacyState? = null

    fun setIsHideBattery(hidden: Boolean?) {}

    fun setPrivacyState(state: MiuiHomePrivacyController.PrivacyState?) {}
}

private class WrongSlotsHost {
    private val ignoredSlots = setOf<String>()

    fun addIgnoredSlots(slots: Set<String>) {}
}

private class WrongBatteryHost {
    private var mIsHideBattery = "false"

    fun setIsHideBattery(hidden: Boolean) {}
}

class SysUiPresentationContractsTest {
    @Test
    fun nativeSlotContractsMatchExactSignaturesAndInheritedField() {
        val source = TestSlotsHost::class.java
        assertEquals("ignoredSlots", SysUiPresentationContracts.ignoredSlotsField(source)?.name)
        assertEquals(
            ADD_IGNORED_SLOTS,
            SysUiPresentationContracts.ignoredSlotsMethod(source, ADD_IGNORED_SLOTS)?.name,
        )
        assertEquals(
            "setIgnoredSlots",
            SysUiPresentationContracts.ignoredSlotsMethod(source, "setIgnoredSlots")?.name,
        )
        assertEquals("onMeasure", SysUiPresentationContracts.measureMethod(source)?.name)
        assertEquals("onLayout", SysUiPresentationContracts.layoutMethod(source)?.name)
        assertEquals("getIslandShowing", SysUiPresentationContracts.islandShowingMethod(source)?.name)
    }

    @Test
    fun nativeBatteryAndPrivacyContractsMatchWithoutGuessing() {
        val source = TestBatteryHost::class.java
        assertEquals("mIsHideBattery", SysUiPresentationContracts.batteryHideField(source)?.name)
        assertEquals("setIsHideBattery", SysUiPresentationContracts.batteryHideMethod(source)?.name)

        val field = SysUiPresentationContracts.privacyField(source)
        assertNotNull(field)
        assertEquals("mPrivacyState", field?.name)
        assertEquals(
            "setPrivacyState",
            SysUiPresentationContracts.privacyMethod(source, requireNotNull(field))?.name,
        )
    }

    @Test
    fun mismatchedNativeContractsFailClosed() {
        val badSlots = WrongSlotsHost::class.java
        assertNull(SysUiPresentationContracts.ignoredSlotsField(badSlots))
        assertNull(SysUiPresentationContracts.ignoredSlotsMethod(badSlots, ADD_IGNORED_SLOTS))
        assertNull(SysUiPresentationContracts.measureMethod(badSlots))
        assertNull(SysUiPresentationContracts.layoutMethod(badSlots))
        assertNull(SysUiPresentationContracts.islandShowingMethod(badSlots))

        val badBattery = WrongBatteryHost::class.java
        assertNull(SysUiPresentationContracts.batteryHideField(badBattery))
        assertNull(SysUiPresentationContracts.batteryHideMethod(badBattery))
        assertNull(SysUiPresentationContracts.privacyField(badBattery))
    }
}
