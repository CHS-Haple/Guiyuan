package com.chaners.guiyuan.xposed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.battery.BatteryArcSegments
import com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy
import com.chaners.guiyuan.xposed.battery.BatteryTopArcPolicy
import com.chaners.guiyuan.xposed.battery.batteryArcSegments
import com.chaners.guiyuan.xposed.battery.batteryTopBaseCenterY
import com.chaners.guiyuan.xposed.battery.batteryTopCenterY
import com.chaners.guiyuan.xposed.battery.batteryTopOverflowPx
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.WifiOpticalReference
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal class StatusPainter(
    private val context: Context,
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var airplaneDrawableResolved = false
    private var cachedAirplaneResourceId: Int = 0
    private val nativeCenterAssets =
        LinkedHashMap<PresentationStore.NativeIconResource, NativeCenterAsset>(
            NATIVE_CENTER_CACHE_SIZE,
            0.75f,
            true,
        )
    private val nativeTintVariantIds = HashMap<Int, Int>()
    private val nativeWifiReferenceIds = HashMap<Int, Int>()

    private var cachedMobileTypeWeight: Int = Int.MIN_VALUE
    private var cachedMobileTypeTypeface: Typeface = Typeface.DEFAULT
    private var cachedBatteryTopTextWeight: Int = Int.MIN_VALUE
    private var cachedBatteryTopTextTypeface: Typeface = Typeface.DEFAULT
    private val batteryTopTextBounds = Rect()
    private val mobileTypeMainBounds = Rect()
    private val mobileTypeSuffixBounds = Rect()
    private val batteryRing = RectF(10f, 8f, 110f, 108f)
    private var cachedCenterVisual: VisualCfg? = null
    private var cachedCenterGeometry: CenterGeometry.Resolved? = null
    private var cachedOuterWeightScale = Float.NaN
    private var cachedOuterGeometry =
        OuterGeometry.resolve(
            OuterGeometry.DEFAULT_WEIGHT_SCALE,
        )
    private val wifiPaths = arrayOf(
        wifiPathLow(),
        wifiPathMid(),
        wifiPathHigh(),
    )
    private val wifiFallbackOpticalComponents =
        wifiPaths.map { path ->
            RectF().also { bounds ->
                path.computeBounds(bounds, true)
            }
        }
    private val wifiFallbackOpticalBounds =
        RectF().also { combined ->
            wifiFallbackOpticalComponents.forEachIndexed { index, pathBounds ->
                if (index == 0) {
                    combined.set(pathBounds)
                } else {
                    combined.union(pathBounds)
                }
            }
        }

    fun clearNativeResources() {
        nativeCenterAssets.clear()
        nativeTintVariantIds.clear()
        nativeWifiReferenceIds.clear()
        airplaneDrawableResolved = false
        cachedAirplaneResourceId = 0
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        model: RenderModel,
        colors: RenderColors,
        opacity: Float,
        visual: VisualCfg = VisualCfg(),
        previousCenterIndicator: CenterIndicator? = null,
        centerExitAmount: Float = 0f,
        centerEnterAmount: Float = 1f,
        outerWeightScale: Float = visual.outerWeightScale,
        scaleMobileTypeWithCanvas: Boolean = false,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }

        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val save = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val outerGeometry = resolveOuterGeometry(outerWeightScale)
        val centerGeometry = resolveCenterGeometry(visual)
        drawBattery(
            canvas = canvas,
            model = model,
            batteryTint = colors.batteryTint,
            batteryTextTint = colors.batteryTextTint,
            chargingIconTint = colors.chargingIconTint,
            opacity = opacity,
            geometry = outerGeometry,
            centerGeometry = centerGeometry,
            visual = visual,
            nativeTransform = nativeTransform,
            scale = scale,
            scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
            previousCenterIndicator = previousCenterIndicator,
            centerExitAmount = centerExitAmount,
            centerEnterAmount = centerEnterAmount,
        )
        if (visual.layout == ContentLayout.BATTERY_CENTER) {
            resolveBatteryTopReadoutLayout(
                model = model,
                visual = visual,
                nativeTransform = nativeTransform,
            )?.let { readout ->
                drawBatteryTopReadout(
                    canvas = canvas,
                    layout = readout,
                    textTint = colors.batteryTextTint,
                    chargingIconTint = colors.chargingIconTint,
                    opacity = opacity,
                    nativeTransform = nativeTransform,
                )
            }
        }
        val centerSave = canvas.save()
        if (visual.layout == ContentLayout.BATTERY_CENTER) {
            canvas.translate(0f, networkTopTranslationY(visual))
        }
        drawCenterTransition(
            canvas = canvas,
            current = model.centerIndicator,
            previous = previousCenterIndicator,
            tint = colors.centerTint,
            opacity = opacity,
            scale = scale,
            exitAmount = centerExitAmount,
            enterAmount = centerEnterAmount,
            geometry = centerGeometry,
            nativeTransform = nativeTransform,
            scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
        )
        canvas.restoreToCount(centerSave)
        drawMobile(
            canvas = canvas,
            model = model,
            tint = colors.mobileTint,
            opacity = opacity,
            geometry = outerGeometry,
        )
        canvas.restoreToCount(save)
    }

    fun requiredTopOverflowPx(
        width: Int,
        height: Int,
        model: RenderModel,
        visual: VisualCfg,
        previousCenterIndicator: CenterIndicator? = null,
        centerExitAmount: Float = 0f,
        centerEnterAmount: Float = 1f,
        scaleMobileTypeWithCanvas: Boolean = false,
    ): Int {
        if (width <= 0 || height <= 0) return 0
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return 0
        val scale = nativeTransform.scale
        val offsetY = nativeTransform.offsetY
        val topBounds =
            if (visual.layout == ContentLayout.BATTERY_CENTER) {
                val geometry =
                    resolveCenterGeometry(visual)
                resolveNetworkTopSlotAvoidance(
                    visual = visual,
                    current = model.centerIndicator,
                    previous = previousCenterIndicator,
                    scale = scale,
                    geometry = geometry,
                    scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
                    exitAmount = centerExitAmount,
                    enterAmount = centerEnterAmount,
                )?.bounds
            } else {
                resolveBatteryTopReadoutLayout(
                    model = model,
                    visual = visual,
                    nativeTransform = nativeTransform,
                )?.groupOpticalBounds
            } ?: return 0
        return batteryTopOverflowPx(
            transformScale = scale,
            transformOffsetY = offsetY,
            contentTopY = topBounds.top,
        )
    }

    fun drawTransitionComponent(
        canvas: Canvas,
        width: Int,
        height: Int,
        model: RenderModel,
        colors: RenderColors,
        component: TransitionComponent,
        shapePolicy: TransitionShapePolicy,
        opacity: Float = 1f,
        visual: VisualCfg = VisualCfg(),
        motionProgress: Float = 0f,
        shapeProgress: Float = 0f,
        mobileTargetWidthRatio: Float? = null,
        mobileTargetHeightRatio: Float? = null,
        mobileTargetBars: List<TransitionNormalizedBounds>? = null,
        batteryNumberTargetWeight: Int? = null,
        batteryNumberTargetStyle: TransitionTextStyle? = null,
        centerTargetTextWeight: Int? = null,
        centerTargetTextStyle: TransitionTextStyle? = null,
        batteryRingExitDirection: BatteryRingTransitionPolicy.ExitDirection =
            BatteryRingTransitionPolicy.ExitDirection.NONE,
    ) {
        if (width <= 0 || height <= 0 || opacity <= 0f) return

        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val save = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val motion = motionProgress.coerceIn(0f, 1f)
        val shape = shapeProgress.coerceIn(0f, 1f)
        val componentSave = canvas.save()

        when (component) {
            TransitionComponent.BATTERY ->
                drawBattery(
                    canvas = canvas,
                    model = model,
                    batteryTint = colors.batteryTint,
                    batteryTextTint = colors.batteryTextTint,
                    chargingIconTint = colors.chargingIconTint,
                    opacity = opacity,
                    geometry =
                        resolveOuterGeometry(
                            visual.outerWeightScale,
                        ),
                    centerGeometry =
                        resolveCenterGeometry(visual),
                    visual = visual,
                    nativeTransform = nativeTransform,
                    scale = scale,
                    scaleMobileTypeWithCanvas = false,
                    drawReadoutText = false,
                    drawReadoutChargingIcon = false,
                    ringRetractProgress =
                        if (shapePolicy == TransitionShapePolicy.BATTERY_RETRACT) {
                            BatteryRingTransitionPolicy.transitionProgress(shape)
                        } else {
                            null
                        },
                    ringRetractExitDirection = batteryRingExitDirection,
                )

            TransitionComponent.BATTERY_NUMBER ->
                drawBatteryTopNumberTransition(
                    canvas = canvas,
                    model = model,
                    textTint = colors.batteryTextTint,
                    opacity = opacity,
                    visual = visual,
                    motionProgress = motion,
                    targetWeight = batteryNumberTargetWeight,
                    targetStyle = batteryNumberTargetStyle,
                    nativeTransform = nativeTransform,
                )

            TransitionComponent.CHARGING_ICON ->
                drawBatteryTopChargingIconTransition(
                    canvas = canvas,
                    model = model,
                    chargingIconTint = colors.chargingIconTint,
                    opacity = opacity,
                    visual = visual,
                    nativeTransform = nativeTransform,
                )

            TransitionComponent.CENTER -> {
                if (visual.layout == ContentLayout.BATTERY_CENTER) {
                    canvas.translate(0f, networkTopTranslationY(visual))
                }
                val baseGeometry =
                    resolveCenterGeometry(visual)
                val transitionGeometry =
                    if (model.centerIndicator is CenterIndicator.MobileType) {
                        baseGeometry.copy(
                            mobileTypeWeight =
                                MobileTypeTransitionPolicy.resolveWeight(
                                    sourceWeight = baseGeometry.mobileTypeWeight,
                                    targetWeight = centerTargetTextWeight,
                                    progress = motion,
                                ),
                        )
                    } else {
                        baseGeometry
                    }
                drawCenterIndicator(
                    canvas = canvas,
                    indicator = model.centerIndicator,
                    tint = colors.centerTint,
                    opacity = opacity,
                    scale = scale,
                    appearAmount = 1f,
                    geometry = transitionGeometry,
                    nativeTransform = nativeTransform,
                    scaleMobileTypeWithCanvas = false,
                    mobileTypeTargetStyle = centerTargetTextStyle,
                    mobileTypeTransitionProgress = motion,
                )
            }

            TransitionComponent.MOBILE -> {
                val outerGeometry =
                    resolveOuterGeometry(
                        visual.outerWeightScale,
                    )
                if (shapePolicy == TransitionShapePolicy.MOBILE_SIGNAL) {
                    drawMobileSignalTransition(
                        canvas = canvas,
                        model = model,
                        tint = colors.mobileTint,
                        opacity = opacity,
                        geometry = outerGeometry,
                        motionProgress = motion,
                        shapeProgress = shape,
                        targetWidthRatio = mobileTargetWidthRatio,
                        targetHeightRatio = mobileTargetHeightRatio,
                        targetBars = mobileTargetBars,
                    )
                } else {
                    drawMobile(
                        canvas = canvas,
                        model = model,
                        tint = colors.mobileTint,
                        opacity = opacity,
                        geometry = outerGeometry,
                    )
                }
            }
        }
        canvas.restoreToCount(componentSave)

        canvas.restoreToCount(save)
    }





    internal enum class TransitionComponent {
        BATTERY,
        BATTERY_NUMBER,
        CHARGING_ICON,
        CENTER,
        MOBILE,
    }

    internal enum class TransitionShapePolicy {
        BATTERY_RETRACT,
        RIGID,
        MOBILE_SIGNAL,
    }

    internal enum class TransitionScalePolicy {
        TARGET,
        SHRINK_ONLY,
    }

    internal sealed interface TransitionTarget {
        data object BatteryIcon : TransitionTarget
        data object BatteryNumber : TransitionTarget
        data object BatteryChargingIcon : TransitionTarget

        data class Slots(
            val preferredSlots: List<String>,
            val preferredChildEntries: List<String> = emptyList(),
        ) : TransitionTarget
    }

    internal data class TransitionBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float
            get() = (right - left).coerceAtLeast(0f)

        val height: Float
            get() = (bottom - top).coerceAtLeast(0f)

        val centerX: Float
            get() = (left + right) / 2f

        val centerY: Float
            get() = (top + bottom) / 2f
    }

    internal data class TransitionComponentSpec(
        val component: TransitionComponent,
        val sourceBounds: TransitionBounds,
        val target: TransitionTarget,
        val shapePolicy: TransitionShapePolicy,
        val scalePolicy: TransitionScalePolicy,
        val targetOpticalBounds: TransitionNormalizedBounds? = null,
    )

    internal data class TransitionNormalizedBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    internal data class TransitionTextStyle(
        val typeface: Typeface?,
        val weight: Int?,
        val fakeBoldText: Boolean,
        val textScaleX: Float,
        val textSkewX: Float,
        val letterSpacing: Float,
        val strokeWidth: Float,
        val paintStyle: Paint.Style,
    )

    fun transitionComponentSpecs(
        width: Int,
        height: Int,
        model: RenderModel,
        visual: VisualCfg = VisualCfg(),
    ): List<TransitionComponentSpec> {
        if (width <= 0 || height <= 0) return emptyList()

        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return emptyList()
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val outerGeometry =
            resolveOuterGeometry(visual.outerWeightScale)
        val centerGeometry =
            resolveCenterGeometry(visual)

        fun toViewBounds(bounds: TransitionBounds): TransitionBounds =
            TransitionBounds(
                left = offsetX + bounds.left * scale,
                top = offsetY + bounds.top * scale,
                right = offsetX + bounds.right * scale,
                bottom = offsetY + bounds.bottom * scale,
            )

        val specs = ArrayList<TransitionComponentSpec>(5)
        val batteryHalfStroke = outerGeometry.ringStroke / 2f
        specs +=
            TransitionComponentSpec(
                component = TransitionComponent.BATTERY,
                sourceBounds =
                    toViewBounds(
                        TransitionBounds(
                            left = batteryRing.left - batteryHalfStroke,
                            top = batteryRing.top - batteryHalfStroke,
                            right = batteryRing.right + batteryHalfStroke,
                            bottom = batteryRing.bottom + batteryHalfStroke,
                        ),
                    ),
                target = TransitionTarget.BatteryIcon,
                shapePolicy = TransitionShapePolicy.BATTERY_RETRACT,
                scalePolicy = TransitionScalePolicy.TARGET,
            )

        resolveBatteryTopReadoutLayout(
            model = model,
            visual = visual,
            nativeTransform = nativeTransform,
        )?.let { readout ->
            if (readout.textVisible) {
                specs +=
                    TransitionComponentSpec(
                        component = TransitionComponent.BATTERY_NUMBER,
                        sourceBounds = toViewBounds(readout.textOpticalBounds),
                        target = TransitionTarget.BatteryNumber,
                        shapePolicy = TransitionShapePolicy.RIGID,
                        scalePolicy = TransitionScalePolicy.TARGET,
                    )
            }
            if (
                readout.chargingIconResourceId != null &&
                readout.chargingIconOpticalBounds.width > 0f &&
                readout.chargingIconOpticalBounds.height > 0f
            ) {
                specs +=
                    TransitionComponentSpec(
                        component = TransitionComponent.CHARGING_ICON,
                        sourceBounds = toViewBounds(readout.chargingIconOpticalBounds),
                        target = TransitionTarget.BatteryChargingIcon,
                        shapePolicy = TransitionShapePolicy.RIGID,
                        scalePolicy = TransitionScalePolicy.TARGET,
                    )
            }
        }

        val centerSpec =
            when (model.centerIndicator) {
                is CenterIndicator.Wifi -> {
                    val metrics =
                        transitionWifiMetrics(
                            indicator = model.centerIndicator,
                            geometry = centerGeometry,
                        )
                    TransitionComponentSpec(
                        component = TransitionComponent.CENTER,
                        sourceBounds =
                            toViewBounds(
                                centeredBounds(
                                    centerX = WIFI_CENTER_X,
                                    centerY = WIFI_CENTER_Y,
                                    width =
                                        metrics?.sourceOpticalWidth
                                            ?: centerGeometry.wifiMaxWidth,
                                    height =
                                        metrics?.sourceOpticalHeight
                                            ?: centerGeometry.wifiMaxHeight,
                                ),
                            ),
                        target =
                            TransitionTarget.Slots(
                                preferredSlots = listOf("wifi"),
                                preferredChildEntries = listOf("wifi_signal"),
                            ),
                        shapePolicy = TransitionShapePolicy.RIGID,
                        scalePolicy = TransitionScalePolicy.TARGET,
                        targetOpticalBounds = metrics?.targetOpticalBounds,
                    )
                }

                is CenterIndicator.MobileType ->
                    TransitionComponentSpec(
                        component = TransitionComponent.CENTER,
                        sourceBounds =
                            toViewBounds(
                                resolveMobileTypeLayout(
                                    indicator = model.centerIndicator,
                                    scale = scale,
                                    geometry = centerGeometry,
                                    scaleWithCanvas = false,
                                ).bounds,
                            ),
                        target =
                            TransitionTarget.Slots(
                                preferredSlots = listOf("mobile", "stacked_mobile"),
                                preferredChildEntries =
                                    listOf("mobile_type_single", "mobile_type"),
                            ),
                        shapePolicy = TransitionShapePolicy.RIGID,
                        scalePolicy = TransitionScalePolicy.TARGET,
                    )

                CenterIndicator.Airplane -> {
                    val metrics =
                        airplaneResourceId()
                            ?.let { resourceId ->
                                transitionNativeCenterMetrics(
                                    resource =
                                        PresentationStore.NativeIconResource(
                                            packageName = SYSTEM_UI_PACKAGE,
                                            resourceId = resourceId,
                                        ),
                                    maxWidth = centerGeometry.airplaneMaxSize,
                                    maxHeight = centerGeometry.airplaneMaxSize,
                                )
                            }
                    TransitionComponentSpec(
                        component = TransitionComponent.CENTER,
                        sourceBounds =
                            toViewBounds(
                                centeredBounds(
                                    centerX = AIRPLANE_CENTER_X,
                                    centerY = AIRPLANE_CENTER_Y,
                                    width =
                                        metrics?.sourceOpticalWidth
                                            ?: centerGeometry.airplaneMaxSize,
                                    height =
                                        metrics?.sourceOpticalHeight
                                            ?: centerGeometry.airplaneMaxSize,
                                ),
                            ),
                        target = TransitionTarget.Slots(listOf("airplane")),
                        shapePolicy = TransitionShapePolicy.RIGID,
                        scalePolicy = TransitionScalePolicy.TARGET,
                        targetOpticalBounds = metrics?.targetOpticalBounds,
                    )
                }

                is CenterIndicator.NoSim -> {
                    val metrics =
                        transitionNativeCenterMetrics(
                            resource = model.centerIndicator.nativeResource,
                            maxWidth = centerGeometry.noSimMaxSize,
                            maxHeight = centerGeometry.noSimMaxSize,
                        )
                    TransitionComponentSpec(
                        component = TransitionComponent.CENTER,
                        sourceBounds =
                            toViewBounds(
                                centeredBounds(
                                    centerX = CENTER_TRANSITION_PIVOT_X,
                                    centerY = CENTER_TRANSITION_PIVOT_Y,
                                    width =
                                        metrics?.sourceOpticalWidth
                                            ?: centerGeometry.noSimMaxSize,
                                    height =
                                        metrics?.sourceOpticalHeight
                                            ?: centerGeometry.noSimMaxSize,
                                ),
                            ),
                        target =
                            TransitionTarget.Slots(
                                listOf("no_sim", "mobile", "stacked_mobile"),
                            ),
                        shapePolicy = TransitionShapePolicy.RIGID,
                        scalePolicy = TransitionScalePolicy.TARGET,
                        targetOpticalBounds = metrics?.targetOpticalBounds,
                    )
                }

                CenterIndicator.Empty -> null
            }
        centerSpec?.let { spec ->
            specs +=
                if (visual.layout == ContentLayout.BATTERY_CENTER) {
                    spec.copy(
                        sourceBounds =
                            shiftBoundsY(
                                spec.sourceBounds,
                                networkTopTranslationY(visual) * scale,
                            ),
                    )
                } else {
                    spec
                }
        }

        val mobileLayout =
            resolveMobileSignalTransitionLayout(
                geometry = outerGeometry,
                model = model,
            )
        specs +=
            TransitionComponentSpec(
                component = TransitionComponent.MOBILE,
                sourceBounds = toViewBounds(mobileLayout.bounds),
                target =
                    TransitionTarget.Slots(
                        preferredSlots = listOf("mobile", "stacked_mobile"),
                        preferredChildEntries = listOf("mobile_signal"),
                    ),
                shapePolicy =
                    if (model.mobileUnavailableMark) {
                        TransitionShapePolicy.RIGID
                    } else {
                        TransitionShapePolicy.MOBILE_SIGNAL
                    },
                scalePolicy = TransitionScalePolicy.SHRINK_ONLY,
            )

        return specs
    }

    fun transitionAirplaneSourceBounds(
        width: Int,
        height: Int,
        visual: VisualCfg = VisualCfg(),
    ): TransitionBounds? {
        if (width <= 0 || height <= 0) return null
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return null
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val geometry =
            resolveCenterGeometry(visual)
        val metrics =
            airplaneResourceId()
                ?.let { resourceId ->
                    transitionNativeCenterMetrics(
                        resource =
                            PresentationStore.NativeIconResource(
                                packageName = SYSTEM_UI_PACKAGE,
                                resourceId = resourceId,
                            ),
                        maxWidth = geometry.airplaneMaxSize,
                        maxHeight = geometry.airplaneMaxSize,
                    )
                }
        val baseLocal =
            centeredBounds(
                centerX = AIRPLANE_CENTER_X,
                centerY = AIRPLANE_CENTER_Y,
                width = metrics?.sourceOpticalWidth ?: geometry.airplaneMaxSize,
                height = metrics?.sourceOpticalHeight ?: geometry.airplaneMaxSize,
            )
        val local =
            if (visual.layout == ContentLayout.BATTERY_CENTER) {
                shiftBoundsY(baseLocal, networkTopTranslationY(visual))
            } else {
                baseLocal
            }
        return TransitionBounds(
            left = offsetX + local.left * scale,
            top = offsetY + local.top * scale,
            right = offsetX + local.right * scale,
            bottom = offsetY + local.bottom * scale,
        )
    }

    fun drawTransitionAirplane(
        canvas: Canvas,
        width: Int,
        height: Int,
        tint: Int,
        opacity: Float,
        visual: VisualCfg = VisualCfg(),
    ) {
        if (width <= 0 || height <= 0 || opacity <= 0f) return
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val geometry =
            resolveCenterGeometry(visual)
        val save = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        if (visual.layout == ContentLayout.BATTERY_CENTER) {
            canvas.translate(0f, networkTopTranslationY(visual))
        }
        drawNativeAirplane(
            canvas = canvas,
            tint = tint,
            opacity = opacity,
            geometry = geometry,
            nativeTransform = nativeTransform,
            pixelAligned = false,
        )
        canvas.restoreToCount(save)
    }

    fun transitionNoSimSourceBounds(
        width: Int,
        height: Int,
        resource: PresentationStore.NativeIconResource,
        visual: VisualCfg = VisualCfg(),
    ): TransitionBounds? {
        if (width <= 0 || height <= 0) return null
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return null
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val geometry =
            resolveCenterGeometry(visual)
        val metrics =
            transitionNativeCenterMetrics(
                resource = resource,
                maxWidth = geometry.noSimMaxSize,
                maxHeight = geometry.noSimMaxSize,
            )
        val baseLocal =
            centeredBounds(
                centerX = CENTER_TRANSITION_PIVOT_X,
                centerY = CENTER_TRANSITION_PIVOT_Y,
                width = metrics?.sourceOpticalWidth ?: geometry.noSimMaxSize,
                height = metrics?.sourceOpticalHeight ?: geometry.noSimMaxSize,
            )
        val local =
            if (visual.layout == ContentLayout.BATTERY_CENTER) {
                shiftBoundsY(baseLocal, networkTopTranslationY(visual))
            } else {
                baseLocal
            }
        return TransitionBounds(
            left = offsetX + local.left * scale,
            top = offsetY + local.top * scale,
            right = offsetX + local.right * scale,
            bottom = offsetY + local.bottom * scale,
        )
    }

    fun drawTransitionNoSim(
        canvas: Canvas,
        width: Int,
        height: Int,
        resource: PresentationStore.NativeIconResource,
        tint: Int,
        opacity: Float,
        visual: VisualCfg = VisualCfg(),
    ) {
        if (width <= 0 || height <= 0 || opacity <= 0f) return
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return
        val scale = nativeTransform.scale
        val offsetX = nativeTransform.offsetX
        val offsetY = nativeTransform.offsetY
        val geometry =
            resolveCenterGeometry(visual)
        val save = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        if (visual.layout == ContentLayout.BATTERY_CENTER) {
            canvas.translate(0f, networkTopTranslationY(visual))
        }
        drawNativeCenterResource(
            canvas = canvas,
            resource = resource,
            tint = tint,
            opacity = opacity,
            centerX = CENTER_TRANSITION_PIVOT_X,
            centerY = CENTER_TRANSITION_PIVOT_Y,
            maxWidth = geometry.noSimMaxSize,
            maxHeight = geometry.noSimMaxSize,
            nativeTransform = nativeTransform,
            pixelAligned = false,
        )
        canvas.restoreToCount(save)
    }

    private fun transitionWifiMetrics(
        indicator: CenterIndicator.Wifi,
        geometry: CenterGeometry.Resolved,
    ): TransitionWifiMetrics? {
        val resourceId = indicator.nativeResourceId ?: return null
        val resource =
            PresentationStore.NativeIconResource(
                packageName = SYSTEM_UI_PACKAGE,
                resourceId = resourceId,
            )
        return transitionNativeCenterMetrics(
            resource = resource,
            opticalReferenceResource = wifiOpticalReferenceResource(resource),
            maxWidth = geometry.wifiMaxWidth,
            maxHeight = geometry.wifiMaxHeight,
        )
    }

    private fun transitionNativeCenterMetrics(
        resource: PresentationStore.NativeIconResource,
        opticalReferenceResource: PresentationStore.NativeIconResource? = null,
        maxWidth: Float,
        maxHeight: Float,
    ): TransitionWifiMetrics? {
        val presentationResource = resolveNativeTintVariant(resource) ?: resource
        val asset = nativeCenterAsset(presentationResource) ?: return null
        val referenceAsset =
            opticalReferenceResource
                ?.let { reference ->
                    val presentationReference =
                        resolveNativeTintVariant(reference) ?: reference
                    nativeCenterAsset(presentationReference)
                }
                ?.takeIf { reference ->
                    WifiOpticalReference.canShareReferenceViewport(
                        currentWidth = asset.intrinsicWidth,
                        currentHeight = asset.intrinsicHeight,
                        referenceWidth = reference.intrinsicWidth,
                        referenceHeight = reference.intrinsicHeight,
                    )
                }
        val fitAsset = referenceAsset ?: asset
        val optical = fitAsset.opticalBounds
        val opticalWidthRatio =
            (optical.right - optical.left).coerceAtLeast(MIN_OPTICAL_RATIO)
        val opticalHeightRatio =
            (optical.bottom - optical.top).coerceAtLeast(MIN_OPTICAL_RATIO)
        val opticalIntrinsicWidth = fitAsset.intrinsicWidth * opticalWidthRatio
        val opticalIntrinsicHeight = fitAsset.intrinsicHeight * opticalHeightRatio
        val drawableScale =
            min(
                maxWidth / opticalIntrinsicWidth,
                maxHeight / opticalIntrinsicHeight,
            )
        return TransitionWifiMetrics(
            sourceOpticalWidth = opticalIntrinsicWidth * drawableScale,
            sourceOpticalHeight = opticalIntrinsicHeight * drawableScale,
            targetOpticalBounds =
                TransitionNormalizedBounds(
                    left = optical.left,
                    top = optical.top,
                    right = optical.right,
                    bottom = optical.bottom,
                ),
        )
    }

    private fun centeredBounds(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
    ): TransitionBounds =
        TransitionBounds(
            left = centerX - width / 2f,
            top = centerY - height / 2f,
            right = centerX + width / 2f,
            bottom = centerY + height / 2f,
        )

    private fun lerp(
        start: Float,
        end: Float,
        progress: Float,
    ): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)

    private fun resolveCanvasTransform(
        width: Int,
        height: Int,
        visual: VisualCfg,
    ): NativeRenderTransform? {
        if (width <= 0 || height <= 0) return null
        val combinedScale =
            visual.combinedScale
                .takeIf(Float::isFinite)
                ?.coerceIn(COMBINED_SCALE_MIN, COMBINED_SCALE_MAX)
                ?: COMBINED_SCALE_DEFAULT
        val scale =
            min(width / CANONICAL_SIZE, height / CANONICAL_SIZE) *
                combinedScale
        if (!scale.isFinite() || scale <= 0f) return null
        val visualWidth = CANONICAL_SIZE * scale
        val visualHeight = CANONICAL_SIZE * scale
        return NativeRenderTransform(
            scale = scale,
            offsetX = (width - visualWidth) / 2f,
            offsetY = (height - visualHeight) / 2f,
        )
    }

    private fun resolveCenterGeometry(visual: VisualCfg): CenterGeometry.Resolved {
        if (cachedCenterVisual === visual) {
            cachedCenterGeometry?.let { return it }
        }
        return CenterGeometry.resolve(
            wifiScale = visual.wifiScale,
            mobileTypeScale = visual.mobileTypeScale,
            airplaneScale = visual.airplaneScale,
            noSimScale = visual.noSimScale,
            mobileTypeWeight = visual.mobileTypeWeight,
            combinedScale = visual.combinedScale,
        ).also { resolved ->
            cachedCenterVisual = visual
            cachedCenterGeometry = resolved
        }
    }

    private fun resolveOuterGeometry(weightScale: Float): OuterGeometry.Resolved {
        val normalized =
            OuterGeometry.normalizeWeightScale(weightScale)
        if (cachedOuterWeightScale != normalized) {
            cachedOuterWeightScale = normalized
            cachedOuterGeometry = OuterGeometry.resolve(normalized)
        }
        return cachedOuterGeometry
    }

    private fun networkTopTranslationY(
        visual: VisualCfg,
    ): Float =
        TOP_SLOT_CENTER_Y -
            CENTER_TRANSITION_PIVOT_Y +
            TopInfoOffsetPolicy.networkTranslationDelta(
                layout = visual.layout,
                rawOffset = visual.topOffsetY,
            )

    private fun shiftBoundsY(
        bounds: TransitionBounds,
        deltaY: Float,
    ): TransitionBounds =
        bounds.copy(
            top = bounds.top + deltaY,
            bottom = bounds.bottom + deltaY,
        )

    private fun resolveNetworkTopSlotAvoidance(
        visual: VisualCfg,
        current: CenterIndicator,
        previous: CenterIndicator?,
        scale: Float,
        geometry: CenterGeometry.Resolved,
        scaleMobileTypeWithCanvas: Boolean,
        exitAmount: Float,
        enterAmount: Float,
    ): TopSlotAvoidance? {
        val translationY = networkTopTranslationY(visual)
        if (previous == null || previous == current) {
            return resolveCenterIndicatorAvoidance(
                indicator = current,
                scale = scale,
                geometry = geometry,
                scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
            )?.let { avoidance ->
                shiftAvoidanceY(avoidance, translationY)
            }
        }

        val previousAvoidance =
            resolveCenterIndicatorAvoidance(
                indicator = previous,
                scale = scale,
                geometry = geometry,
                scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
            )?.let { avoidance ->
                scaleAvoidanceAroundPivot(
                    avoidance = avoidance,
                    pivotX = CENTER_TRANSITION_PIVOT_X,
                    pivotY = CENTER_TRANSITION_PIVOT_Y,
                    amount = exitAmount,
                )
            }?.let { avoidance ->
                shiftAvoidanceY(avoidance, translationY)
            }
        val currentAvoidance =
            resolveCenterIndicatorAvoidance(
                indicator = current,
                scale = scale,
                geometry = geometry,
                scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
            )?.let { avoidance ->
                scaleAvoidanceAroundPivot(
                    avoidance = avoidance,
                    pivotX = CENTER_TRANSITION_PIVOT_X,
                    pivotY = CENTER_TRANSITION_PIVOT_Y,
                    amount = enterAmount,
                )
            }?.let { avoidance ->
                shiftAvoidanceY(avoidance, translationY)
            }
        return unionAvoidance(previousAvoidance, currentAvoidance)
    }

    private fun shiftAvoidanceY(
        avoidance: TopSlotAvoidance,
        deltaY: Float,
    ): TopSlotAvoidance =
        TopSlotAvoidance(
            bounds = shiftBoundsY(avoidance.bounds, deltaY),
            components =
                avoidance.components.map { component ->
                    shiftBoundsY(component, deltaY)
                },
        )

    private fun scaleAvoidanceAroundPivot(
        avoidance: TopSlotAvoidance,
        pivotX: Float,
        pivotY: Float,
        amount: Float,
    ): TopSlotAvoidance? {
        val bounds =
            scaleBoundsAroundPivot(
                bounds = avoidance.bounds,
                pivotX = pivotX,
                pivotY = pivotY,
                amount = amount,
            ) ?: return null
        val components =
            avoidance.components.mapNotNull { component ->
                scaleBoundsAroundPivot(
                    bounds = component,
                    pivotX = pivotX,
                    pivotY = pivotY,
                    amount = amount,
                )
            }
        return TopSlotAvoidance(
            bounds = bounds,
            components = components.ifEmpty { listOf(bounds) },
        )
    }

    private fun unionAvoidance(
        first: TopSlotAvoidance?,
        second: TopSlotAvoidance?,
    ): TopSlotAvoidance? =
        when {
            first == null -> second
            second == null -> first
            else ->
                TopSlotAvoidance(
                    bounds = unionBounds(first.bounds, second.bounds)!!,
                    components = first.components + second.components,
                )
        }

    private fun scaleBoundsAroundPivot(
        bounds: TransitionBounds,
        pivotX: Float,
        pivotY: Float,
        amount: Float,
    ): TransitionBounds? {
        val resolved = amount.coerceIn(0f, 1f)
        if (resolved <= 0f) return null
        return TransitionBounds(
            left = pivotX + (bounds.left - pivotX) * resolved,
            top = pivotY + (bounds.top - pivotY) * resolved,
            right = pivotX + (bounds.right - pivotX) * resolved,
            bottom = pivotY + (bounds.bottom - pivotY) * resolved,
        )
    }

    private fun unionBounds(
        first: TransitionBounds?,
        second: TransitionBounds?,
    ): TransitionBounds? =
        when {
            first == null -> second
            second == null -> first
            else ->
                TransitionBounds(
                    left = min(first.left, second.left),
                    top = min(first.top, second.top),
                    right = max(first.right, second.right),
                    bottom = max(first.bottom, second.bottom),
                )
        }

    private fun resolveCenterIndicatorAvoidance(
        indicator: CenterIndicator,
        scale: Float,
        geometry: CenterGeometry.Resolved,
        scaleMobileTypeWithCanvas: Boolean,
    ): TopSlotAvoidance? =
        when (indicator) {
            is CenterIndicator.Wifi -> {
                val resourceId = indicator.nativeResourceId
                val drawGeometry =
                    if (resourceId != null) {
                        val resource =
                            PresentationStore.NativeIconResource(
                                packageName = SYSTEM_UI_PACKAGE,
                                resourceId = resourceId,
                            )
                        resolveNativeCenterDrawGeometry(
                            resource = resource,
                            opticalReferenceResource = wifiOpticalReferenceResource(resource),
                            centerX = WIFI_CENTER_X,
                            centerY = WIFI_CENTER_Y,
                            maxWidth = geometry.wifiMaxWidth,
                            maxHeight = geometry.wifiMaxHeight,
                        )
                    } else {
                        null
                    }
                drawGeometry?.let { resolved ->
                    TopSlotAvoidance(
                        bounds = resolved.opticalBounds,
                        components =
                            resolved.opticalComponents()
                                .ifEmpty { listOf(resolved.opticalBounds) },
                    )
                } ?: resolveWifiFallbackAvoidance(geometry)
            }

            is CenterIndicator.MobileType ->
                singleTopSlotAvoidance(
                    resolveMobileTypeLayout(
                        indicator = indicator,
                        scale = scale,
                        geometry = geometry,
                        scaleWithCanvas = scaleMobileTypeWithCanvas,
                    ).bounds,
                )

            CenterIndicator.Airplane ->
                airplaneResourceId()
                    ?.let { resourceId ->
                        resolveNativeCenterDrawGeometry(
                            resource =
                                PresentationStore.NativeIconResource(
                                    packageName = SYSTEM_UI_PACKAGE,
                                    resourceId = resourceId,
                                ),
                            centerX = AIRPLANE_CENTER_X,
                            centerY = AIRPLANE_CENTER_Y,
                            maxWidth = geometry.airplaneMaxSize,
                            maxHeight = geometry.airplaneMaxSize,
                        )?.opticalBounds
                    }?.let(::singleTopSlotAvoidance)

            is CenterIndicator.NoSim ->
                resolveNativeCenterDrawGeometry(
                    resource = indicator.nativeResource,
                    centerX = CENTER_TRANSITION_PIVOT_X,
                    centerY = CENTER_TRANSITION_PIVOT_Y,
                    maxWidth = geometry.noSimMaxSize,
                    maxHeight = geometry.noSimMaxSize,
                )?.opticalBounds
                    ?.let(::singleTopSlotAvoidance)

            CenterIndicator.Empty -> null
        }

    private fun singleTopSlotAvoidance(
        bounds: TransitionBounds,
    ): TopSlotAvoidance =
        TopSlotAvoidance(
            bounds = bounds,
            components = listOf(bounds),
        )

    private fun resolveWifiFallbackAvoidance(
        geometry: CenterGeometry.Resolved,
    ): TopSlotAvoidance {
        val scale = 3f * geometry.wifiScale

        fun map(bounds: RectF): TransitionBounds =
            TransitionBounds(
                left =
                    WIFI_CENTER_X +
                        (bounds.left - WIFI_FALLBACK_CENTER_X) * scale,
                top =
                    WIFI_CENTER_Y +
                        (bounds.top - WIFI_FALLBACK_CENTER_Y) * scale,
                right =
                    WIFI_CENTER_X +
                        (bounds.right - WIFI_FALLBACK_CENTER_X) * scale,
                bottom =
                    WIFI_CENTER_Y +
                        (bounds.bottom - WIFI_FALLBACK_CENTER_Y) * scale,
            )

        val bounds = map(wifiFallbackOpticalBounds)
        val components =
            wifiFallbackOpticalComponents.map(::map)
        return TopSlotAvoidance(
            bounds = bounds,
            components = components.ifEmpty { listOf(bounds) },
        )
    }

    private fun resolveBatteryTopGap(
        avoidance: TopSlotAvoidance,
        ringStroke: Float,
    ): BatteryTopArcPolicy.Gap {
        // Preserve disconnected visible shapes (notably Wi-Fi arcs) instead of
        // reserving the empty corners of their union rectangle.
        val gaps =
            avoidance.components
                .ifEmpty { listOf(avoidance.bounds) }
                .map { component ->
                    BatteryTopArcPolicy.resolveGap(
                        contentLeft = component.left,
                        contentTop = component.top,
                        contentRight = component.right,
                        contentBottom = component.bottom,
                        ringCenterX = batteryRing.centerX(),
                        ringCenterY = batteryRing.centerY(),
                        ringRadius = batteryRing.width() / 2f,
                        ringStroke = ringStroke,
                        visualClearance = BATTERY_TOP_RING_VISUAL_CLEARANCE,
                        startDegrees = BATTERY_START_DEGREES,
                        maxSweep = BATTERY_MAX_SWEEP,
                    )
                }
        return BatteryTopArcPolicy.mergeGaps(gaps)
    }

    private fun batteryReadoutPreferredCenterY(
        visual: VisualCfg,
    ): Float =
        if (visual.layout == ContentLayout.BATTERY_CENTER) {
            BATTERY_COMPONENT_CENTER_Y +
                BATTERY_TOP_DEFAULT_OPTICAL_RISE +
                BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
        } else {
            BATTERY_TOP_CONTENT_CENTER_Y
        }

    private fun drawBattery(
        canvas: Canvas,
        model: RenderModel,
        batteryTint: Int,
        batteryTextTint: Int,
        chargingIconTint: Int,
        opacity: Float,
        geometry: OuterGeometry.Resolved,
        centerGeometry: CenterGeometry.Resolved,
        visual: VisualCfg,
        nativeTransform: NativeRenderTransform,
        scale: Float,
        scaleMobileTypeWithCanvas: Boolean,
        previousCenterIndicator: CenterIndicator? = null,
        centerExitAmount: Float = 0f,
        centerEnterAmount: Float = 1f,
        drawReadoutText: Boolean = true,
        drawReadoutChargingIcon: Boolean = true,
        ringRetractProgress: Float? = null,
        ringRetractExitDirection: BatteryRingTransitionPolicy.ExitDirection =
            BatteryRingTransitionPolicy.ExitDirection.NONE,
    ) {
        val readout =
            resolveBatteryTopReadoutLayout(
                model = model,
                visual = visual,
                nativeTransform = nativeTransform,
            )
        val topContentAvoidance =
            if (visual.layout == ContentLayout.BATTERY_CENTER) {
                resolveNetworkTopSlotAvoidance(
                    visual = visual,
                    current = model.centerIndicator,
                    previous = previousCenterIndicator,
                    scale = scale,
                    geometry = centerGeometry,
                    scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
                    exitAmount = centerExitAmount,
                    enterAmount = centerEnterAmount,
                )
            } else {
                readout?.groupOpticalBounds
                    ?.let(::singleTopSlotAvoidance)
            }

        if (ringRetractProgress != null) {
            val drawableArcs =
                if (topContentAvoidance == null) {
                    listOf(
                        BatteryTopArcPolicy.Arc(
                            startDegrees = BATTERY_START_DEGREES,
                            sweepDegrees = BATTERY_MAX_SWEEP,
                        ),
                    )
                } else {
                    val gap =
                        resolveBatteryTopGap(
                            avoidance = topContentAvoidance,
                            ringStroke = geometry.ringStroke,
                        )
                    BatteryTopArcPolicy.drawableArcs(
                        startDegrees = BATTERY_START_DEGREES,
                        maxSweep = BATTERY_MAX_SWEEP,
                        gapCenterDegrees = gap.centerDegrees,
                        gapSweepDegrees = gap.sweepDegrees,
                    )
                }
            val segments =
                BatteryRingTransitionPolicy.resolve(
                    drawableArcs = drawableArcs,
                    batteryPercent = model.batteryPercent,
                    progress = ringRetractProgress,
                    exitDirection = ringRetractExitDirection,
                    followRetractEndpoint =
                        visual.fillFollowsRetract,
                )
            val totalSweepDegrees =
                drawableArcs
                    .sumOf { arc -> arc.sweepDegrees.coerceAtLeast(0f).toDouble() }
                    .toFloat()
            val terminalCapDominated =
                BatteryRingTransitionPolicy.isTerminalCapDominated(
                    remainingFraction = segments.remainingFraction,
                    totalSweepDegrees = totalSweepDegrees,
                    radiusPx = OuterGeometry.RING_RADIUS,
                    strokeWidthPx = geometry.ringStroke,
                )

            if (!terminalCapDominated) {
                stroke(batteryTint, 48, geometry.ringStroke, opacity)
                segments.background.forEach { arc ->
                    if (arc.sweepDegrees > 0f) {
                        canvas.drawArc(
                            batteryRing,
                            arc.startDegrees,
                            arc.sweepDegrees,
                            false,
                            paint,
                        )
                    }
                }
                stroke(batteryTint, 255, geometry.ringStroke, opacity)
                segments.active.forEach { arc ->
                    if (arc.sweepDegrees > 0f) {
                        canvas.drawArc(
                            batteryRing,
                            arc.startDegrees,
                            arc.sweepDegrees,
                            false,
                            paint,
                        )
                    }
                }
            }
        } else if (topContentAvoidance == null) {
            val segments =
                batteryArcSegments(
                    batteryPercent = model.batteryPercent,
                    startDegrees = BATTERY_START_DEGREES,
                    maxSweep = BATTERY_MAX_SWEEP,
                    degreesPerPercent = BATTERY_DEGREES_PER_PERCENT,
                )
            if (segments.inactiveSweep > 0f) {
                stroke(batteryTint, 48, geometry.ringStroke, opacity)
                canvas.drawArc(
                    batteryRing,
                    segments.inactiveStart,
                    segments.inactiveSweep,
                    false,
                    paint,
                )
            }
            if (segments.activeSweep > 0f) {
                stroke(batteryTint, 255, geometry.ringStroke, opacity)
                canvas.drawArc(
                    batteryRing,
                    BATTERY_START_DEGREES,
                    segments.activeSweep,
                    false,
                    paint,
                )
            }
        } else {
            val gap =
                resolveBatteryTopGap(
                    avoidance = topContentAvoidance,
                    ringStroke = geometry.ringStroke,
                )
            val segments =
                BatteryTopArcPolicy.resolve(
                    batteryPercent = model.batteryPercent,
                    startDegrees = BATTERY_START_DEGREES,
                    maxSweep = BATTERY_MAX_SWEEP,
                    gapCenterDegrees = gap.centerDegrees,
                    gapSweepDegrees = gap.sweepDegrees,
                )

            stroke(batteryTint, 48, geometry.ringStroke, opacity)
            segments.inactive.forEach { arc ->
                if (arc.sweepDegrees > 0f) {
                    canvas.drawArc(
                        batteryRing,
                        arc.startDegrees,
                        arc.sweepDegrees,
                        false,
                        paint,
                    )
                }
            }
            stroke(batteryTint, 255, geometry.ringStroke, opacity)
            segments.active.forEach { arc ->
                if (arc.sweepDegrees > 0f) {
                    canvas.drawArc(
                        batteryRing,
                        arc.startDegrees,
                        arc.sweepDegrees,
                        false,
                        paint,
                    )
                }
            }
        }

        if (
            visual.layout == ContentLayout.NETWORK_CENTER &&
            readout != null
        ) {
            drawBatteryTopReadout(
                canvas = canvas,
                layout = readout,
                textTint = batteryTextTint,
                chargingIconTint = chargingIconTint,
                opacity = opacity,
                nativeTransform = nativeTransform,
                drawText = drawReadoutText,
                drawChargingIcon = drawReadoutChargingIcon,
            )
        }
    }

    private fun resolveBatteryTopReadoutLayout(
        model: RenderModel,
        visual: VisualCfg,
        nativeTransform: NativeRenderTransform,
    ): BatteryTopReadoutLayout? {
        val textVisible = visual.showTopReadout
        val chargingSlotVisible =
            model.charging && visual.showTopChargingIcon
        if (!textVisible && !chargingSlotVisible) return null

        val text = model.batteryPercent.coerceIn(0, 100).toString()
        val textSize = BATTERY_TOP_TEXT_SIZE * visual.topTextScale
        val textExtraStroke =
            if (textVisible) {
                batteryTopTextExtraStroke(
                    weight = visual.topTextWeight,
                    textSize = textSize,
                )
            } else {
                0f
            }
        paint.typeface = batteryTopTextTypeface(visual.topTextWeight)
        paint.textSize = textSize
        paint.textAlign = Paint.Align.LEFT
        if (textVisible) {
            paint.getTextBounds(text, 0, text.length, batteryTopTextBounds)
        } else {
            batteryTopTextBounds.setEmpty()
        }

        val textWidth =
            if (textVisible) batteryTopTextBounds.width().toFloat().coerceAtLeast(0f)
            else 0f
        val textHeight =
            if (textVisible) batteryTopTextBounds.height().toFloat().coerceAtLeast(0f)
            else 0f
        val textOpticalWidth = textWidth + textExtraStroke
        val textOpticalHeight = textHeight + textExtraStroke
        val chargingIconResourceId =
            model.chargingIconResId
                ?.takeIf { chargingSlotVisible && it != 0 }
        val chargingIconSize =
            if (chargingSlotVisible) {
                BATTERY_TOP_CHARGING_ICON_SIZE *
                    visual.topChargingIconScale
            } else {
                0f
            }
        val chargingOpticalSize =
            chargingIconResourceId
                ?.let { resourceId ->
                    nativeResourceOpticalSize(
                        resource =
                            PresentationStore.NativeIconResource(
                                packageName = SYSTEM_UI_PACKAGE,
                                resourceId = resourceId,
                            ),
                        maxSize = chargingIconSize,
                    )
                }
        val chargingOpticalWidth = chargingOpticalSize?.width ?: 0f
        val chargingOpticalHeight = chargingOpticalSize?.height ?: 0f
        val chargingInkVisible =
            chargingOpticalWidth > 0f && chargingOpticalHeight > 0f
        val iconGap =
            if (textVisible && chargingInkVisible) {
                BATTERY_TOP_ICON_TEXT_GAP
            } else {
                0f
            }
        val groupWidth =
            textOpticalWidth +
                chargingOpticalWidth +
                iconGap
        if (groupWidth <= 0f) return null

        val groupLeft = BATTERY_COMPONENT_CENTER_X - groupWidth / 2f
        val textInkLeft =
            groupLeft +
                if (chargingInkVisible) {
                    chargingOpticalWidth + iconGap
                } else {
                    0f
                }
        val chargingTopExtent =
            if (chargingInkVisible) {
                chargingOpticalSize
                    ?.let { optical ->
                        (
                            optical.height / 2f +
                                optical.inkCenterOffsetY -
                                optical.centerOffsetY
                        ).coerceAtLeast(0f)
                    }
                    ?: chargingIconSize / 2f
            } else {
                0f
            }
        val chargingBottomExtent =
            if (chargingInkVisible) {
                chargingOpticalSize
                    ?.let { optical ->
                        (
                            optical.height / 2f -
                                optical.inkCenterOffsetY +
                                optical.centerOffsetY
                        ).coerceAtLeast(0f)
                    }
                    ?: chargingIconSize / 2f
            } else {
                0f
            }
        val groupTopExtent =
            max(
                if (textVisible) textOpticalHeight / 2f else 0f,
                chargingTopExtent,
            )
        val groupBottomExtent =
            max(
                if (textVisible) textOpticalHeight / 2f else 0f,
                chargingBottomExtent,
            )

        val groupBaseCenterY =
            batteryTopBaseCenterY(
                preferredCenterY = batteryReadoutPreferredCenterY(visual),
                defaultOpticalRise = BATTERY_TOP_DEFAULT_OPTICAL_RISE,
            )
        val groupCenterY =
            batteryTopCenterY(
                baseCenterY = groupBaseCenterY,
                requestedOffset =
                    TopInfoOffsetPolicy.readoutRequestedOffset(
                        layout = visual.layout,
                        rawOffset = visual.topOffsetY,
                    ),
            )
        val textBaselineY =
            if (textVisible) {
                groupCenterY -
                    (batteryTopTextBounds.top + batteryTopTextBounds.bottom) / 2f
            } else {
                groupCenterY
            }
        val textOpticalBounds =
            if (textVisible) {
                TransitionBounds(
                    left = textInkLeft,
                    top = groupCenterY - textOpticalHeight / 2f,
                    right = textInkLeft + textOpticalWidth,
                    bottom = groupCenterY + textOpticalHeight / 2f,
                )
            } else {
                centeredBounds(
                    centerX = BATTERY_COMPONENT_CENTER_X,
                    centerY = groupCenterY,
                    width = 0f,
                    height = 0f,
                )
            }
        val groupOpticalBounds =
            TransitionBounds(
                left = groupLeft,
                top = groupCenterY - groupTopExtent,
                right = groupLeft + groupWidth,
                bottom = groupCenterY + groupBottomExtent,
            )
        val chargingIconOpticalBounds =
            if (chargingInkVisible) {
                TransitionBounds(
                    left = groupLeft,
                    top = groupCenterY - chargingTopExtent,
                    right = groupLeft + chargingOpticalWidth,
                    bottom = groupCenterY + chargingBottomExtent,
                )
            } else {
                centeredBounds(
                    centerX = BATTERY_COMPONENT_CENTER_X,
                    centerY = groupCenterY,
                    width = 0f,
                    height = 0f,
                )
            }

        return BatteryTopReadoutLayout(
            textVisible = textVisible,
            text = text,
            textSize = textSize,
            textWeight = visual.topTextWeight,
            textExtraStroke = textExtraStroke,
            textX =
                textInkLeft +
                    textExtraStroke / 2f -
                    batteryTopTextBounds.left,
            textBaselineY = textBaselineY,
            textOpticalBounds = textOpticalBounds,
            groupOpticalBounds = groupOpticalBounds,
            chargingIconOpticalBounds = chargingIconOpticalBounds,
            chargingIconResourceId = chargingIconResourceId.takeIf { chargingInkVisible },
            chargingIconCenterX =
                if (chargingInkVisible) {
                    val desiredOpticalCenterX =
                        groupLeft + chargingOpticalWidth / 2f
                    desiredOpticalCenterX -
                        (chargingOpticalSize?.centerOffsetX ?: 0f)
                } else {
                    BATTERY_COMPONENT_CENTER_X
                },
            chargingIconCenterY =
                groupCenterY -
                    (chargingOpticalSize?.inkCenterOffsetY ?: 0f),
            chargingIconSize = chargingIconSize,
        )
    }

    private fun drawBatteryTopReadout(
        canvas: Canvas,
        layout: BatteryTopReadoutLayout,
        textTint: Int,
        chargingIconTint: Int,
        opacity: Float,
        nativeTransform: NativeRenderTransform,
        drawText: Boolean = true,
        drawChargingIcon: Boolean = true,
        chargingIconOpacity: Float = 1f,
    ) {
        layout.chargingIconResourceId
            ?.takeIf { drawChargingIcon && chargingIconOpacity > 0f }
            ?.let { resourceId ->
                drawNativeCenterResource(
                    canvas = canvas,
                    resource =
                        PresentationStore.NativeIconResource(
                            packageName = SYSTEM_UI_PACKAGE,
                            resourceId = resourceId,
                        ),
                    tint = chargingIconTint,
                    opacity = opacity * chargingIconOpacity.coerceIn(0f, 1f),
                    centerX = layout.chargingIconCenterX,
                    centerY = layout.chargingIconCenterY,
                    maxWidth = layout.chargingIconSize,
                    maxHeight = layout.chargingIconSize,
                    nativeTransform = nativeTransform,
                    pixelAligned = false,
                )
            }

        if (drawText && layout.textVisible) {
            drawBatteryTopText(
                canvas = canvas,
                layout = layout,
                textTint = textTint,
                opacity = opacity,
                weight = layout.textWeight,
            )
        }
    }

    private fun drawBatteryTopNumberTransition(
        canvas: Canvas,
        model: RenderModel,
        textTint: Int,
        opacity: Float,
        visual: VisualCfg,
        motionProgress: Float,
        targetWeight: Int?,
        targetStyle: TransitionTextStyle?,
        nativeTransform: NativeRenderTransform,
    ) {
        val layout =
            resolveBatteryTopReadoutLayout(
                model = model,
                visual = visual,
                nativeTransform = nativeTransform,
            ) ?: return
        if (!layout.textVisible) return
        val sourceWeight = layout.textWeight
        val resolvedTargetWeight =
            targetWeight?.coerceIn(BATTERY_TOP_WEIGHT_TRANSITION_MIN, BATTERY_TOP_WEIGHT_TRANSITION_MAX)
                ?: sourceWeight
        val progress = motionProgress.coerceIn(0f, 1f)
        val weight =
            (sourceWeight + (resolvedTargetWeight - sourceWeight) * progress)
                .roundToInt()
        drawBatteryTopText(
            canvas = canvas,
            layout = layout,
            textTint = textTint,
            opacity = opacity,
            weight = weight,
            targetStyle = targetStyle,
            transitionProgress = progress,
        )
    }

    private fun drawBatteryTopChargingIconTransition(
        canvas: Canvas,
        model: RenderModel,
        chargingIconTint: Int,
        opacity: Float,
        visual: VisualCfg,
        nativeTransform: NativeRenderTransform,
    ) {
        val layout =
            resolveBatteryTopReadoutLayout(
                model = model,
                visual = visual,
                nativeTransform = nativeTransform,
            ) ?: return
        val resourceId = layout.chargingIconResourceId ?: return
        drawNativeCenterResource(
            canvas = canvas,
            resource =
                PresentationStore.NativeIconResource(
                    packageName = SYSTEM_UI_PACKAGE,
                    resourceId = resourceId,
                ),
            tint = chargingIconTint,
            opacity = opacity,
            centerX = layout.chargingIconCenterX,
            centerY = layout.chargingIconCenterY,
            maxWidth = layout.chargingIconSize,
            maxHeight = layout.chargingIconSize,
            nativeTransform = nativeTransform,
            pixelAligned = false,
        )
    }

    fun transitionMobileTypeCurrentBounds(
        width: Int,
        height: Int,
        indicator: CenterIndicator.MobileType,
        targetWeight: Int?,
        targetStyle: TransitionTextStyle?,
        progress: Float,
        visual: VisualCfg = VisualCfg(),
    ): TransitionBounds? {
        if (width <= 0 || height <= 0) return null
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return null
        val scale = nativeTransform.scale
        val base =
            resolveCenterGeometry(visual)
        val weight =
            MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = base.mobileTypeWeight,
                targetWeight = targetWeight,
                progress = progress,
            )
        val current = base.copy(mobileTypeWeight = weight)
        val baseLocal =
            resolveMobileTypeLayout(
                indicator = indicator,
                scale = scale,
                geometry = current,
                scaleWithCanvas = false,
                targetStyle = targetStyle,
                transitionProgress = progress,
            ).bounds
        val local =
            if (visual.layout == ContentLayout.BATTERY_CENTER) {
                shiftBoundsY(baseLocal, networkTopTranslationY(visual))
            } else {
                baseLocal
            }
        return TransitionBounds(
            left = nativeTransform.offsetX + local.left * scale,
            top = nativeTransform.offsetY + local.top * scale,
            right = nativeTransform.offsetX + local.right * scale,
            bottom = nativeTransform.offsetY + local.bottom * scale,
        )
    }

    fun transitionBatteryNumberCurrentBounds(
        width: Int,
        height: Int,
        model: RenderModel,
        visual: VisualCfg,
        targetWeight: Int?,
        targetStyle: TransitionTextStyle?,
        progress: Float,
    ): TransitionBounds? {
        if (width <= 0 || height <= 0) return null
        val nativeTransform =
            resolveCanvasTransform(
                width = width,
                height = height,
                visual = visual,
            ) ?: return null
        val scale = nativeTransform.scale
        val layout =
            resolveBatteryTopReadoutLayout(
                model = model,
                visual = visual,
                nativeTransform = nativeTransform,
            ) ?: return null
        val sourceWeight = layout.textWeight
        val resolvedTargetWeight =
            targetWeight
                ?.coerceIn(
                    BATTERY_TOP_WEIGHT_TRANSITION_MIN,
                    BATTERY_TOP_WEIGHT_TRANSITION_MAX,
                )
                ?: sourceWeight
        val currentWeight =
            (
                sourceWeight +
                    (resolvedTargetWeight - sourceWeight) *
                        progress.coerceIn(0f, 1f)
            ).roundToInt()
        val local =
            if (layout.textVisible) {
                batteryTopTextOpticalBounds(
                    layout = layout,
                    weight = currentWeight,
                    targetStyle = targetStyle,
                    transitionProgress = progress,
                )
            } else {
                layout.groupOpticalBounds
            }
        return TransitionBounds(
            left = nativeTransform.offsetX + local.left * scale,
            top = nativeTransform.offsetY + local.top * scale,
            right = nativeTransform.offsetX + local.right * scale,
            bottom = nativeTransform.offsetY + local.bottom * scale,
        )
    }

    private fun batteryTopTextOpticalBounds(
        layout: BatteryTopReadoutLayout,
        weight: Int,
        targetStyle: TransitionTextStyle? = null,
        transitionProgress: Float = 0f,
    ): TransitionBounds {
        val extraStroke =
            batteryTopTextExtraStroke(
                weight = weight,
                textSize = layout.textSize,
            )
        configureTransitionTextStyle(
            sourceTypeface = batteryTopTextTypeface(weight),
            currentWeight = weight,
            targetStyle = targetStyle,
            progress = transitionProgress,
            textSize = layout.textSize,
        )
        paint.getTextBounds(layout.text, 0, layout.text.length, batteryTopTextBounds)
        val width =
            batteryTopTextBounds.width().toFloat().coerceAtLeast(0f) +
                extraStroke
        val height =
            batteryTopTextBounds.height().toFloat().coerceAtLeast(0f) +
                extraStroke
        return centeredBounds(
            centerX = layout.textOpticalBounds.centerX,
            centerY = layout.textOpticalBounds.centerY,
            width = width,
            height = height,
        )
    }

    private fun drawBatteryTopText(
        canvas: Canvas,
        layout: BatteryTopReadoutLayout,
        textTint: Int,
        opacity: Float,
        weight: Int,
        targetStyle: TransitionTextStyle? = null,
        transitionProgress: Float = 0f,
    ) {
        val extraStroke =
            batteryTopTextExtraStroke(
                weight = weight,
                textSize = layout.textSize,
            )
        paint.style = Paint.Style.FILL
        paint.color = textTint
        paint.alpha =
            VisualIntensity.resolveCanvasAlpha(
                color = textTint,
                semanticAlpha = 255,
                opacity = opacity,
            )
        configureTransitionTextStyle(
            sourceTypeface = batteryTopTextTypeface(weight),
            currentWeight = weight,
            targetStyle = targetStyle,
            progress = transitionProgress,
            textSize = layout.textSize,
        )
        paint.textAlign = Paint.Align.LEFT

        // Weight interpolation can change glyph ink width. Re-anchor every
        // frame to the source optical center so typography changes cannot
        // introduce a sideways drift on top of the geometry morph.
        paint.getTextBounds(layout.text, 0, layout.text.length, batteryTopTextBounds)
        val currentBounds =
            batteryTopTextOpticalBounds(
                layout = layout,
                weight = weight,
                targetStyle = targetStyle,
                transitionProgress = transitionProgress,
            )
        val currentOpticalWidth = currentBounds.width
        val currentCenterX = layout.textOpticalBounds.centerX
        val textX =
            currentCenterX -
                currentOpticalWidth / 2f +
                extraStroke / 2f -
                batteryTopTextBounds.left
        val textBaselineY =
            layout.textOpticalBounds.centerY -
                (batteryTopTextBounds.top + batteryTopTextBounds.bottom) / 2f

        if (extraStroke > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = extraStroke
            paint.strokeJoin = Paint.Join.ROUND
            canvas.drawText(
                layout.text,
                textX,
                textBaselineY,
                paint,
            )
        }
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        canvas.drawText(
            layout.text,
            textX,
            textBaselineY,
            paint,
        )
    }

    private fun configureTransitionTextStyle(
        sourceTypeface: Typeface,
        currentWeight: Int,
        targetStyle: TransitionTextStyle?,
        progress: Float,
        textSize: Float,
    ) {
        val styleProgress =
            targetStyle
                ?.let { TransitionTypographyPolicy.styleProgress(progress) }
                ?: 0f
        val targetWeight = targetStyle?.weight ?: currentWeight
        val resolvedWeight =
            (
                currentWeight +
                    (targetWeight - currentWeight) * styleProgress
            ).roundToInt().coerceIn(1, 1000)
        val targetTypeface = targetStyle?.typeface
        paint.typeface =
            when {
                targetTypeface == null || styleProgress <= 0f ->
                    sourceTypeface
                styleProgress >= 0.999f ->
                    targetTypeface
                else ->
                    Typeface.create(
                        targetTypeface,
                        resolvedWeight,
                        targetTypeface.isItalic,
                    )
            }
        paint.isFakeBoldText =
            targetStyle?.fakeBoldText == true &&
                styleProgress >= 0.72f
        paint.textScaleX =
            lerp(
                1f,
                targetStyle?.textScaleX
                    ?.takeIf { it.isFinite() && it > 0f }
                    ?: 1f,
                styleProgress,
            )
        paint.textSkewX =
            lerp(
                0f,
                targetStyle?.textSkewX
                    ?.takeIf(Float::isFinite)
                    ?: 0f,
                styleProgress,
            )
        paint.letterSpacing =
            lerp(
                0f,
                targetStyle?.letterSpacing
                    ?.takeIf(Float::isFinite)
                    ?: 0f,
                styleProgress,
            )
        paint.strokeWidth =
            lerp(
                0f,
                targetStyle?.strokeWidth
                    ?.takeIf { it.isFinite() && it >= 0f }
                    ?: 0f,
                styleProgress,
            )
        paint.style =
            if (targetStyle != null && styleProgress >= 0.999f) {
                targetStyle.paintStyle
            } else {
                Paint.Style.FILL
            }
        paint.textSize = textSize
    }

    private fun batteryTopTextTypeface(weight: Int): Typeface {
        val nativeWeight = weight.coerceIn(1, BATTERY_TOP_NATIVE_WEIGHT_MAX)
        if (cachedBatteryTopTextWeight != nativeWeight) {
            cachedBatteryTopTextWeight = nativeWeight
            cachedBatteryTopTextTypeface =
                Typeface.create(Typeface.DEFAULT, nativeWeight, false)
        }
        return cachedBatteryTopTextTypeface
    }

    private fun batteryTopTextExtraStroke(
        weight: Int,
        textSize: Float,
    ): Float {
        val extraWeight =
            (weight - BATTERY_TOP_NATIVE_WEIGHT_MAX)
                .coerceIn(0, BATTERY_TOP_SYNTHETIC_WEIGHT_RANGE)
        if (extraWeight == 0 || textSize <= 0f) return 0f
        return textSize *
            BATTERY_TOP_SYNTHETIC_STROKE_RATIO *
            extraWeight.toFloat() /
            BATTERY_TOP_SYNTHETIC_WEIGHT_RANGE.toFloat()
    }
    private fun drawCenterTransition(
        canvas: Canvas,
        current: CenterIndicator,
        previous: CenterIndicator?,
        tint: Int,
        opacity: Float,
        scale: Float,
        exitAmount: Float,
        enterAmount: Float,
        geometry: CenterGeometry.Resolved,
        nativeTransform: NativeRenderTransform,
        scaleMobileTypeWithCanvas: Boolean,
    ) {
        if (previous == null || previous == current) {
            drawCenterIndicator(
                canvas = canvas,
                indicator = current,
                tint = tint,
                opacity = opacity,
                scale = scale,
                appearAmount = 1f,
                geometry = geometry,
                nativeTransform = nativeTransform,
                scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
            )
            return
        }

        drawCenterIndicator(
            canvas = canvas,
            indicator = previous,
            tint = tint,
            opacity = opacity,
            scale = scale,
            appearAmount = exitAmount.coerceIn(0f, 1f),
            geometry = geometry,
            nativeTransform = nativeTransform,
            scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
        )
        drawCenterIndicator(
            canvas = canvas,
            indicator = current,
            tint = tint,
            opacity = opacity,
            scale = scale,
            appearAmount = enterAmount.coerceIn(0f, 1f),
            geometry = geometry,
            nativeTransform = nativeTransform,
            scaleMobileTypeWithCanvas = scaleMobileTypeWithCanvas,
        )
    }

    private fun drawCenterIndicator(
        canvas: Canvas,
        indicator: CenterIndicator,
        tint: Int,
        opacity: Float,
        scale: Float,
        appearAmount: Float,
        geometry: CenterGeometry.Resolved,
        nativeTransform: NativeRenderTransform,
        scaleMobileTypeWithCanvas: Boolean,
        mobileTypeTargetStyle: TransitionTextStyle? = null,
        mobileTypeTransitionProgress: Float = 0f,
    ) {
        if (appearAmount <= 0f) {
            return
        }

        val save = canvas.save()
        canvas.scale(
            appearAmount,
            appearAmount,
            CENTER_TRANSITION_PIVOT_X,
            CENTER_TRANSITION_PIVOT_Y,
        )
        // Match StatusBarIconView's iconAppearAmount contract:
        // center content scales with appearance progress while tint alpha remains native.
        val animatedOpacity = opacity

        when (indicator) {
            is CenterIndicator.Wifi ->
                drawWifi(
                    canvas = canvas,
                    indicator = indicator,
                    tint = tint,
                    opacity = opacity,
                    geometry = geometry,
                    nativeTransform = nativeTransform,
                    pixelAligned = appearAmount >= NATIVE_STEADY_APPEAR_THRESHOLD,
                )

            is CenterIndicator.MobileType ->
                drawMobileType(
                    canvas = canvas,
                    indicator = indicator,
                    tint = tint,
                    opacity = opacity,
                    scale = scale,
                    geometry = geometry,
                    scaleWithCanvas = scaleMobileTypeWithCanvas,
                    targetStyle = mobileTypeTargetStyle,
                    transitionProgress = mobileTypeTransitionProgress,
                )

            CenterIndicator.Airplane ->
                drawNativeAirplane(
                    canvas = canvas,
                    tint = tint,
                    opacity = animatedOpacity,
                    geometry = geometry,
                    nativeTransform = nativeTransform,
                    pixelAligned = appearAmount >= NATIVE_STEADY_APPEAR_THRESHOLD,
                )

            is CenterIndicator.NoSim ->
                drawNativeCenterResource(
                    canvas = canvas,
                    resource = indicator.nativeResource,
                    tint = tint,
                    opacity = animatedOpacity,
                    centerX = CENTER_TRANSITION_PIVOT_X,
                    centerY = CENTER_TRANSITION_PIVOT_Y,
                    maxWidth = geometry.noSimMaxSize,
                    maxHeight = geometry.noSimMaxSize,
                    nativeTransform = nativeTransform,
                    pixelAligned = appearAmount >= NATIVE_STEADY_APPEAR_THRESHOLD,
                )

            CenterIndicator.Empty -> Unit
        }
        canvas.restoreToCount(save)
    }

    private fun drawWifi(
        canvas: Canvas,
        indicator: CenterIndicator.Wifi,
        tint: Int,
        opacity: Float,
        geometry: CenterGeometry.Resolved,
        nativeTransform: NativeRenderTransform,
        pixelAligned: Boolean,
    ) {
        val nativeResourceId = indicator.nativeResourceId
        if (nativeResourceId != null) {
            val nativeResource =
                PresentationStore.NativeIconResource(
                    packageName = SYSTEM_UI_PACKAGE,
                    resourceId = nativeResourceId,
                )
            if (
                drawNativeCenterResource(
                    canvas = canvas,
                    resource = nativeResource,
                    opticalReferenceResource =
                        wifiOpticalReferenceResource(nativeResource),
                    tint = tint,
                    opacity = opacity,
                    centerX = WIFI_CENTER_X,
                    centerY = WIFI_CENTER_Y,
                    maxWidth = geometry.wifiMaxWidth,
                    maxHeight = geometry.wifiMaxHeight,
                    nativeTransform = nativeTransform,
                    pixelAligned = pixelAligned,
                )
            ) {
                return
            }
        }

        val save = canvas.save()
        canvas.translate(WIFI_CENTER_X, WIFI_CENTER_Y)
        canvas.scale(3f * geometry.wifiScale, 3f * geometry.wifiScale)
        canvas.translate(-WIFI_FALLBACK_CENTER_X, -WIFI_FALLBACK_CENTER_Y)

        wifiPaths.forEachIndexed { index, path ->
            fill(
                color = tint,
                alpha = if (index < indicator.segments) 255 else 102,
                opacity = opacity,
            )
            canvas.drawPath(path, paint)
        }
        canvas.restoreToCount(save)
    }

    private fun wifiOpticalReferenceResource(
        resource: PresentationStore.NativeIconResource,
    ): PresentationStore.NativeIconResource? {
        if (resource.packageName != SYSTEM_UI_PACKAGE) {
            return null
        }

        val referenceId =
            nativeWifiReferenceIds.getOrPut(resource.resourceId) {
                runCatching {
                    val drawableContext =
                        if (resource.packageName == context.packageName) {
                            context
                        } else {
                            context.createPackageContext(resource.packageName, 0)
                        }
                    val entryName =
                        drawableContext.resources.getResourceEntryName(resource.resourceId)
                    val referenceEntry =
                        WifiOpticalReference.connectedReferenceEntry(entryName)
                            ?: return@runCatching 0
                    drawableContext.resources.getIdentifier(
                        referenceEntry,
                        "drawable",
                        resource.packageName,
                    )
                }.getOrDefault(0)
            }
        return referenceId
            .takeIf { it != 0 }
            ?.let { resource.copy(resourceId = it) }
    }

    private fun resolveNativeTintVariant(
        resource: PresentationStore.NativeIconResource,
    ): PresentationStore.NativeIconResource? {
        if (resource.packageName != SYSTEM_UI_PACKAGE) {
            return null
        }

        val tintResourceId =
            nativeTintVariantIds.getOrPut(resource.resourceId) {
                runCatching {
                    val drawableContext =
                        context.createPackageContext(resource.packageName, 0)
                    val entryName =
                        drawableContext.resources.getResourceEntryName(resource.resourceId)
                    val tintEntryName =
                        nativeCenterTintEntryName(entryName)
                    drawableContext.resources.getIdentifier(
                        tintEntryName,
                        "drawable",
                        resource.packageName,
                    )
                }.getOrDefault(0)
            }
        return tintResourceId
            .takeIf { it != 0 }
            ?.let { resource.copy(resourceId = it) }
    }

    private fun nativeCenterAsset(
        resource: PresentationStore.NativeIconResource,
    ): NativeCenterAsset? {
        nativeCenterAssets[resource]?.let { return it }

        val asset =
            runCatching {
                val drawableContext =
                    if (resource.packageName == context.packageName) {
                        context
                    } else {
                        context.createPackageContext(resource.packageName, 0)
                    }
                val drawable =
                    drawableContext.getDrawable(resource.resourceId)
                        ?.constantState
                        ?.newDrawable(drawableContext.resources)
                        ?.mutate()
                        ?: drawableContext.getDrawable(resource.resourceId)?.mutate()
                        ?: return@runCatching null
                val visualProbe =
                    resolveNativeVisualProbe(
                        drawable = drawable,
                        resources = drawableContext.resources,
                    ) ?: return@runCatching null
                NativeCenterAsset(
                    drawable = drawable,
                    intrinsicWidth = drawable.intrinsicWidth,
                    intrinsicHeight = drawable.intrinsicHeight,
                    opticalBounds = visualProbe.opticalBounds,
                    opticalComponents = visualProbe.opticalComponents,
                    inkCenterY = visualProbe.inkCenterY,
                )
            }.getOrNull()
                ?: return null

        nativeCenterAssets[resource] = asset
        if (nativeCenterAssets.size > NATIVE_CENTER_CACHE_SIZE) {
            val eldest = nativeCenterAssets.entries.iterator().next()
            nativeCenterAssets.remove(eldest.key)
        }
        return asset
    }

    private fun resolveNativeVisualProbe(
        drawable: Drawable,
        resources: android.content.res.Resources,
    ): NativeVisualProbe? {
        val visual =
            ParticipantVisualSnapshot.resolveDrawable(
                drawable = drawable,
                resources = resources,
            ) ?: return null
        val optical = visual.envelope
        val components =
            visual.components.map { component ->
                OpticalBounds(
                    left = component.left,
                    top = component.top,
                    right = component.right,
                    bottom = component.bottom,
                )
            }
        return NativeVisualProbe(
            opticalBounds =
                OpticalBounds(
                    left = optical.left,
                    top = optical.top,
                    right = optical.right,
                    bottom = optical.bottom,
                ),
            opticalComponents = components,
            inkCenterY = visual.inkCenterY ?: optical.centerY,
        )
    }

    private fun nativeResourceOpticalSize(
        resource: PresentationStore.NativeIconResource,
        maxSize: Float,
    ): NativeOpticalSize? {
        if (maxSize <= 0f) return null
        val presentationResource = resolveNativeTintVariant(resource) ?: resource
        val asset = nativeCenterAsset(presentationResource) ?: return null
        if (asset.intrinsicWidth <= 0 || asset.intrinsicHeight <= 0) return null

        val opticalWidthRatio =
            (asset.opticalBounds.right - asset.opticalBounds.left)
                .coerceAtLeast(MIN_OPTICAL_RATIO)
        val opticalHeightRatio =
            (asset.opticalBounds.bottom - asset.opticalBounds.top)
                .coerceAtLeast(MIN_OPTICAL_RATIO)
        val opticalIntrinsicWidth = asset.intrinsicWidth * opticalWidthRatio
        val opticalIntrinsicHeight = asset.intrinsicHeight * opticalHeightRatio
        val scale =
            min(
                maxSize / opticalIntrinsicWidth,
                maxSize / opticalIntrinsicHeight,
            )
        val drawWidth = asset.intrinsicWidth * scale
        val drawHeight = asset.intrinsicHeight * scale
        return NativeOpticalSize(
            width = opticalIntrinsicWidth * scale,
            height = opticalIntrinsicHeight * scale,
            centerOffsetX =
                ((asset.opticalBounds.left + asset.opticalBounds.right) / 2f - 0.5f) *
                    drawWidth,
            centerOffsetY =
                ((asset.opticalBounds.top + asset.opticalBounds.bottom) / 2f - 0.5f) *
                    drawHeight,
            inkCenterOffsetY =
                (asset.inkCenterY - 0.5f) *
                    drawHeight,
        )
    }

    private fun resolveNativeCenterDrawGeometry(
        resource: PresentationStore.NativeIconResource,
        opticalReferenceResource: PresentationStore.NativeIconResource? = null,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
    ): NativeCenterDrawGeometry? {
        if (maxWidth <= 0f || maxHeight <= 0f) return null
        val presentationResource = resolveNativeTintVariant(resource) ?: resource
        val asset = nativeCenterAsset(presentationResource) ?: return null
        if (asset.intrinsicWidth <= 0 || asset.intrinsicHeight <= 0) return null

        val referenceAsset =
            opticalReferenceResource
                ?.let { reference ->
                    val presentationReference =
                        resolveNativeTintVariant(reference) ?: reference
                    nativeCenterAsset(presentationReference)
                }
                ?.takeIf { reference ->
                    WifiOpticalReference.canShareReferenceViewport(
                        currentWidth = asset.intrinsicWidth,
                        currentHeight = asset.intrinsicHeight,
                        referenceWidth = reference.intrinsicWidth,
                        referenceHeight = reference.intrinsicHeight,
                    )
                }
        val fitAsset = referenceAsset ?: asset
        val resolved =
            NativeOpticalGeometry.resolve(
                currentIntrinsicWidth = asset.intrinsicWidth,
                currentIntrinsicHeight = asset.intrinsicHeight,
                currentOpticalLeft = asset.opticalBounds.left,
                currentOpticalTop = asset.opticalBounds.top,
                currentOpticalRight = asset.opticalBounds.right,
                currentOpticalBottom = asset.opticalBounds.bottom,
                fitIntrinsicWidth = fitAsset.intrinsicWidth,
                fitIntrinsicHeight = fitAsset.intrinsicHeight,
                fitOpticalLeft = fitAsset.opticalBounds.left,
                fitOpticalTop = fitAsset.opticalBounds.top,
                fitOpticalRight = fitAsset.opticalBounds.right,
                fitOpticalBottom = fitAsset.opticalBounds.bottom,
                centerX = centerX,
                centerY = centerY,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            ) ?: return null
        val drawLeft = centerX - resolved.drawWidth / 2f
        val drawTop = centerY - resolved.drawHeight / 2f
        return NativeCenterDrawGeometry(
            asset = asset,
            drawWidth = resolved.drawWidth,
            drawHeight = resolved.drawHeight,
            opticalBounds =
                TransitionBounds(
                    left = resolved.opticalLeft,
                    top = resolved.opticalTop,
                    right = resolved.opticalRight,
                    bottom = resolved.opticalBottom,
                ),
            drawLeft = drawLeft,
            drawTop = drawTop,
        )
    }

    private fun drawNativeCenterResource(
        canvas: Canvas,
        resource: PresentationStore.NativeIconResource,
        opticalReferenceResource: PresentationStore.NativeIconResource? = null,
        tint: Int,
        opacity: Float,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
        nativeTransform: NativeRenderTransform,
        pixelAligned: Boolean,
    ): Boolean {
        // Resolve draw size and actual current-resource optical bounds through
        // one geometry path shared with top-slot avoidance.
        val geometry =
            resolveNativeCenterDrawGeometry(
                resource = resource,
                opticalReferenceResource = opticalReferenceResource,
                centerX = centerX,
                centerY = centerY,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            ) ?: return false
        val asset = geometry.asset
        val intrinsicWidth = asset.intrinsicWidth
        val intrinsicHeight = asset.intrinsicHeight
        val drawWidth = geometry.drawWidth
        val drawHeight = geometry.drawHeight

        val drawable = asset.drawable
        drawable.setTint(tint)
        drawable.alpha =
            VisualIntensity.resolveDrawableAlpha(opacity)

        if (pixelAligned && nativeTransform.scale > 0f) {
            val bounds =
                NativeRenderGeometry.resolvePixelBounds(
                    centerX = centerX,
                    centerY = centerY,
                    drawWidth = drawWidth,
                    drawHeight = drawHeight,
                    transform = nativeTransform,
                )
            val save = canvas.save()
            canvas.scale(
                1f / nativeTransform.scale,
                1f / nativeTransform.scale,
            )
            canvas.translate(
                -nativeTransform.offsetX,
                -nativeTransform.offsetY,
            )
            drawNativeDrawable(
                canvas = canvas,
                drawable = drawable,
                left = bounds.left.toFloat(),
                top = bounds.top.toFloat(),
                width = (bounds.right - bounds.left).toFloat(),
                height = (bounds.bottom - bounds.top).toFloat(),
                intrinsicWidth = intrinsicWidth,
                intrinsicHeight = intrinsicHeight,
            )
            canvas.restoreToCount(save)
        } else {
            drawNativeDrawable(
                canvas = canvas,
                drawable = drawable,
                left = centerX - drawWidth / 2f,
                top = centerY - drawHeight / 2f,
                width = drawWidth,
                height = drawHeight,
                intrinsicWidth = intrinsicWidth,
                intrinsicHeight = intrinsicHeight,
            )
        }
        return true
    }

    private fun drawNativeDrawable(
        canvas: Canvas,
        drawable: Drawable,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        intrinsicWidth: Int,
        intrinsicHeight: Int,
    ) {
        if (
            width <= 0f ||
            height <= 0f ||
            intrinsicWidth <= 0 ||
            intrinsicHeight <= 0
        ) {
            return
        }

        val save = canvas.save()
        canvas.translate(left, top)
        canvas.scale(
            width / intrinsicWidth.toFloat(),
            height / intrinsicHeight.toFloat(),
        )
        drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        drawable.draw(canvas)
        canvas.restoreToCount(save)
    }

    private fun airplaneResourceId(): Int? {
        if (airplaneDrawableResolved) {
            return cachedAirplaneResourceId.takeIf { it != 0 }
        }

        airplaneDrawableResolved = true
        cachedAirplaneResourceId =
            runCatching {
                val resourceContext =
                    if (context.packageName == SYSTEM_UI_PACKAGE) {
                        context
                    } else {
                        context.createPackageContext(SYSTEM_UI_PACKAGE, 0)
                    }
                resourceContext.resources.getIdentifier(
                    AIRPLANE_RESOURCE_NAME,
                    "drawable",
                    SYSTEM_UI_PACKAGE,
                )
            }.getOrDefault(0)
        return cachedAirplaneResourceId.takeIf { it != 0 }
    }

    private fun drawNativeAirplane(
        canvas: Canvas,
        tint: Int,
        opacity: Float,
        geometry: CenterGeometry.Resolved,
        nativeTransform: NativeRenderTransform,
        pixelAligned: Boolean,
    ) {
        val resourceId = airplaneResourceId() ?: return
        drawNativeCenterResource(
            canvas = canvas,
            resource =
                PresentationStore.NativeIconResource(
                    packageName = SYSTEM_UI_PACKAGE,
                    resourceId = resourceId,
                ),
            tint = tint,
            opacity = opacity,
            centerX = AIRPLANE_CENTER_X,
            centerY = AIRPLANE_CENTER_Y,
            maxWidth = geometry.airplaneMaxSize,
            maxHeight = geometry.airplaneMaxSize,
            nativeTransform = nativeTransform,
            pixelAligned = pixelAligned,
        )
    }

    private fun drawMobileType(
        canvas: Canvas,
        indicator: CenterIndicator.MobileType,
        tint: Int,
        opacity: Float,
        scale: Float,
        geometry: CenterGeometry.Resolved,
        scaleWithCanvas: Boolean,
        targetStyle: TransitionTextStyle? = null,
        transitionProgress: Float = 0f,
    ) {
        val layout =
            resolveMobileTypeLayout(
                indicator = indicator,
                scale = scale,
                geometry = geometry,
                scaleWithCanvas = scaleWithCanvas,
                targetStyle = targetStyle,
                transitionProgress = transitionProgress,
            )

        paint.style = Paint.Style.FILL
        paint.color = tint
        paint.alpha =
            VisualIntensity.resolveCanvasAlpha(
                color = tint,
                semanticAlpha = 255,
                opacity = opacity,
            )
        configureTransitionTextStyle(
            sourceTypeface = mobileTypeTypeface(geometry.mobileTypeWeight),
            currentWeight = geometry.mobileTypeWeight,
            targetStyle = targetStyle,
            progress = transitionProgress,
            textSize = layout.mainTextSize,
        )
        paint.textAlign = Paint.Align.LEFT

        paint.textSize = layout.mainTextSize
        canvas.drawText(
            layout.main,
            layout.mainX,
            layout.mainBaselineY,
            paint,
        )
        if (layout.suffix.isNotEmpty()) {
            paint.textSize = layout.suffixTextSize
            canvas.drawText(
                layout.suffix,
                layout.suffixX,
                layout.suffixBaselineY,
                paint,
            )
        }
    }

    private fun resolveMobileTypeLayout(
        indicator: CenterIndicator.MobileType,
        scale: Float,
        geometry: CenterGeometry.Resolved,
        scaleWithCanvas: Boolean,
        targetStyle: TransitionTextStyle? = null,
        transitionProgress: Float = 0f,
    ): MobileTypeLayout {
        val normalized = indicator.label.trim().uppercase()
        val split =
            when {
                normalized == "5GA" || normalized == "5G-A" || normalized == "5G_A" ->
                    "5G" to "A"
                normalized.startsWith("5G") && normalized.length > 2 ->
                    "5G" to
                        normalized
                            .removePrefix("5G")
                            .removePrefix("-")
                            .removePrefix("_")
                normalized.startsWith("4G") && normalized.length > 2 ->
                    "4G" to
                        normalized
                            .removePrefix("4G")
                            .removePrefix("-")
                            .removePrefix("_")
                indicator.enhanced && normalized == "5G" -> "5G" to "++"
                else -> normalized to ""
            }

        val mainTextSize =
            MobileTypeScalePolicy.localValue(
                baseValue = geometry.mobileTypeTextSize,
                canvasScale = scale,
                combinedScale = geometry.combinedScale,
                scaleWithCanvas = scaleWithCanvas,
            )
        val suffixTextSize =
            MobileTypeScalePolicy.localValue(
                baseValue = geometry.mobileTypeSuffixSize,
                canvasScale = scale,
                combinedScale = geometry.combinedScale,
                scaleWithCanvas = scaleWithCanvas,
            )

        configureTransitionTextStyle(
            sourceTypeface = mobileTypeTypeface(geometry.mobileTypeWeight),
            currentWeight = geometry.mobileTypeWeight,
            targetStyle = targetStyle,
            progress = transitionProgress,
            textSize = mainTextSize,
        )
        paint.textAlign = Paint.Align.LEFT
        paint.getTextBounds(
            split.first,
            0,
            split.first.length,
            mobileTypeMainBounds,
        )
        val mainBaselineY =
            MOBILE_TYPE_CENTER_Y -
                (mobileTypeMainBounds.top + mobileTypeMainBounds.bottom) / 2f

        if (split.second.isEmpty()) {
            val mainX =
                MOBILE_TYPE_CENTER_X -
                    (mobileTypeMainBounds.left + mobileTypeMainBounds.right) / 2f
            return MobileTypeLayout(
                main = split.first,
                suffix = "",
                mainTextSize = mainTextSize,
                suffixTextSize = suffixTextSize,
                mainX = mainX,
                mainBaselineY = mainBaselineY,
                suffixX = mainX,
                suffixBaselineY = mainBaselineY,
                bounds =
                    TransitionBounds(
                        left = mainX + mobileTypeMainBounds.left,
                        top = mainBaselineY + mobileTypeMainBounds.top,
                        right = mainX + mobileTypeMainBounds.right,
                        bottom = mainBaselineY + mobileTypeMainBounds.bottom,
                    ),
            )
        }

        paint.textSize = suffixTextSize
        paint.getTextBounds(
            split.second,
            0,
            split.second.length,
            mobileTypeSuffixBounds,
        )
        val totalInkWidth =
            mobileTypeMainBounds.width() +
                MOBILE_TYPE_SUFFIX_GAP +
                mobileTypeSuffixBounds.width()
        val groupLeft = MOBILE_TYPE_CENTER_X - totalInkWidth / 2f
        val mainX = groupLeft - mobileTypeMainBounds.left
        val suffixX =
            groupLeft +
                mobileTypeMainBounds.width() +
                MOBILE_TYPE_SUFFIX_GAP -
                mobileTypeSuffixBounds.left
        val suffixOffset =
            MobileTypeScalePolicy.localValue(
                baseValue = geometry.mobileTypeSuffixRise,
                canvasScale = scale,
                combinedScale = geometry.combinedScale,
                scaleWithCanvas = scaleWithCanvas,
            )
        val suffixCenterY =
            MOBILE_TYPE_CENTER_Y +
                MobileTypeSuffixPolicy.verticalOffset(
                    suffix = split.second,
                    magnitude = suffixOffset,
                )
        val suffixBaselineY =
            suffixCenterY -
                (mobileTypeSuffixBounds.top + mobileTypeSuffixBounds.bottom) / 2f

        return MobileTypeLayout(
            main = split.first,
            suffix = split.second,
            mainTextSize = mainTextSize,
            suffixTextSize = suffixTextSize,
            mainX = mainX,
            mainBaselineY = mainBaselineY,
            suffixX = suffixX,
            suffixBaselineY = suffixBaselineY,
            bounds =
                TransitionBounds(
                    left =
                        min(
                            mainX + mobileTypeMainBounds.left,
                            suffixX + mobileTypeSuffixBounds.left,
                        ),
                    top =
                        min(
                            mainBaselineY + mobileTypeMainBounds.top,
                            suffixBaselineY + mobileTypeSuffixBounds.top,
                        ),
                    right =
                        max(
                            mainX + mobileTypeMainBounds.right,
                            suffixX + mobileTypeSuffixBounds.right,
                        ),
                    bottom =
                        max(
                            mainBaselineY + mobileTypeMainBounds.bottom,
                            suffixBaselineY + mobileTypeSuffixBounds.bottom,
                        ),
                ),
        )
    }

    private fun mobileTypeTypeface(weight: Int): Typeface {
        if (cachedMobileTypeWeight != weight) {
            cachedMobileTypeWeight = weight
            cachedMobileTypeTypeface =
                Typeface.create(Typeface.DEFAULT, weight, false)
        }
        return cachedMobileTypeTypeface
    }

    private fun resolveMobileSignalTransitionLayout(
        geometry: OuterGeometry.Resolved,
        model: RenderModel,
    ): MobileSignalTransitionLayout {
        val sourceCenters = ArrayList<TransitionPoint>(MOBILE_DOT_COUNT)
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        for (index in 0 until MOBILE_DOT_COUNT) {
            val angle = geometry.bottomDotAngle(index)
            val x =
                MOBILE_CENTER_X +
                    cos(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
            val y =
                MOBILE_CENTER_Y +
                    sin(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
            sourceCenters += TransitionPoint(x, y)
            left = min(left, x - geometry.mobileDotRadius)
            top = min(top, y - geometry.mobileDotRadius)
            right = max(right, x + geometry.mobileDotRadius)
            bottom = max(bottom, y + geometry.mobileDotRadius)
        }
        if (model.mobileUnavailableMark) {
            val half =
                max(
                    geometry.unavailableMarkHalfExtent,
                    geometry.unavailableMarkStroke,
                )
            left = min(left, MOBILE_UNAVAILABLE_CENTER_X - half)
            top = min(top, MOBILE_UNAVAILABLE_CENTER_Y - half)
            right = max(right, MOBILE_UNAVAILABLE_CENTER_X + half)
            bottom = max(bottom, MOBILE_UNAVAILABLE_CENTER_Y + half)
        }

        val bounds = TransitionBounds(left, top, right, bottom)
        val diameter = geometry.mobileDotRadius * 2f
        val usableWidth = (bounds.width - diameter).coerceAtLeast(diameter * 3f)
        val step = usableWidth / (MOBILE_DOT_COUNT - 1).toFloat()
        val firstX = bounds.left + diameter / 2f
        val targetCenters =
            List(MOBILE_DOT_COUNT) { index ->
                TransitionPoint(
                    x = firstX + step * index,
                    y = bounds.centerY,
                )
            }

        return MobileSignalTransitionLayout(
            bounds = bounds,
            sourceCenters = sourceCenters,
            targetCenters = targetCenters,
        )
    }

    private fun drawMobileSignalTransition(
        canvas: Canvas,
        model: RenderModel,
        tint: Int,
        opacity: Float,
        geometry: OuterGeometry.Resolved,
        motionProgress: Float,
        shapeProgress: Float,
        targetWidthRatio: Float?,
        targetHeightRatio: Float?,
        targetBars: List<TransitionNormalizedBounds>?,
    ) {
        val layout = resolveMobileSignalTransitionLayout(geometry, model)
        @Suppress("UNUSED_VARIABLE")
        val motion = motionProgress.coerceIn(0f, 1f)
        val shape = shapeProgress.coerceIn(0f, 1f)
        val rowProgress = MobileSignalMorphPolicy.rowProgress(shape)
        val barProgress = MobileSignalMorphPolicy.barProgress(shape)
        val diameter = geometry.mobileDotRadius * 2f
        val exactBars =
            targetBars
                ?.takeIf { bars -> bars.size == MOBILE_DOT_COUNT }
                ?.sortedBy { bar -> (bar.left + bar.right) / 2f }
        if (exactBars != null) {
            drawMobileSignalTransitionToExactBars(
                canvas = canvas,
                model = model,
                tint = tint,
                opacity = opacity,
                geometry = geometry,
                layout = layout,
                rowProgress = rowProgress,
                barProgress = barProgress,
                targetWidthRatio = targetWidthRatio,
                targetHeightRatio = targetHeightRatio,
                targetBars = exactBars,
            )
            return
        }
        val maxBarHeight =
            MobileSignalMorphPolicy.targetMaxBarHeight(
                sourceBoundsHeight = layout.bounds.height,
                diameter = diameter,
                targetHeightRatio = targetHeightRatio,
            )
        val level = model.mobileLevel

        for (index in 0 until MOBILE_DOT_COUNT) {
            val source = layout.sourceCenters[index]
            val target = layout.targetCenters[index]
            val centerX = lerp(source.x, target.x, rowProgress)
            val centerY = lerp(source.y, target.y, rowProgress)
            val targetBarHeight =
                MobileSignalMorphPolicy.targetBarHeight(
                    index = index,
                    maxBarHeight = maxBarHeight,
                    diameter = diameter,
                )
            val barHeight =
                diameter +
                    (targetBarHeight - diameter) * barProgress
            val bottom =
                centerY +
                    geometry.mobileDotRadius +
                    MobileSignalMorphPolicy.sharedBottomExpansion(
                        maxBarHeight = maxBarHeight,
                        diameter = diameter,
                        barProgress = barProgress,
                    )
            fill(
                color = tint,
                alpha = if (level != null && level > index) 255 else 48,
                opacity = opacity,
            )
            canvas.drawRoundRect(
                centerX - geometry.mobileDotRadius,
                bottom - barHeight,
                centerX + geometry.mobileDotRadius,
                bottom,
                geometry.mobileDotRadius,
                geometry.mobileDotRadius,
                paint,
            )
        }
    }

    private fun drawMobileSignalTransitionToExactBars(
        canvas: Canvas,
        model: RenderModel,
        tint: Int,
        opacity: Float,
        geometry: OuterGeometry.Resolved,
        layout: MobileSignalTransitionLayout,
        rowProgress: Float,
        barProgress: Float,
        targetWidthRatio: Float?,
        targetHeightRatio: Float?,
        targetBars: List<TransitionNormalizedBounds>,
    ) {
        val diameter = geometry.mobileDotRadius * 2f
        val level = model.mobileLevel
        val outerScale =
            MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = targetWidthRatio,
                targetHeightRatio = targetHeightRatio,
            )
        val widthCompensation =
            MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = targetWidthRatio,
                outerScale = outerScale,
            )
        val heightCompensation =
            MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = targetHeightRatio,
                outerScale = outerScale,
            )
        val targetCenterX = layout.bounds.centerX
        val targetCenterY = layout.bounds.centerY
        targetBars.forEachIndexed { index, normalized ->
            val source = layout.sourceCenters[index]
            val targetLeft =
                targetCenterX +
                    (normalized.left.coerceIn(0f, 1f) - 0.5f) *
                        layout.bounds.width *
                        widthCompensation
            val targetTop =
                targetCenterY +
                    (normalized.top.coerceIn(0f, 1f) - 0.5f) *
                        layout.bounds.height *
                        heightCompensation
            val targetRight =
                targetCenterX +
                    (normalized.right.coerceIn(0f, 1f) - 0.5f) *
                        layout.bounds.width *
                        widthCompensation
            val targetBottom =
                targetCenterY +
                    (normalized.bottom.coerceIn(0f, 1f) - 0.5f) *
                        layout.bounds.height *
                        heightCompensation
            val targetWidth =
                (targetRight - targetLeft).coerceAtLeast(0.001f)
            val targetHeight =
                (targetBottom - targetTop).coerceAtLeast(0.001f)
            val targetBarCenterX = (targetLeft + targetRight) / 2f
            val targetBottomCenterY = targetBottom - diameter / 2f

            val rowCenterX = lerp(source.x, targetBarCenterX, rowProgress)
            val rowCenterY = lerp(source.y, targetBottomCenterY, rowProgress)
            val startLeft = rowCenterX - diameter / 2f
            val startRight = rowCenterX + diameter / 2f
            val startTop = rowCenterY - diameter / 2f
            val startBottom = rowCenterY + diameter / 2f

            val left = lerp(startLeft, targetBarCenterX - targetWidth / 2f, barProgress)
            val right = lerp(startRight, targetBarCenterX + targetWidth / 2f, barProgress)
            val top = lerp(startTop, targetBottom - targetHeight, barProgress)
            val bottom = lerp(startBottom, targetBottom, barProgress)
            val radius =
                min(
                    (right - left).coerceAtLeast(0f) / 2f,
                    (bottom - top).coerceAtLeast(0f) / 2f,
                )

            fill(
                color = tint,
                alpha = if (level != null && level > index) 255 else 48,
                opacity = opacity,
            )
            canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                radius,
                radius,
                paint,
            )
        }
    }

    private fun drawMobile(
        canvas: Canvas,
        model: RenderModel,
        tint: Int,
        opacity: Float,
        geometry: OuterGeometry.Resolved,
    ) {
        val level = model.mobileLevel

        for (index in 0 until MOBILE_DOT_COUNT) {
            fill(
                color = tint,
                alpha = if (level != null && level > index) 255 else 48,
                opacity = opacity,
            )
            val angle = geometry.bottomDotAngle(index)
            canvas.drawCircle(
                MOBILE_CENTER_X +
                    cos(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS,
                MOBILE_CENTER_Y +
                    sin(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS,
                geometry.mobileDotRadius,
                paint,
            )
        }

        if (model.mobileUnavailableMark) {
            val half = geometry.unavailableMarkHalfExtent
            stroke(
                color = tint,
                alpha = 255,
                width = geometry.unavailableMarkStroke,
                opacity = opacity,
            )
            canvas.drawLine(
                MOBILE_UNAVAILABLE_CENTER_X - half,
                MOBILE_UNAVAILABLE_CENTER_Y - half,
                MOBILE_UNAVAILABLE_CENTER_X + half,
                MOBILE_UNAVAILABLE_CENTER_Y + half,
                paint,
            )
            canvas.drawLine(
                MOBILE_UNAVAILABLE_CENTER_X + half,
                MOBILE_UNAVAILABLE_CENTER_Y - half,
                MOBILE_UNAVAILABLE_CENTER_X - half,
                MOBILE_UNAVAILABLE_CENTER_Y + half,
                paint,
            )
        }
    }

    private fun fill(
        color: Int,
        alpha: Int,
        opacity: Float,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha =
            VisualIntensity.resolveCanvasAlpha(
                color = color,
                semanticAlpha = alpha,
                opacity = opacity,
            )
    }

    private fun stroke(
        color: Int,
        alpha: Int,
        width: Float,
        opacity: Float,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = width
        paint.color = color
        paint.alpha =
            VisualIntensity.resolveCanvasAlpha(
                color = color,
                semanticAlpha = alpha,
                opacity = opacity,
            )
    }

    private fun wifiPathLow(): Path =
        Path().apply {
            moveTo(9.778f, 15.752f)
            cubicTo(9.63f, 15.704f, 9.503f, 15.572f, 9.249f, 15.31f)
            lineTo(7.905f, 13.923f)
            cubicTo(7.729f, 13.742f, 7.641f, 13.652f, 7.61f, 13.528f)
            cubicTo(7.586f, 13.432f, 7.601f, 13.293f, 7.644f, 13.204f)
            cubicTo(7.7f, 13.089f, 7.786f, 13.031f, 7.958f, 12.915f)
            cubicTo(8.548f, 12.516f, 9.259f, 12.283f, 10.025f, 12.283f)
            cubicTo(10.762f, 12.283f, 11.448f, 12.499f, 12.025f, 12.87f)
            cubicTo(12.202f, 12.984f, 12.29f, 13.041f, 12.349f, 13.156f)
            cubicTo(12.394f, 13.246f, 12.41f, 13.387f, 12.386f, 13.484f)
            cubicTo(12.355f, 13.61f, 12.266f, 13.701f, 12.088f, 13.885f)
            lineTo(10.707f, 15.31f)
            cubicTo(10.453f, 15.572f, 10.325f, 15.704f, 10.178f, 15.752f)
            cubicTo(10.048f, 15.796f, 9.908f, 15.796f, 9.778f, 15.752f)
            close()
        }

    private fun wifiPathMid(): Path =
        Path().apply {
            moveTo(5.678f, 11.626f)
            cubicTo(5.865f, 11.82f, 5.959f, 11.917f, 6.057f, 11.954f)
            cubicTo(6.152f, 11.991f, 6.228f, 11.997f, 6.327f, 11.976f)
            cubicTo(6.43f, 11.954f, 6.554f, 11.861f, 6.801f, 11.675f)
            cubicTo(7.7f, 11.001f, 8.816f, 10.602f, 10.025f, 10.602f)
            cubicTo(11.214f, 10.602f, 12.312f, 10.988f, 13.202f, 11.64f)
            cubicTo(13.449f, 11.821f, 13.572f, 11.912f, 13.674f, 11.933f)
            cubicTo(13.773f, 11.953f, 13.849f, 11.947f, 13.943f, 11.91f)
            cubicTo(14.04f, 11.872f, 14.132f, 11.777f, 14.318f, 11.586f)
            lineTo(14.894f, 10.991f)
            cubicTo(15.078f, 10.802f, 15.169f, 10.707f, 15.202f, 10.59f)
            cubicTo(15.228f, 10.493f, 15.221f, 10.372f, 15.182f, 10.279f)
            cubicTo(15.135f, 10.167f, 15.042f, 10.092f, 14.854f, 9.942f)
            cubicTo(13.531f, 8.883f, 11.852f, 8.249f, 10.025f, 8.249f)
            cubicTo(8.171f, 8.249f, 6.47f, 8.901f, 5.138f, 9.989f)
            cubicTo(4.954f, 10.14f, 4.861f, 10.215f, 4.816f, 10.327f)
            cubicTo(4.778f, 10.419f, 4.771f, 10.54f, 4.798f, 10.636f)
            cubicTo(4.83f, 10.752f, 4.921f, 10.846f, 5.103f, 11.033f)
            lineTo(5.678f, 11.626f)
            close()
        }

    private fun wifiPathHigh(): Path =
        Path().apply {
            moveTo(16.025f, 8.728f)
            cubicTo(16.248f, 8.912f, 16.359f, 9.004f, 16.464f, 9.031f)
            cubicTo(16.562f, 9.055f, 16.649f, 9.05f, 16.744f, 9.015f)
            cubicTo(16.846f, 8.978f, 16.939f, 8.882f, 17.125f, 8.69f)
            lineTo(17.702f, 8.094f)
            cubicTo(17.886f, 7.904f, 17.978f, 7.809f, 18.011f, 7.695f)
            cubicTo(18.039f, 7.598f, 18.034f, 7.483f, 17.997f, 7.39f)
            cubicTo(17.954f, 7.279f, 17.859f, 7.198f, 17.67f, 7.036f)
            cubicTo(15.613f, 5.277f, 12.943f, 4.215f, 10.025f, 4.215f)
            cubicTo(7.081f, 4.215f, 4.389f, 5.296f, 2.326f, 7.083f)
            cubicTo(2.139f, 7.245f, 2.045f, 7.327f, 2.002f, 7.437f)
            cubicTo(1.966f, 7.53f, 1.961f, 7.645f, 1.989f, 7.741f)
            cubicTo(2.022f, 7.855f, 2.114f, 7.95f, 2.297f, 8.139f)
            lineTo(2.873f, 8.734f)
            cubicTo(3.061f, 8.927f, 3.154f, 9.023f, 3.257f, 9.061f)
            cubicTo(3.352f, 9.096f, 3.439f, 9.1f, 3.537f, 9.075f)
            cubicTo(3.643f, 9.048f, 3.754f, 8.955f, 3.977f, 8.768f)
            cubicTo(5.613f, 7.395f, 7.722f, 6.568f, 10.025f, 6.568f)
            cubicTo(12.306f, 6.568f, 14.396f, 7.379f, 16.025f, 8.728f)
            close()
        }

    private companion object {
        const val CANONICAL_SIZE = 120f
        const val BATTERY_START_DEGREES = 150f
        const val BATTERY_MAX_SWEEP = 240f
        const val BATTERY_DEGREES_PER_PERCENT = 2.4f
        const val MOBILE_DOT_COUNT = 4
        const val MOBILE_CENTER_X = 60f
        const val MOBILE_CENTER_Y = 58f
        const val MOBILE_UNAVAILABLE_CENTER_X = 60f
        const val MOBILE_UNAVAILABLE_CENTER_Y = 94f
        const val WIFI_CENTER_X = 60f
        const val WIFI_CENTER_Y = 58f
        const val WIFI_FALLBACK_CENTER_X = 10f
        const val WIFI_FALLBACK_CENTER_Y = 10f
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val AIRPLANE_RESOURCE_NAME = "stat_sys_signal_flightmode"
        const val AIRPLANE_CENTER_X = 60f
        const val AIRPLANE_CENTER_Y = 56f
        const val CENTER_TRANSITION_PIVOT_X = 60f
        const val CENTER_TRANSITION_PIVOT_Y = 60f
        const val MOBILE_TYPE_CENTER_X = 60f
        const val MOBILE_TYPE_CENTER_Y = 60f
        const val MOBILE_TYPE_SUFFIX_GAP = 2f
        const val NATIVE_CENTER_CACHE_SIZE = 8
        const val MIN_OPTICAL_RATIO = 0.08f
        const val NATIVE_STEADY_APPEAR_THRESHOLD = 0.999f
        const val BATTERY_COMPONENT_CENTER_X = 60f
        const val BATTERY_COMPONENT_CENTER_Y = 58f
        const val BATTERY_TOP_TEXT_SIZE = 24f
        const val BATTERY_TOP_CHARGING_ICON_SIZE = 18f
        const val BATTERY_TOP_ICON_TEXT_GAP = 1f
        const val BATTERY_TOP_RING_VISUAL_CLEARANCE = 2f
        const val BATTERY_TOP_DEFAULT_OPTICAL_RISE = 1.5f
        const val BATTERY_TOP_CONTENT_CENTER_Y = 16f
        const val TOP_SLOT_CENTER_Y =
            BATTERY_TOP_CONTENT_CENTER_Y -
                BATTERY_TOP_DEFAULT_OPTICAL_RISE -
                BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
        const val BATTERY_TOP_NATIVE_WEIGHT_MAX = 1000
        const val BATTERY_TOP_SYNTHETIC_WEIGHT_RANGE = 400
        const val BATTERY_TOP_SYNTHETIC_STROKE_RATIO = 0.07f
        const val BATTERY_TOP_WEIGHT_TRANSITION_MIN = 100
        const val BATTERY_TOP_WEIGHT_TRANSITION_MAX = 1400

    }

    private data class TopSlotAvoidance(
        val bounds: TransitionBounds,
        val components: List<TransitionBounds>,
    )

    private data class BatteryTopReadoutLayout(
        val textVisible: Boolean,
        val text: String,
        val textSize: Float,
        val textWeight: Int,
        val textExtraStroke: Float,
        val textX: Float,
        val textBaselineY: Float,
        val textOpticalBounds: TransitionBounds,
        val groupOpticalBounds: TransitionBounds,
        val chargingIconOpticalBounds: TransitionBounds,
        val chargingIconResourceId: Int?,
        val chargingIconCenterX: Float,
        val chargingIconCenterY: Float,
        val chargingIconSize: Float,
    )

    private data class TransitionPoint(
        val x: Float,
        val y: Float,
    )

    private data class MobileSignalTransitionLayout(
        val bounds: TransitionBounds,
        val sourceCenters: List<TransitionPoint>,
        val targetCenters: List<TransitionPoint>,
    )

    private data class TransitionWifiMetrics(
        val sourceOpticalWidth: Float,
        val sourceOpticalHeight: Float,
        val targetOpticalBounds: TransitionNormalizedBounds,
    )

    private data class MobileTypeLayout(
        val main: String,
        val suffix: String,
        val mainTextSize: Float,
        val suffixTextSize: Float,
        val mainX: Float,
        val mainBaselineY: Float,
        val suffixX: Float,
        val suffixBaselineY: Float,
        val bounds: TransitionBounds,
    )

    private data class NativeCenterDrawGeometry(
        val asset: NativeCenterAsset,
        val drawWidth: Float,
        val drawHeight: Float,
        val opticalBounds: TransitionBounds,
        val drawLeft: Float,
        val drawTop: Float,
    ) {
        fun opticalComponents(): List<TransitionBounds> =
            asset.opticalComponents.map { component ->
                TransitionBounds(
                    left = drawLeft + component.left * drawWidth,
                    top = drawTop + component.top * drawHeight,
                    right = drawLeft + component.right * drawWidth,
                    bottom = drawTop + component.bottom * drawHeight,
                )
            }
    }

    private data class NativeOpticalSize(
        val width: Float,
        val height: Float,
        val centerOffsetX: Float,
        val centerOffsetY: Float,
        val inkCenterOffsetY: Float,
    )

    private data class NativeCenterAsset(
        val drawable: Drawable,
        val intrinsicWidth: Int,
        val intrinsicHeight: Int,
        val opticalBounds: OpticalBounds,
        val opticalComponents: List<OpticalBounds>,
        val inkCenterY: Float,
    )

    private data class NativeVisualProbe(
        val opticalBounds: OpticalBounds,
        val opticalComponents: List<OpticalBounds>,
        val inkCenterY: Float,
    )

    private data class OpticalBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        companion object {
            val FULL =
                OpticalBounds(
                    left = 0f,
                    top = 0f,
                    right = 1f,
                    bottom = 1f,
                )
        }
    }
}
