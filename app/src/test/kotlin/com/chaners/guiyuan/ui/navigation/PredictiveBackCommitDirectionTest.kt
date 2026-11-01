package com.chaners.guiyuan.ui.navigation

import java.io.File
import kotlin.math.exp
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression contract for the precisely pinned MIUIX NavDisplay source overlay.
 *
 * The unit tests verify the mathematical failure and the directional handoff;
 * the source assertion binds those cases to the actual compiled MIUIX checkout.
 * It is not a substitute for the target-device OEM back-event trace.
 */
class PredictiveBackCommitDirectionTest {
    private val springFrequency = sqrt(146.0)

    // Mirror the single expression installed by the reviewed upstream patch.
    private fun completedBackVelocity(
        measuredProgressVelocity: Float,
        distanceToExit: Float,
    ): Float {
        val released = -measuredProgressVelocity
        val floor = -springFrequency.toFloat() * distanceToExit
        return released.coerceIn(floor, 0f)
    }

    @Test
    fun mildReverseSampleMustNotReverseAlreadyCommittedReturn() {
        // OEM completion has already happened, but its final two progress samples
        // indicate a small return motion; upstream accepts it above -1f.
        val measuredProgressVelocity = -0.6f
        assertTrue(measuredProgressVelocity > -1f)

        val unsafeSeed = -measuredProgressVelocity
        val correctedSeed = completedBackVelocity(measuredProgressVelocity, 0.3f)
        assertTrue("Unbounded upstream seed moves away from exit", unsafeSeed > 0f)
        assertEquals("Commit seed cannot move away from exit", 0f, correctedSeed)

        // A critically damped spring starting at x>0 with v>0 initially reverses,
        // whereas v=0 first advances toward target without a frame of backtracking.
        fun distanceAfterFrame(v: Double): Double {
            val dt = 1.0 / 60.0
            val initialDistance = 0.3
            return exp(-springFrequency * dt) *
                (initialDistance + (v + springFrequency * initialDistance) * dt)
        }
        assertTrue(distanceAfterFrame(unsafeSeed.toDouble()) > 0.3)
        assertTrue(distanceAfterFrame(correctedSeed.toDouble()) < 0.3)
    }

    @Test
    fun normalCommitKeepsVelocityWhileExcessSpeedRetainsNoOvershootFloor() {
        assertEquals(-0.6f, completedBackVelocity(0.6f, 0.3f), 0.0001f)
        assertEquals(0f, completedBackVelocity(0f, 0.3f), 0.0001f)
        val floor = -springFrequency.toFloat() * 0.3f
        assertEquals(floor, completedBackVelocity(100f, 0.3f), 0.0001f)
        assertEquals(0f, completedBackVelocity(-0.5f, 0.3f), 0.0001f)
    }

    @Test
    fun sourceBuiltMiuixCheckoutContainsExactlyTheReviewedExpression() {
        val sourceDir = System.getenv("MIUIX_SOURCE_DIR") ?: return
        val relativePath =
            "miuix-nav/src/commonMain/kotlin/top/yukonga/miuix/kmp/nav/core/NavDisplay.kt"
        // Gradle's JUnit working directory is the :app module, while GitHub's
        // action checks out MIUIX at the repository root.
        val candidates = listOf(
            File(sourceDir, relativePath),
            File(File(System.getenv("GITHUB_WORKSPACE") ?: ".", sourceDir), relativePath),
            File("../$sourceDir", relativePath),
        )
        val source = candidates.firstOrNull(File::isFile)
        assertTrue("CI source-built MIUIX navigation file is missing", source != null)
        val contents = requireNotNull(source).readText()
        assertEquals(
            1,
            contents.windowed("initialVelocity = released.coerceIn(floor, 0f),".length)
                .count { it == "initialVelocity = released.coerceIn(floor, 0f)," },
        )
        assertTrue("System return cancellation still uses upstream settle", contents.contains("presentation.animatedTop.settleCancel("))
    }
}
