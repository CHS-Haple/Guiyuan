package com.chaners.guiyuan.xposed

internal object CcCarrierGeometryPolicy {
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

    fun interpolateCarriedSourceToRootTargetExact(
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
        return CcTransitionPolicy.interpolateGeometry(
            source = carriedSource,
            target = target,
            progress = CcTransitionPolicy.geometryProgress(progress),
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
