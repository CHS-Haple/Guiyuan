package com.chaners.guiyuan.xposed

internal fun transitionGeometry(
    centerX: Float = 0f,
    centerY: Float = 0f,
    width: Float,
    height: Float,
): FloatArray =
    floatArrayOf(
        centerX,
        centerY,
        width,
        0f,
        0f,
        height,
    )
