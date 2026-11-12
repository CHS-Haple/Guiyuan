// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.material.withUnderlayBlur as upstreamWithUnderlayBlur

// Use the pinned MIUIX Material models without keeping app-owned copies of their state and tokens.
// Local GlassStroke and the adapted shader/dismissal implementation remain separately owned.
typealias GlassColorBlendMode = top.yukonga.miuix.kmp.material.GlassColorBlendMode
typealias GlassColorLayer = top.yukonga.miuix.kmp.material.GlassColorLayer
typealias GlassMaterial = top.yukonga.miuix.kmp.material.GlassMaterial
typealias GlassMaterials = top.yukonga.miuix.kmp.material.GlassMaterials
typealias GlassShape = top.yukonga.miuix.kmp.material.GlassShape
typealias GlassStyle = top.yukonga.miuix.kmp.material.GlassStyle
typealias GlassStyles = top.yukonga.miuix.kmp.material.GlassStyles
typealias GlassDefaults = top.yukonga.miuix.kmp.material.GlassDefaults
typealias GlassBlend = top.yukonga.miuix.kmp.material.GlassBlend
typealias GlassInner = top.yukonga.miuix.kmp.material.GlassInner
typealias GlassEdge = top.yukonga.miuix.kmp.material.GlassEdge
typealias GlassReflect = top.yukonga.miuix.kmp.material.GlassReflect
typealias GlassLight = top.yukonga.miuix.kmp.material.GlassLight
typealias GlassRefract = top.yukonga.miuix.kmp.material.GlassRefract
typealias GlassBackground = top.yukonga.miuix.kmp.material.GlassBackground
typealias GlassBlur = top.yukonga.miuix.kmp.material.GlassBlur
typealias GlassShadow = top.yukonga.miuix.kmp.material.GlassShadow
typealias GlassShadows = top.yukonga.miuix.kmp.material.GlassShadows

// The upstream factories live in the material package; retain the local call sites unchanged.
fun GlassShape(cornerRadius: Dp, smoothing: Float = GlassDefaults.Smoothing): GlassShape =
    top.yukonga.miuix.kmp.material.GlassShape(cornerRadius, smoothing)

fun GlassShape(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    smoothing: Float = GlassDefaults.Smoothing,
): GlassShape = top.yukonga.miuix.kmp.material.GlassShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

internal fun GlassMaterial.withUnderlayBlur(underlay: GlassMaterial?): GlassMaterial =
    this.upstreamWithUnderlayBlur(underlay)
