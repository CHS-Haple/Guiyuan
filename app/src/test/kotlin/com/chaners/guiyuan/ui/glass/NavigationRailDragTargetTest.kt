// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

/** Rail's variable-width targeting must not change the accepted symmetric bottom bar. */
class NavigationRailDragTargetTest {
    @Test
    fun equalWidthBarKeepsSameItemTrail() {
        val target = navigationDragTarget(20f, 30f, 0f, 100f, 4f, false)
        assertEquals(4f, target.left, 0.001f)
        assertEquals(50f, target.right, 0.001f)
    }

    @Test
    fun changingDestinationDoesNotStretchTheTarget() {
        val target = navigationDragTarget(20f, 30f, 0f, 100f, 4f, true)
        assertEquals(20f, target.left, 0.001f)
        assertEquals(50f, target.right, 0.001f)
    }

    @Test
    fun asymmetricTrailingEdgeDoesNotOvershootOnOutwardDrag() {
        val target = navigationDragTarget(100f, 30f, 10f, 120f, 3f, false)
        assertEquals(90f, target.left, 0.001f)
        assertEquals(120f, target.right, 0.001f)
    }

    @Test
    fun indicatorClampsAgainstDistinctStartAndEndInsets() {
        val bounds = navigationIndicatorBounds(-20f, 160f, 120f, 12f, 20f)
        assertEquals(12f, bounds.x, 0.001f)
        assertEquals(100f, bounds.y, 0.001f)
    }

    @Test
    fun variableWidthSlotMapsToFractionalDestination() {
        assertEquals(1.5f, navigationSlotAt(40f, listOf(20f, 40f, 60f)), 0.001f)
    }

    @Test
    fun dragWidthInterpolatesWithoutJumpingAcrossDifferentSlots() {
        assertEquals(30f, navigationDragWidth(25f, listOf(20f, 40f)), 0.001f)
    }
}
