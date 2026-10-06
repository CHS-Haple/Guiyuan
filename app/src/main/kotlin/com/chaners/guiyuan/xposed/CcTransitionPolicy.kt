package com.chaners.guiyuan.xposed

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object CcTransition {
    private const val LATENT_REVEAL_COMPLETE_FRACTION = 0.35f
    fun geometryProgress(raw: Float): Float =
        if (raw.isFinite()) raw.coerceIn(0f, 1f) else 0f

    fun motionProgress(raw: Float): Float =
        geometryProgress(raw)

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

    fun mobileSignalShapeProgress(rawProgress: Float): Float {
        val p = geometryProgress(rawProgress)
        return p * p
    }

    fun horizontalExitDirection(
        source: FloatArray,
        target: FloatArray,
    ): BatteryRingTransition.ExitDirection {
        if (source.size != 6 || target.size != 6) {
            return BatteryRingTransition.ExitDirection.NONE
        }
        val sourceCenterX = source[0] + (source[2] + source[4]) * 0.5f
        val targetCenterX = target[0] + (target[2] + target[4]) * 0.5f
        if (!sourceCenterX.isFinite() || !targetCenterX.isFinite()) {
            return BatteryRingTransition.ExitDirection.NONE
        }
        return when {
            targetCenterX - sourceCenterX < -0.5f ->
                BatteryRingTransition.ExitDirection.LEFT
            targetCenterX - sourceCenterX > 0.5f ->
                BatteryRingTransition.ExitDirection.RIGHT
            else -> BatteryRingTransition.ExitDirection.NONE
        }
    }

    fun batteryRingExitDirection(
        liveCenterDirection: BatteryRingTransition.ExitDirection,
        nativeBatteryIslandActive: Boolean,
        targetRowRtl: Boolean,
    ): BatteryRingTransition.ExitDirection {
        if (!nativeBatteryIslandActive) return liveCenterDirection

        // During HyperOS Battery-Island expansion the final status row is
        // itself being reflowed. Its live pixel X is therefore not a stable
        // direction authority for the ring's first frames. The structural
        // destination is still toward the status-row logical start.
        return if (targetRowRtl) {
            BatteryRingTransition.ExitDirection.RIGHT
        } else {
            BatteryRingTransition.ExitDirection.LEFT
        }
    }

    fun unmatchedExitVisibleFraction(rawProgress: Float): Float {
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

    fun selectNativeTransitionTint(
        statusIconPeerTint: Int?,
        cachedTint: Int?,
    ): Int? {
        fun valid(color: Int?): Int? =
            color?.takeIf { candidate -> candidate ushr 24 != 0 }
        return valid(statusIconPeerTint)
            ?: valid(cachedTint)
    }

    fun usesSemanticTransitionReservation(
        sourceScene: SourceScene,
        charging: Boolean = false,
        nativeBatteryIslandActive: Boolean? = null,
    ): Boolean =
        when (sourceScene) {
            SourceScene.HOME,
            SourceScene.KEYGUARD,
            -> true
            SourceScene.UNKNOWN -> false
        }

    fun allowsNativePadding(
        sourceScene: SourceScene,
        genericIslandShowing: Boolean?,
    ): Boolean =
        when (sourceScene) {
            SourceScene.HOME,
            SourceScene.KEYGUARD,
            -> true
            SourceScene.UNKNOWN -> false
        }

    data class ReservationSpan(
        val sourceLeft: Float,
        val sourceRight: Float,
        val targetLeft: Float,
        val targetRight: Float,
    )

    fun resolveReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
        progress: Float,
    ): Int {
        val compact = compactWidthPx.coerceAtLeast(0)
        if (compact == 0) return 0
        val p = geometryProgress(progress)
        var left = -compact.toFloat()
        var right = 0f
        spans.forEach { span ->
            val currentLeft =
                span.sourceLeft +
                    (span.targetLeft - span.sourceLeft) * p
            val currentRight =
                span.sourceRight +
                    (span.targetRight - span.sourceRight) * p
            left = min(left, currentLeft)
            right = maxOf(right, currentRight)
        }
        return kotlin.math.ceil((right - left).coerceAtLeast(compact.toFloat())).toInt()
    }

    fun resolveFinalReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
    ): Int =
        resolveReservationWidth(
            compactWidthPx = compactWidthPx,
            spans = spans,
            progress = 1f,
        )

    fun resolveTransitionReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
        progress: Float,
    ): Int {
        val compact = compactWidthPx.coerceAtLeast(0)
        if (compact == 0) return 0
        val finalWidth =
            resolveFinalReservationWidth(
                compactWidthPx = compact,
                spans = spans,
            )
        val p = geometryProgress(progress)
        return (
            compact +
                (finalWidth - compact) * p
            ).roundToInt().coerceAtLeast(compact)
    }

    fun batteryPeerReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
        semanticWidthPx: Int,
        progress: Float,
        targetEndOffsetPx: Float = 0f,
    ): Int {
        val compact = compactWidthPx.coerceAtLeast(0)
        val semantic = semanticWidthPx.coerceAtLeast(compact)
        if (compact == 0) return 0
        val targetEndOffset =
            targetEndOffsetPx
                .takeIf(Float::isFinite)
                ?: 0f
        val p = geometryProgress(progress)
        var left = -compact.toFloat()
        spans.forEach { span ->
            val currentLeft =
                span.sourceLeft +
                    (span.targetLeft + targetEndOffset - span.sourceLeft) * p
            left = min(left, currentLeft)
        }

        // statusIcons.paddingEnd reserves space only on the peer side of the
        // live QS_FAKE end edge (logical x=0). A projected Guiyuan span may
        // extend to x>0 while converging on the final QS surface; that
        // end-side extent is drawable occupancy, not native-peer intrusion.
        return kotlin.math
            .ceil((-left.coerceAtMost(0f)).coerceAtLeast(compact.toFloat()))
            .toInt()
            .coerceAtMost(semantic)
    }

    fun transitionTintProgress(progress: Float): Float {
        val ringPhase =
            BatteryRingTransition
                .transitionProgress(progress)
                .coerceIn(0f, 1f)
        return ringPhase * ringPhase * (3f - 2f * ringPhase)
    }

    fun resolveTransitionTint(
        source: Int,
        target: Int,
        progress: Float,
        tinted: Boolean,
        transitionEnabled: Boolean,
    ): Int =
        when {
            !tinted -> target
            !transitionEnabled -> source
            else ->
                interpolateColor(
                    source = source,
                    target = target,
                    progress = progress,
                )
        }

    fun interpolateColor(
        source: Int,
        target: Int,
        progress: Float,
    ): Int {
        val p = transitionTintProgress(progress)
        fun channel(
            fromShift: Int,
            toShift: Int = fromShift,
        ): Int {
            val from = source ushr fromShift and 0xff
            val to = target ushr toShift and 0xff
            return (from + (to - from) * p)
                .roundToInt()
                .coerceIn(0, 255)
        }
        return (
            channel(24) shl 24 or
                (channel(16) shl 16) or
                (channel(8) shl 8) or
                channel(0)
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

    fun composeSourceGeometry(
        positionAuthority: FloatArray,
        basisAuthority: FloatArray,
    ): FloatArray {
        require(positionAuthority.size == 6 && basisAuthority.size == 6)
        return floatArrayOf(
            positionAuthority[0],
            positionAuthority[1],
            basisAuthority[2],
            basisAuthority[3],
            basisAuthority[4],
            basisAuthority[5],
        )
    }

    fun endAnchoredMotionCarrierGeometry(
        carrierGeometry: FloatArray,
        carrierWidth: Int,
        carrierHeight: Int,
        logicalWidth: Int,
        isRtl: Boolean,
    ): FloatArray? =
        endAnchoredSlotGeometry(
            hostGeometry = carrierGeometry,
            hostWidth = carrierWidth,
            hostHeight = carrierHeight,
            slotWidth = logicalWidth,
            isRtl = isRtl,
        )

    fun endAnchoredSlotGeometry(
        hostGeometry: FloatArray,
        hostWidth: Int,
        hostHeight: Int,
        slotWidth: Int,
        isRtl: Boolean,
    ): FloatArray? {
        if (
            hostGeometry.size != 6 ||
            hostWidth <= 0 ||
            hostHeight <= 0 ||
            slotWidth <= 0 ||
            slotWidth > hostWidth
        ) {
            return null
        }
        val left = if (isRtl) 0f else (hostWidth - slotWidth).toFloat()
        val right = if (isRtl) slotWidth.toFloat() else hostWidth.toFloat()
        return componentGeometry(
            parentGeometry = hostGeometry,
            parentWidth = hostWidth,
            parentHeight = hostHeight,
            bounds =
                StatusPainter.TransitionBounds(
                    left = left,
                    top = 0f,
                    right = right,
                    bottom = hostHeight.toFloat(),
                ),
        )
    }

    fun interpolateCarriedToRoot(
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
            rebaseSourceToCurrentCarrier(
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

    fun interpolateCarriedToRootExact(
        source: FloatArray,
        target: FloatArray,
        sourceCarrier: FloatArray,
        currentCarrier: FloatArray,
        progress: Float,
    ): FloatArray {
        require(
            source.size == 6 &&
                target.size == 6 &&
                sourceCarrier.size == 6 &&
                currentCarrier.size == 6,
        )
        val carriedSource =
            rebaseSourceToCurrentCarrier(
                source = source,
                sourceCarrier = sourceCarrier,
                currentCarrier = currentCarrier,
            )
        return interpolateGeometry(
            source = carriedSource,
            target = target,
            progress = geometryProgress(progress),
        )
    }

    fun rebaseSourceToCurrentCarrier(
        source: FloatArray,
        sourceCarrier: FloatArray,
        currentCarrier: FloatArray,
    ): FloatArray {
        require(
            source.size == 6 &&
                sourceCarrier.size == 6 &&
                currentCarrier.size == 6,
        )
        return floatArrayOf(
            source[0] + (currentCarrier[0] - sourceCarrier[0]),
            source[1] + (currentCarrier[1] - sourceCarrier[1]),
            source[2],
            source[3],
            source[4],
            source[5],
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
                preferredChildEntries.any { entry ->
                    entry == "mobile_type_single" || entry == "mobile_type"
                } ->
                    StatusPainter.TransitionNormalizedBounds(
                        left = 0f,
                        top = 0f,
                        right = 0.42f,
                        bottom = 1f,
                    )

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

    fun scaleGeometry(
        source: FloatArray,
        scale: Float,
    ): FloatArray {
        require(source.size == 6)
        val normalized = scale.coerceAtLeast(0f)
        return floatArrayOf(
            source[0],
            source[1],
            source[2] * normalized,
            source[3] * normalized,
            source[4] * normalized,
            source[5] * normalized,
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

    fun followAnchorGeometry(
        follower: FloatArray,
        sourceAnchor: FloatArray,
        currentAnchor: FloatArray,
    ): FloatArray? {
        if (
            follower.size != 6 ||
            sourceAnchor.size != 6 ||
            currentAnchor.size != 6
        ) return null

        val swx = sourceAnchor[2]
        val swy = sourceAnchor[3]
        val shx = sourceAnchor[4]
        val shy = sourceAnchor[5]
        val determinant = swx * shy - shx * swy
        if (!determinant.isFinite() || kotlin.math.abs(determinant) < 0.0001f) {
            return null
        }

        fun mapVector(x: Float, y: Float): Pair<Float, Float> {
            val localX = (x * shy - shx * y) / determinant
            val localY = (swx * y - x * swy) / determinant
            return Pair(
                currentAnchor[2] * localX + currentAnchor[4] * localY,
                currentAnchor[3] * localX + currentAnchor[5] * localY,
            )
        }

        val centerDeltaX = follower[0] - sourceAnchor[0]
        val centerDeltaY = follower[1] - sourceAnchor[1]
        val mappedCenterDelta = mapVector(centerDeltaX, centerDeltaY)
        val mappedWidth = mapVector(follower[2], follower[3])
        val mappedHeight = mapVector(follower[4], follower[5])
        return floatArrayOf(
            currentAnchor[0] + mappedCenterDelta.first,
            currentAnchor[1] + mappedCenterDelta.second,
            mappedWidth.first,
            mappedWidth.second,
            mappedHeight.first,
            mappedHeight.second,
        )
    }

    fun componentGeometry(
        parentGeometry: FloatArray,
        parentWidth: Int,
        parentHeight: Int,
        bounds: StatusPainter.TransitionBounds,
    ): FloatArray? {
        if (
            parentGeometry.size != 6 ||
            parentWidth <= 0 ||
            parentHeight <= 0 ||
            bounds.width <= 0f ||
            bounds.height <= 0f
        ) {
            return null
        }
        val normalizedCenterX =
            bounds.centerX / parentWidth.toFloat() - 0.5f
        val normalizedCenterY =
            bounds.centerY / parentHeight.toFloat() - 0.5f
        val widthScale = bounds.width / parentWidth.toFloat()
        val heightScale = bounds.height / parentHeight.toFloat()
        return floatArrayOf(
            parentGeometry[0] +
                parentGeometry[2] * normalizedCenterX +
                parentGeometry[4] * normalizedCenterY,
            parentGeometry[1] +
                parentGeometry[3] * normalizedCenterX +
                parentGeometry[5] * normalizedCenterY,
            parentGeometry[2] * widthScale,
            parentGeometry[3] * widthScale,
            parentGeometry[4] * heightScale,
            parentGeometry[5] * heightScale,
        )
    }
}
