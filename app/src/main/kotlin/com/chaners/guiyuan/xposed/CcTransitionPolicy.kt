package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object CcTransitionPolicy {
    private const val LATENT_REVEAL_COMPLETE_FRACTION = 0.35f

    fun geometryProgress(raw: Float): Float =
        if (raw.isFinite()) raw.coerceIn(0f, 1f) else 0f

    // Keep a retargeted projected participant still at the held native fraction.
    // Continued panel motion, not a new timer, brings it to the live endpoint.
    fun rootAnchoredCarrier(
        carrier: FloatArray,
        root: FloatArray,
        rootRelativeX: Float,
    ): FloatArray {
        require(carrier.size == 6 && root.size == 6)
        return carrier.copyOf().also {
            it[0] = root[0] + rootRelativeX
        }
    }

    fun retargetWeight(progress: Float, anchor: Float): Float {
        val p = geometryProgress(progress)
        val at = geometryProgress(anchor)
        if (at <= 0f || at >= 1f) return 0f
        return if (p <= at) p / at else (1f - p) / (1f - at)
    }

    // Reject a native FINAL row while its Wi-Fi slot overlaps the battery carrier.
    fun wifiBeforeBattery(wifi: FloatArray, battery: FloatArray, rtl: Boolean): Boolean {
        if (wifi.size != 6 || battery.size != 6 ||
            !wifi.all(Float::isFinite) || !battery.all(Float::isFinite)
        ) return false
        val wifiWidth = vectorLength(wifi[2], wifi[3])
        val batteryWidth = vectorLength(battery[2], battery[3])
        if (wifiWidth <= 0f || batteryWidth <= 0f) return false
        return if (rtl) {
            wifi[0] - wifiWidth / 2f >= battery[0] + batteryWidth / 2f
        } else {
            wifi[0] + wifiWidth / 2f <= battery[0] - batteryWidth / 2f
        }
    }

    fun followBatteryAnchor(
        target: FloatArray,
        previousBattery: FloatArray,
        currentBattery: FloatArray,
    ): FloatArray {
        require(target.size == 6 && previousBattery.size == 6 && currentBattery.size == 6)
        return target.copyOf().apply {
            this[0] += currentBattery[0] - previousBattery[0]
            this[1] += currentBattery[1] - previousBattery[1]
        }
    }

    fun retargetShift(
        priorShift: Float,
        priorAnchor: Float?,
        progress: Float,
        nativeDelta: Float,
    ): Float {
        val retained = priorAnchor?.let {
            priorShift * retargetWeight(progress, it)
        } ?: 0f
        return retained - nativeDelta
    }

    fun handoffMotionProgress(
        expansionProgress: Float,
        finalAppearanceAlpha: Float,
        finalAppearanceActive: Boolean,
    ): Float {
        val expansion = geometryProgress(expansionProgress)
        if (!finalAppearanceActive) return expansion
        val appearance =
            finalAppearanceAlpha
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 0f
        return expansion + (1f - expansion) * appearance
    }

    fun aggregateAnchors(
        bounds: StatusPainter.TransitionBounds,
        top: ParticipantVisualSnapshot.NormalizedRect,
        bottom: ParticipantVisualSnapshot.NormalizedRect,
        topShape: ParticipantVisualSnapshot.RowShape,
        bottomShape: ParticipantVisualSnapshot.RowShape,
        widthCompensation: Float,
        heightCompensation: Float,
        dotDiameter: Float,
    ): FloatArray {
        fun y(
            rect: ParticipantVisualSnapshot.NormalizedRect,
            shape: ParticipantVisualSnapshot.RowShape,
        ): Float {
            val normalized = if (shape == ParticipantVisualSnapshot.RowShape.DOTS) {
                rect.centerY
            } else {
                rect.bottom
            }
            val position = bounds.centerY +
                (normalized - 0.5f) * bounds.height * heightCompensation
            return if (shape == ParticipantVisualSnapshot.RowShape.BARS) {
                position - dotDiameter / 2f
            } else position
        }
        return floatArrayOf(
            bounds.centerX +
                ((top.centerX + bottom.centerX) / 2f - 0.5f) *
                    bounds.width * widthCompensation,
            y(top, topShape),
            y(bottom, bottomShape),
        )
    }

    fun mobileSignalShapeProgress(rawProgress: Float): Float {
        val p = geometryProgress(rawProgress)
        return p * p
    }

    fun horizontalExitDirection(
        source: FloatArray,
        target: FloatArray,
    ): BatteryRingTransitionPolicy.ExitDirection {
        if (source.size != 6 || target.size != 6) {
            return BatteryRingTransitionPolicy.ExitDirection.NONE
        }
        val sourceCenterX = source[0] + (source[2] + source[4]) * 0.5f
        val targetCenterX = target[0] + (target[2] + target[4]) * 0.5f
        if (!sourceCenterX.isFinite() || !targetCenterX.isFinite()) {
            return BatteryRingTransitionPolicy.ExitDirection.NONE
        }
        return when {
            targetCenterX - sourceCenterX < -0.5f ->
                BatteryRingTransitionPolicy.ExitDirection.LEFT
            targetCenterX - sourceCenterX > 0.5f ->
                BatteryRingTransitionPolicy.ExitDirection.RIGHT
            else -> BatteryRingTransitionPolicy.ExitDirection.NONE
        }
    }

    fun batteryRingExitDirection(
        liveCenterDirection: BatteryRingTransitionPolicy.ExitDirection,
        nativeBatteryIslandActive: Boolean,
        targetRowRtl: Boolean,
    ): BatteryRingTransitionPolicy.ExitDirection {
        // The ring must retain its exit side when the native center glyph
        // disappears. Battery-Island reflow also makes the live X unreliable.
        if (
            !nativeBatteryIslandActive &&
            liveCenterDirection != BatteryRingTransitionPolicy.ExitDirection.NONE
        ) {
            return liveCenterDirection
        }
        return if (targetRowRtl) {
            BatteryRingTransitionPolicy.ExitDirection.RIGHT
        } else {
            BatteryRingTransitionPolicy.ExitDirection.LEFT
        }
    }

    fun unmatchedExitOpacity(rawProgress: Float): Float {
        val remaining = 1f - geometryProgress(rawProgress)
        return remaining * remaining * remaining
    }

    fun horizontalClipBounds(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        visibleFraction: Float,
        anchorRight: Boolean,
    ): FloatArray? {
        if (
            !left.isFinite() ||
            !top.isFinite() ||
            !right.isFinite() ||
            !bottom.isFinite() ||
            right <= left ||
            bottom <= top
        ) {
            return null
        }
        val fraction =
            visibleFraction
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 0f
        if (fraction <= 0f) return null
        if (fraction >= 1f) return floatArrayOf(left, top, right, bottom)
        val width = (right - left) * fraction
        return if (anchorRight) {
            floatArrayOf(right - width, top, right, bottom)
        } else {
            floatArrayOf(left, top, left + width, bottom)
        }
    }

    fun expandedClipBounds(
        bounds: StatusPainter.TransitionBounds,
        widthScale: Float,
        heightScale: Float,
    ): StatusPainter.TransitionBounds {
        val resolvedWidthScale =
            widthScale
                .takeIf { it.isFinite() && it > 0f }
                ?.coerceAtLeast(1f)
                ?: 1f
        val resolvedHeightScale =
            heightScale
                .takeIf { it.isFinite() && it > 0f }
                ?.coerceAtLeast(1f)
                ?: 1f
        val halfWidth = bounds.width * resolvedWidthScale / 2f
        val halfHeight = bounds.height * resolvedHeightScale / 2f
        return StatusPainter.TransitionBounds(
            left = bounds.centerX - halfWidth,
            top = bounds.centerY - halfHeight,
            right = bounds.centerX + halfWidth,
            bottom = bounds.centerY + halfHeight,
        )
    }

    fun interpolateGeometry(
        source: FloatArray,
        target: FloatArray,
        progress: Float,
    ): FloatArray {
        require(source.size == 6 && target.size == 6)
        val p = progress.coerceIn(0f, 1f)
        return FloatArray(6) { index ->
            source[index] + (target[index] - source[index]) * p
        }
    }

    // The carried dot pair shares one translation before its native rows split.
    fun dualMobileRowMotionGeometry(
        source: FloatArray,
        target: FloatArray,
        groupTravelX: Float,
        groupTravelY: Float,
        progress: Float,
        scalePolicy: StatusPainter.TransitionScalePolicy,
    ): FloatArray {
        require(source.size == 6 && target.size == 6)
        val together = MobileSignalStagePolicy.lockstepProgress(progress)
        val paired = source.copyOf().apply {
            this[0] += groupTravelX * together
            this[1] += groupTravelY * together
        }
        val normal = interpolateSimilarityGeometry(
            source = source,
            target = target,
            progress = progress,
            scalePolicy = scalePolicy,
        )
        return interpolateGeometry(
            source = paired,
            target = normal,
            progress = MobileSignalStagePolicy.moveProgress(progress),
        )
    }

    fun interpolateSimilarityGeometry(
        source: FloatArray,
        target: FloatArray,
        progress: Float,
        scalePolicy: StatusPainter.TransitionScalePolicy,
    ): FloatArray {
        require(source.size == 6 && target.size == 6)
        val p = progress.coerceIn(0f, 1f)
        val sourceWidth = vectorLength(source[2], source[3])
        val sourceHeight = vectorLength(source[4], source[5])
        val targetWidth = vectorLength(target[2], target[3])
        val targetHeight = vectorLength(target[4], target[5])
        if (
            sourceWidth <= 0f ||
            sourceHeight <= 0f ||
            targetWidth <= 0f ||
            targetHeight <= 0f
        ) {
            return source.copyOf()
        }
        val rawTargetScale =
            min(
                targetWidth / sourceWidth,
                targetHeight / sourceHeight,
            )
        val targetScale =
            when (scalePolicy) {
                StatusPainter.TransitionScalePolicy.TARGET ->
                    rawTargetScale

                StatusPainter.TransitionScalePolicy.SHRINK_ONLY ->
                    min(rawTargetScale, 1f)
            }
        val scale = 1f + (targetScale - 1f) * p
        return floatArrayOf(
            source[0] + (target[0] - source[0]) * p,
            source[1] + (target[1] - source[1]) * p,
            source[2] * scale,
            source[3] * scale,
            source[4] * scale,
            source[5] * scale,
        )
    }

    fun interpolateCarriedSourceToRootTarget(
        source: FloatArray,
        target: FloatArray,
        sourceCarrier: FloatArray,
        currentCarrier: FloatArray,
        progress: Float,
        scalePolicy: StatusPainter.TransitionScalePolicy,
    ): FloatArray {
        require(
            source.size == 6 &&
                target.size == 6 &&
                sourceCarrier.size == 6 &&
                currentCarrier.size == 6,
        )
        val carriedSource =
            CcCarrierGeometryPolicy.rebaseSourceToCurrentCarrier(
                source = source,
                sourceCarrier = sourceCarrier,
                currentCarrier = currentCarrier,
            )
        return interpolateSimilarityGeometry(
            source = carriedSource,
            target = target,
            progress = geometryProgress(progress),
            scalePolicy = scalePolicy,
        )
    }

    fun latentReservationProgress(
        compactWidthPx: Int,
        currentReservationPx: Int,
        requiredReservationPx: Int,
        visualWidthPx: Float,
    ): Float {
        if (!visualWidthPx.isFinite() || visualWidthPx <= 0f) return 0f
        val compact = compactWidthPx.coerceAtLeast(0).toFloat()
        val current = currentReservationPx.coerceAtLeast(0).toFloat()
        val required = requiredReservationPx.coerceAtLeast(compactWidthPx).toFloat()
        val start = maxOf(compact, required - visualWidthPx)
        if (required <= start) {
            return if (current >= required) 1f else 0f
        }
        val normalized =
            ((current - start) / (required - start))
                .coerceIn(0f, 1f)
        return normalized * normalized * (3f - 2f * normalized)
    }

    fun latentRevealVisibleFraction(
        current: FloatArray,
        target: FloatArray,
        visualExtent: Float,
        reservationProgress: Float,
    ): Float {
        require(current.size == 6 && target.size == 6)
        if (
            !visualExtent.isFinite() ||
            visualExtent <= 0f
        ) return 0f

        val reservation =
            reservationProgress
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 0f
        if (reservation <= 0f) return 0f

        val deltaX = target[0] - current[0]
        val deltaY = target[1] - current[1]
        val remainingDistance =
            sqrt(deltaX * deltaX + deltaY * deltaY)
        if (remainingDistance >= visualExtent) return 0f

        val proximityProgress =
            (1f - remainingDistance / visualExtent)
                .coerceIn(0f, 1f)
        val proximity =
            acceleratedLatentRevealProgress(proximityProgress)
        val occupancy =
            acceleratedLatentRevealProgress(reservation)
        return min(proximity, occupancy)
    }

    fun acceleratedLatentRevealProgress(progress: Float): Float {
        val normalized =
            (
                progress
                    .takeIf(Float::isFinite)
                    ?.coerceIn(0f, 1f)
                    ?: 0f
            ) / LATENT_REVEAL_COMPLETE_FRACTION
        val phase = normalized.coerceIn(0f, 1f)
        return phase * phase * (3f - 2f * phase)
    }

    fun semanticFallbackBounds(
        preferredChildEntries: List<String>,
        isRtl: Boolean,
    ): StatusPainter.TransitionNormalizedBounds? {
        val logical =
            when {
                preferredChildEntries.contains("mobile_signal") ->
                    StatusPainter.TransitionNormalizedBounds(
                        left = 0.48f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                    )

                preferredChildEntries.contains("wifi_signal") ->
                    StatusPainter.TransitionNormalizedBounds(
                        left = 0f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                    )

                else -> null
            } ?: return null
        if (!isRtl || (logical.left == 0f && logical.right == 1f)) {
            return logical
        }
        return StatusPainter.TransitionNormalizedBounds(
            left = 1f - logical.right,
            top = logical.top,
            right = 1f - logical.left,
            bottom = logical.bottom,
        )
    }

    fun relativeGeometryWidth(
        target: FloatArray,
        current: FloatArray,
    ): Float? {
        require(target.size == 6)
        require(current.size == 6)
        val targetWidth = vectorLength(target[2], target[3])
        val currentWidth = vectorLength(current[2], current[3])
        if (targetWidth <= 0f || currentWidth <= 0f) return null
        return targetWidth / currentWidth
    }

    fun relativeGeometryHeight(
        target: FloatArray,
        current: FloatArray,
    ): Float? {
        require(target.size == 6)
        require(current.size == 6)
        val targetHeight = vectorLength(target[4], target[5])
        val currentHeight = vectorLength(current[4], current[5])
        if (targetHeight <= 0f || currentHeight <= 0f) return null
        return targetHeight / currentHeight
    }

    private fun vectorLength(
        x: Float,
        y: Float,
    ): Float = sqrt(x * x + y * y)
}
