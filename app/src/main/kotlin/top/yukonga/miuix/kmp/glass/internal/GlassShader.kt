// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass.internal

/** Cache key for the compiled glass shader. */
internal const val GLASS_SHADER_KEY = "MiuixGlass"

/**
 * The silhouette, shared by the material shader and the mask shader so the two can never disagree
 * about where the surface ends.
 */
private val GLASS_SDF_SOURCE: String = """
uniform float2 in_size;         // component size
uniform float4 in_radii;        // topLeft, topRight, bottomRight, bottomLeft
uniform float in_smoothing;     // 0 = circular corner, 1 = continuous corner

float roundedBoxSdf(float2 p, float2 b, float r) {
    float2 d = p - b + r;
    return min(max(d.x, d.y), 0.0) + length(max(d, float2(0.0))) - r;
}

float supercircleSdf(float2 off, float tile) {
    float2 q = max(float2(0.0), (float2(tile) + off) / tile);
    float hi = max(q.x, q.y);
    float ratio = (hi == 0.0) ? 0.0 : clamp(min(q.x, q.y) / hi, 0.0, 1.0);
    float fit = (((($FIT_4 * ratio + $FIT_3) * ratio + $FIT_2) * ratio + $FIT_1) * ratio + $FIT_0);
    float len = length(q);
    float distBase = (len + 1.0) - 1.0 / (1.0 - ratio * ratio * clamp(len, 0.0, 1.0) * fit);
    return min(max(tile + off.x, tile + off.y), 0.0) + tile * (distBase - 1.0);
}

float pickRadius(float2 local, float2 b) {
    float top = (local.x < b.x) ? in_radii.x : in_radii.y;
    float bottom = (local.x < b.x) ? in_radii.w : in_radii.z;
    return min((local.y < b.y) ? top : bottom, min(b.x, b.y));
}

float sdfShape(float2 p, float2 b, float r) {
    float box = roundedBoxSdf(p, b, r);
    float minHalf = min(b.x, b.y);
    if (in_smoothing <= 0.001 || r < $MIN_SUPERCIRCLE_RADIUS || (minHalf - r) <= 1.0) {
        return box;
    }
    float tile = min($TILE_SCALE * r, minHalf);
    return mix(box, supercircleSdf(p - b, tile), in_smoothing);
}

// Distance to the silhouette from a point in component space. Negative inside.
float silhouetteSdf(float2 local) {
    float2 b = in_size * 0.5;
    return sdfShape(abs(local - b), b, pickRadius(local, b));
}
"""

/** The rim: how the surface curves near the silhouette, and how the two directional lights catch it. */
private val GLASS_RIM_SOURCE: String = """
const float3 kLumaWeights = float3(0.2126, 0.7152, 0.0722);

float luma(float3 color) {
    return dot(color, kLumaWeights);
}

float smooth5Map(float t) {
    t = clamp(mix(0.5, 1.0, t), 0.0, 1.0);
    t = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    return (t - 0.5) * 2.0;
}

// Depth 0 at the silhouette, 1 once the surface is flat. `p` shapes how fast it gets there.
float edgeCurve(float x, float p) {
    if (x >= 0.85) return 1.0;
    float c = smooth5Map(sqrt(clamp(x, 0.0, 1.0)));
    return 1.0 - pow(1.0 - c, p);
}

// Slope of `edgeCurve` in `x`.
//
// The rim profile is differentiated in closed form. A one-pixel step would be a sixth of the band
// on the downscaled layer this pass runs on, and a step that coarse averages the turn away: the
// tilt it reports inside the band comes out several times the real one, and the rim reads as a dome
// rather than an edge.
float edgeCurveSlope(float x, float p) {
    if (x >= 0.85) return 0.0;
    float t = sqrt(clamp(x, 0.000001, 1.0));
    float u = 0.5 + 0.5 * t;
    float um = u - 1.0;
    float c = smooth5Map(t);
    // d(smooth5Map(sqrt x))/dx, the quintic's derivative carried through the square root.
    float slope = 15.0 * u * u * um * um / t;
    return p * pow(max(1.0 - c, 0.0), p - 1.0) * slope;
}

// Direction the distance field grows in: a unit vector pointing out of the shape. The unshaped
// field is locally a plane, so a half-pixel difference reads it exactly.
float2 sdfGradient(float2 local) {
    float dx = silhouetteSdf(local + float2(0.5, 0.0)) - silhouetteSdf(local - float2(0.5, 0.0));
    float dy = silhouetteSdf(local + float2(0.0, 0.5)) - silhouetteSdf(local - float2(0.0, 0.5));
    float2 gradient = float2(dx, dy);
    float length2d = length(gradient);
    return (length2d < 0.0001) ? float2(0.0) : gradient / length2d;
}

// Lifts a colour toward white without clipping: the gain is applied around a luminance-dependent
// pivot, so bright pixels move less than dark ones.
float3 liftToward(float3 color, float amount) {
    float v = luma(color);
    float w = smoothstep(0.0, 0.5, v);
    float k = mix(1.0 - v, v, w);
    float gain = 1.0 + smoothstep(0.0, 1.0, amount) * mix(0.75, 0.4, w);
    return (color + k) * gain - k;
}

float dynamicAdd(float3 color) {
    float toWhite = smoothstep(0.2, 1.0, distance(float3(1.0), color));
    return mix(0.8, mix(0.2, 1.0, toWhite), luma(color));
}

// acos is not worth its cost here; this is the usual sqrt-based polynomial fit.
float lightFalloff(float3 normal, float3 direction, float intensity, float angleRange) {
    float dp = dot(normal, direction);
    float d = clamp(dp, -1.0, 1.0);
    float u = 1.0 - d;
    float positive = sqrt(u) * (u * (u * 0.04587603 + 0.10884186) + 1.41502593);
    float angle = (d < 0.0) ? (3.14159265 - positive) : positive;
    return max(dp, 0.0) * max(intensity * (1.0 - angle / max(angleRange, 0.0001)), 0.0);
}

// How much light the rim carries where neither directional light reaches. Calibrated so the sides
// between the two lit arcs come out about a dozen levels above the surface.
const float kEdgeAmbient = 0.24;

// How much of the rim band the light rides, measured from the silhouette inward. The band itself
// is what bends the refraction ray; the highlight drawn on it is a hairline.
const float kRimBandFraction = 0.33;

// How far the two lights lift a surface of `color`, in `[0, 1]`. The second light points the
// opposite way in x and y, so one direction lights both sides of the shape.
float rimLighten(float3 normal, float4 lightDir, float2 amounts, float3 color) {
    float3 lit = float3(normal.x, -normal.y, normal.z);
    float primary = lightFalloff(lit, lightDir.xyz, amounts.x, lightDir.w);
    float opposite = lightFalloff(lit, lightDir.xyz * float3(-1.0, -1.0, 1.0), amounts.y, lightDir.w);
    // Two opposed lights alone leave two lit arcs and two dark ones. An ambient term proportional
    // to how far the rim has turned away from the viewer joins them up.
    float edgeFactor = clamp(length(lit.xy), 0.0, 1.0);
    float raw = clamp(primary + opposite + kEdgeAmbient * edgeFactor, 0.0, 1.0) * dynamicAdd(color);
    raw = pow(clamp(raw, 0.0, 1.0), 0.85);
    // A soft knee, tighter over a bright backdrop: full light lifts the rim about half way, never
    // to white. Without it the whole edge band clips and the hairline has nothing left to sit on.
    float knee = mix(1.0, 0.7, smoothstep(0.0, 0.5, luma(color)));
    return raw / (raw + knee);
}

// The steepest the bevel is allowed to get, as a rise over a run.
//
// The closed-form slope runs to the thousands at the silhouette, which stands the normal on end and
// hands both lights everything they can take. The cap keeps the highlight a hairline.
const float kMaxBevelSlope = 1.7;
"""

/** The glass material, as one AGSL/SkSL pass over the blurred backdrop. */
internal val GLASS_SHADER: String = """
uniform shader child;

uniform float2 in_pad;          // component origin inside the padded layer
uniform float2 in_maxCoord;     // sampling clamp of the padded layer
uniform float4 in_alphaEdge;    // alpha, edge width, thickness, reflect offset
uniform float4 in_iorRefl;      // ior, reflect strength, reflect lighten, colour gamma
uniform float4 in_tint;         // tint rgb, tint strength
uniform float4 in_whiteMixBg;   // colour white, colour mix, backdrop saturation, backdrop brightness
uniform float4 in_darker;       // darker start, darker end, darker, inner bottom
uniform float4 in_lightDir;     // direction xyz, angle range * pi
uniform float4 in_lightAmt;     // intensity, opposite intensity, luminance amount, overspill
uniform float4 in_lumCurve;     // cubic coefficients A, B, C, D
uniform float4 in_satBri;       // saturation, brightness, burn, unshade
uniform float4 in_edgePow;      // edge pow, wide-sample radius

const float3 kCurveWeights = float3(0.2125, 0.7153, 0.0721);
const float3 kShadowTint = float3(0.07874, 0.02848, 0.09278);

// ------------------------------------------------- silhouette and edge profile
$GLASS_SDF_SOURCE
$GLASS_RIM_SOURCE
// ------------------------------------------------------------------ sampling

float4 tapLayer(float2 local) {
    return float4(child.eval(clamp(local + in_pad, float2(0.5), in_maxCoord)));
}

// The rays reach past the silhouette, and the recorded layer only carries backdrop as far as the
// blur padding. A tap beyond it returns transparent black, which would ring the shape in a dark
// halo, so fall back to the nearest tap inside the surface.
float4 sampleBackdrop(float2 local) {
    float4 col = tapLayer(local);
    if (col.a < 0.02) {
        col = tapLayer(clamp(local, float2(0.5), in_size - 0.5));
    }
    if (col.a > 0.004) {
        col = float4(col.rgb / col.a, 1.0);
    }
    float t = smoothstep(in_darker.x, in_darker.y, luma(col.rgb)) * in_darker.z;
    col.rgb = mix(col.rgb, kShadowTint, t);
    return col;
}

// Stands in for the second, far wider blur.
//
// The taps sit on a Vogel spiral, which covers the disc evenly instead of leaving the middle empty;
// a sparse ring would paint the pattern it leaves behind onto a bright object. Taps are averaged by
// coverage, so one that lands past the recorded backdrop contributes nothing rather than pulling
// the colour toward black.
float4 sampleWide(float2 local) {
    float2 outer = in_edgePow.yz;
    if (outer.x <= 0.5 || outer.y <= 0.5) return sampleBackdrop(local);
    // The colour texture is blurred at a radius far larger than any surface it sits on, so across a
    // bar or a button it is very nearly one colour. `in_edgePow.w` says how completely: at 1 the
    // disc is centred on the surface and every pixel reads the same average.
    float2 origin = mix(local, in_size * 0.5, in_edgePow.w);
    float3 sum = float3(0.0);
    float coverage = 0.0;
    for (int i = 0; i < 25; i++) {
        float index = float(i);
        float radius = sqrt((index + 0.5) * 0.04);
        float angle = index * 2.39996323;
        float4 tap = tapLayer(origin + float2(cos(angle), sin(angle)) * radius * outer);
        sum += tap.rgb;
        coverage += tap.a;
    }
    if (coverage < 0.0001) return sampleBackdrop(local);
    float3 color = sum / coverage;
    float t = smoothstep(in_darker.x, in_darker.y, luma(color)) * in_darker.z;
    return float4(mix(color, kShadowTint, t), 1.0);
}

// ------------------------------------------------------------------- grading

float4 adjustColor(float4 color, float saturation, float brightness) {
    float l = dot(color.rgb, kCurveWeights);
    return float4(mix(float3(l), color.rgb, saturation) + float3(brightness * color.a), color.a);
}

float4 luminanceCurve(float4 color) {
    float a = max(color.a, 0.0001);
    float3 straight = color.rgb / a;
    float l = clamp(dot(straight, kCurveWeights), 0.0, 1.0);
    float adjusted = ((in_lumCurve.x * l + in_lumCurve.y) * l + in_lumCurve.z) * l + in_lumCurve.w;
    adjusted = clamp(adjusted, 0.0, 1.0);
    float scale = adjusted / max(l, 0.01) * smoothstep(0.0, 0.1, l);
    return float4(mix(straight, straight * scale, in_lightAmt.z) * a, color.a);
}

float4 processColor(float4 color) {
    color = luminanceCurve(color);
    color.rgb = adjustColor(color, in_satBri.x, in_satBri.y).rgb;
    return color;
}

float4 processGlassColor(float4 material, float2 local) {
    float4 wide = adjustColor(sampleWide(local), in_whiteMixBg.z, in_whiteMixBg.w);
    wide.rgb = mix(wide.rgb, float3(1.0), in_whiteMixBg.x);

    float lumin = clamp(luma(wide.rgb), 0.0, 1.0);
    float burn = pow(lumin, max(in_satBri.z, 0.5)) - 0.5;
    float colorRatio = 0.8 * mix(lumin, 1.0, (1.587 * burn * burn * burn) + 0.5);

    float3 ratio = mix(float3(1.0), wide.rgb, colorRatio);
    float mean = (ratio.r + ratio.g + ratio.b) * 0.33333333;
    // Pull near-grey backdrops all the way to grey so the glass never picks up a false cast.
    ratio = mix(float3(mean), ratio, smoothstep(0.0, 0.4, distance(wide.rgb, float3(mean))));
    ratio = mix(ratio, in_tint.rgb, in_tint.a);

    material.rgb *= ratio;
    material.rgb = mix(material.rgb, ratio, in_whiteMixBg.y * colorRatio);
    return material;
}

float3 addLight(float3 color, float3 lightColor, float strength) {
    float toWhite = smoothstep(0.2, 1.0, distance(float3(1.0), color));
    return color + lightColor * strength * mix(0.3, 1.0, toWhite);
}

// ---------------------------------------------------------------------- main

// The material at one point, given how deep into the rim it is and which way the rim faces.
//
// `d` is passed in rather than measured: the caller walks it across a pixel, and over that distance
// the distance field is a plane, so stepping the value is exact and free.
float4 shadeMaterial(float2 local, float d, float2 gradient, float edge) {
    float depth = clamp(-d / edge, 0.0, 1.0);
    float shaped = edgeCurve(depth, in_edgePow.x);
    // The reflection and the added light ride the same outer third of the band the rim light does.
    // Spread across the whole band they come out as a soft halo several times wider than the edge
    // the edge, because this pass cannot resolve where the turn actually is.
    float nmlZ = edgeCurve(min(depth / kRimBandFraction, 1.0), 1.0);

    bool isFlat = shaped >= 1.0;
    float3 normal = isFlat
        ? float3(0.0, 0.0, 1.0)
        : normalize(float3(gradient * edgeCurveSlope(depth, in_edgePow.x), 1.0));

    float2 refractUv = local;
    if (!isFlat) {
        float3 bent = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / max(in_iorRefl.x, 1.0));
        float thickness = in_alphaEdge.z;
        refractUv += bent.xy * mix((thickness - edge) * 2.0, thickness * 2.0, shaped);
    }
    float4 material = sampleBackdrop(refractUv);

    float rimZ = 1.0 - nmlZ;
    float4 mirrored = float4(0.0);
    if (rimZ > 0.000001) {
        float3 bounced = reflect(float3(0.0, 0.0, -1.0), normal);
        mirrored = sampleBackdrop(local + bounced.xy * (in_alphaEdge.w * (1.0 - shaped)));
        material = mix(material, mirrored, clamp(rimZ * in_iorRefl.y, 0.0, 1.0));
    }
    material.rgb = addLight(material.rgb, mirrored.rgb, (1.0 - shaped) * in_iorRefl.z);

    material = processColor(material);
    material = processGlassColor(material, local);

    // Additive glow along the bottom edge.
    float bottom = smoothstep(1.0, 0.0, 1.0 - local.y / max(in_size.y, 1.0));
    material.rgb += bottom * bottom * in_darker.w;


    material = pow(max(material, float4(0.0)), float4(in_iorRefl.w));
    material.rgb = mix(clamp(material.rgb, 0.0, 1.0), in_tint.rgb, in_satBri.w);
    return material;
}

// ---------------------------------------------------------------------- main

half4 main(float2 coord) {
    float2 local = coord - in_pad;
    float d = silhouetteSdf(local);
    // Overspill the silhouette: the mask pass trims the edge at full resolution, so this one only
    // has to reach past it.
    if (d >= in_lightAmt.w) return half4(0.0);

    // A fully unshaded style is a flat tinted shape; skip the whole material.
    if ((1.0 - in_satBri.w) <= 0.000001) {
        float flatAlpha = in_alphaEdge.x;
        return half4(half3(in_tint.rgb * flatAlpha), half(flatAlpha));
    }

    float edge = max(in_alphaEdge.y, 1.0);
    float2 gradient = sdfGradient(local);
    float4 material;

    // Now that the normal is exact, the lit part of the rim is a few display pixels wide — less
    // than one pixel of this layer. Read once per pixel it would alias into a staircase, so inside
    // the band the material is integrated across the pixel instead. One dimension is enough: the
    // rim varies along the distance field's gradient and is constant across it.
    if (-d < edge && length(gradient) > 0.5) {
        material = shadeMaterial(local - gradient * 0.375, d - 0.375, gradient, edge) * 0.25;
        material += shadeMaterial(local, d, gradient, edge) * 0.5;
        material += shadeMaterial(local + gradient * 0.375, d + 0.375, gradient, edge) * 0.25;
    } else {
        material = shadeMaterial(local, d, gradient, edge);
    }

    float alpha = material.a * in_alphaEdge.x;
    return half4(half3(material.rgb * alpha), half(alpha));
}
"""

internal const val GLASS_COLOR_BLEND_SHADER_KEY = "MiuixGlassColorBlend"

/** The colour treatment a glass surface lays over its blurred backdrop. */
internal val GLASS_COLOR_BLEND_SHADER: String = """
uniform shader child;

uniform float4 in_blend0;     // layer 0 colour, straight; w = amount
uniform float4 in_blend1;     // layer 1
uniform float4 in_blend2;     // layer 2
uniform float4 in_blendMode;  // mode of layers 0..2; w = how many layers are live

const float3 kLumWeights = float3(0.3, 0.59, 0.11);

float lumOf(float3 c) {
    return dot(c, kLumWeights);
}

// Pulls a colour back inside the unit cube without moving its luminance, which is what keeps a
// non-separable blend from clipping to a different tone than it computed.
float3 clipColor(float3 c) {
    float l = lumOf(c);
    float n = min(min(c.r, c.g), c.b);
    float x = max(max(c.r, c.g), c.b);
    if (n < 0.0) c = l + (c - l) * l / max(l - n, 0.0001);
    if (x > 1.0) c = l + (c - l) * (1.0 - l) / max(x - l, 0.0001);
    return c;
}

float3 setLum(float3 c, float l) {
    return clipColor(c + (l - lumOf(c)));
}

float softLightChannel(float d, float s) {
    float dd = (d <= 0.25) ? ((16.0 * d - 12.0) * d + 4.0) * d : sqrt(d);
    return (s <= 0.5)
        ? d - (1.0 - 2.0 * s) * d * (1.0 - d)
        : d + (2.0 * s - 1.0) * (dd - d);
}

float3 blendLayer(float3 d, float4 layer, float mode) {
    float3 s = layer.rgb;
    float a = layer.a;
    int m = int(mode + 0.5);
    float3 b;
    if (m == 1) {
        // plus darker: subtractive, so it floors the backdrop instead of greying it
        return clamp(d - a * (1.0 - s), 0.0, 1.0);
    } else if (m == 2) {
        // plus lighter
        return clamp(d + a * s, 0.0, 1.0);
    } else if (m == 3) {
        b = float3(
            softLightChannel(d.r, s.r),
            softLightChannel(d.g, s.g),
            softLightChannel(d.b, s.b)
        );
    } else if (m == 4) {
        // hard light: the layer decides
        b = mix(1.0 - 2.0 * (1.0 - s) * (1.0 - d), 2.0 * s * d, step(s, float3(0.5)));
    } else if (m == 5) {
        // overlay: the backdrop decides
        b = mix(1.0 - 2.0 * (1.0 - d) * (1.0 - s), 2.0 * d * s, step(d, float3(0.5)));
    } else if (m == 6) {
        b = setLum(d, lumOf(s));
    } else if (m == 7) {
        b = min(float3(1.0), d / max(1.0 - s, float3(0.0001)));
    } else if (m == 8) {
        b = 1.0 - min(float3(1.0), (1.0 - d) / max(s, float3(0.0001)));
    } else {
        b = s;
    }
    return clamp(mix(d, b, a), 0.0, 1.0);
}

half4 main(float2 coord) {
    half4 src = child.eval(coord);
    float a = float(src.a);
    if (a <= 0.0) return src;
    float3 d = float3(src.rgb) / a;
    d = blendLayer(d, in_blend0, in_blendMode.x);
    if (in_blendMode.w > 1.5) d = blendLayer(d, in_blend1, in_blendMode.y);
    if (in_blendMode.w > 2.5) d = blendLayer(d, in_blend2, in_blendMode.z);
    return half4(half3(d * a), src.a);
}
"""
