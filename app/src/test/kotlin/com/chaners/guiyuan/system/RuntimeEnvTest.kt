package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Test

private const val TEST_OS_PATCHED_VERSION = "4.0.0.14.XOBCNXM.D01"
private const val TEST_OS_SOURCE_VERSION = "OS4.0.0.14.XOBCNXM"
private const val TEST_OS_BASE_VERSION = "4.0.0.14.XOBCNXM"
private const val TEST_OS_REVISION = "D01"

class RuntimeEnvTest {
    @Test
    fun appendsSoftwareUpdateRevisionToHyperOsVersion() {
        assertEquals(
            TEST_OS_PATCHED_VERSION,
            RuntimeEnv.composeOsVersion(
                baseVersion = TEST_OS_SOURCE_VERSION,
                primaryRevision = TEST_OS_REVISION,
                secondaryRevision = "",
            ),
        )
    }

    @Test
    fun keepsAlreadySuffixedVersionUnchanged() {
        assertEquals(
            TEST_OS_PATCHED_VERSION,
            RuntimeEnv.composeOsVersion(
                baseVersion = TEST_OS_PATCHED_VERSION,
                primaryRevision = TEST_OS_REVISION,
                secondaryRevision = "",
            ),
        )
    }

    @Test
    fun choosesNewerRevisionWithSamePrefix() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D02",
            RuntimeEnv.composeOsVersion(
                baseVersion = TEST_OS_BASE_VERSION,
                primaryRevision = TEST_OS_REVISION,
                secondaryRevision = "D02",
            ),
        )
    }

    @Test
    fun ignoresInvalidRevision() {
        assertEquals(
            TEST_OS_BASE_VERSION,
            RuntimeEnv.composeOsVersion(
                baseVersion = TEST_OS_SOURCE_VERSION,
                primaryRevision = "invalid",
                secondaryRevision = "",
            ),
        )
    }
}
