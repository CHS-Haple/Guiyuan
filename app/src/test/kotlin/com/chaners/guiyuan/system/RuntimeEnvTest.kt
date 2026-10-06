package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeEnvTest {
    @Test
    fun appendsHyperOsRevision() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D01",
            RuntimeEnv.composeOsVersion(
                baseVersion = "OS4.0.0.14.XOBCNXM",
                primaryRevision = "D01",
                secondaryRevision = "",
            ),
        )
    }

    @Test
    fun keepsAlreadySuffixedVersionUnchanged() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D01",
            RuntimeEnv.composeOsVersion(
                baseVersion = "4.0.0.14.XOBCNXM.D01",
                primaryRevision = "D01",
                secondaryRevision = "",
            ),
        )
    }

    @Test
    fun choosesNewerRevisionWithSamePrefix() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D02",
            RuntimeEnv.composeOsVersion(
                baseVersion = "4.0.0.14.XOBCNXM",
                primaryRevision = "D01",
                secondaryRevision = "D02",
            ),
        )
    }

    @Test
    fun ignoresInvalidRevision() {
        assertEquals(
            "4.0.0.14.XOBCNXM",
            RuntimeEnv.composeOsVersion(
                baseVersion = "OS4.0.0.14.XOBCNXM",
                primaryRevision = "invalid",
                secondaryRevision = "",
            ),
        )
    }
}
