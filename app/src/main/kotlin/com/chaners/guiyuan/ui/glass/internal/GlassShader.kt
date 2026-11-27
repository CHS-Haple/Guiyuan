// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass.internal

/** Cache key of the compiled colour-blend shader. */
internal const val GLASS_COLOR_BLEND_SHADER_KEY = "MiuixGlassColorBlend"

/** Colour-blend shader applied to the blurred backdrop. */
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
