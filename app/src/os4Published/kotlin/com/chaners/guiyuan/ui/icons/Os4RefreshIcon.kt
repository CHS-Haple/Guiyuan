// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

// Exact pinned MIUIX PR #423 Glass Regular.Refresh path; match the other OS4 top-bar icons.
internal val Os4GlassRefresh: ImageVector by lazy {
    ImageVector.Builder(
                name = "Refresh.Glass.Regular",
                defaultWidth = 24.0f.dp,
                defaultHeight = 24.0f.dp,
                viewportWidth = 1142.9f,
                viewportHeight = 1142.9f,
                autoMirror = false,
            ).apply {
                group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                    addPath(
                        pathData = listOf(
                            PathNode.MoveTo(983.9f, 204.0f),
                            PathNode.LineTo(962.9f, 231.0f),
                            PathNode.QuadTo(956.9f, 239.0f, 956.9f, 246.5f),
                            PathNode.QuadTo(956.9f, 254.0f, 963.9f, 263.0f),
                            PathNode.QuadTo(1024.9f, 344.0f, 1051.4f, 440.0f),
                            PathNode.QuadTo(1077.9f, 536.0f, 1066.4f, 634.5f),
                            PathNode.QuadTo(1054.9f, 733.0f, 1002.9f, 821.0f),
                            PathNode.QuadTo(946.9f, 917.0f, 857.9f, 979.5f),
                            PathNode.QuadTo(768.9f, 1042.0f, 661.4f, 1062.0f),
                            PathNode.QuadTo(553.9f, 1082.0f, 443.9f, 1054.0f),
                            PathNode.QuadTo(429.9f, 1050.0f, 424.9f, 1038.5f),
                            PathNode.QuadTo(419.9f, 1027.0f, 425.9f, 1016.0f),
                            PathNode.LineTo(441.9f, 989.0f),
                            PathNode.QuadTo(447.9f, 977.0f, 454.9f, 974.5f),
                            PathNode.QuadTo(461.9f, 972.0f, 473.9f, 974.0f),
                            PathNode.QuadTo(563.9f, 994.0f, 651.4f, 976.5f),
                            PathNode.QuadTo(738.9f, 959.0f, 811.4f, 908.0f),
                            PathNode.QuadTo(883.9f, 857.0f, 928.9f, 778.0f),
                            PathNode.QuadTo(970.9f, 706.0f, 981.4f, 625.5f),
                            PathNode.QuadTo(991.9f, 545.0f, 971.4f, 467.5f),
                            PathNode.QuadTo(950.9f, 390.0f, 903.9f, 326.0f),
                            PathNode.QuadTo(900.9f, 322.0f, 896.4f, 322.0f),
                            PathNode.QuadTo(891.9f, 322.0f, 887.9f, 327.0f),
                            PathNode.LineTo(871.9f, 347.0f),
                            PathNode.QuadTo(865.9f, 354.0f, 856.9f, 356.0f),
                            PathNode.QuadTo(847.9f, 358.0f, 839.9f, 353.0f),
                            PathNode.QuadTo(831.9f, 348.0f, 827.9f, 334.0f),
                            PathNode.LineTo(789.9f, 191.0f),
                            PathNode.QuadTo(786.9f, 179.0f, 793.4f, 170.0f),
                            PathNode.QuadTo(799.9f, 161.0f, 812.9f, 161.0f),
                            PathNode.LineTo(966.9f, 165.0f),
                            PathNode.QuadTo(977.9f, 165.0f, 983.9f, 171.5f),
                            PathNode.QuadTo(989.9f, 178.0f, 989.9f, 187.5f),
                            PathNode.QuadTo(989.9f, 197.0f, 983.9f, 204.0f),
                            PathNode.Close,
                            PathNode.MoveTo(702.9f, 125.0f),
                            PathNode.LineTo(686.9f, 153.0f),
                            PathNode.QuadTo(682.9f, 162.0f, 674.4f, 165.5f),
                            PathNode.QuadTo(665.9f, 169.0f, 651.9f, 166.0f),
                            PathNode.QuadTo(564.9f, 150.0f, 480.9f, 169.0f),
                            PathNode.QuadTo(396.9f, 188.0f, 327.4f, 238.5f),
                            PathNode.QuadTo(257.9f, 289.0f, 213.9f, 365.0f),
                            PathNode.QuadTo(172.9f, 436.0f, 161.9f, 515.5f),
                            PathNode.QuadTo(150.9f, 595.0f, 170.4f, 672.0f),
                            PathNode.QuadTo(189.9f, 749.0f, 235.9f, 813.0f),
                            PathNode.QuadTo(239.9f, 818.0f, 246.4f, 818.0f),
                            PathNode.QuadTo(252.9f, 818.0f, 256.9f, 813.0f),
                            PathNode.LineTo(275.9f, 790.0f),
                            PathNode.QuadTo(281.9f, 783.0f, 290.4f, 782.0f),
                            PathNode.QuadTo(298.9f, 781.0f, 306.4f, 785.5f),
                            PathNode.QuadTo(313.9f, 790.0f, 315.9f, 799.0f),
                            PathNode.LineTo(354.9f, 941.0f),
                            PathNode.QuadTo(359.9f, 958.0f, 353.9f, 968.5f),
                            PathNode.QuadTo(347.9f, 979.0f, 332.9f, 978.0f),
                            PathNode.LineTo(177.9f, 974.0f),
                            PathNode.QuadTo(167.9f, 974.0f, 162.4f, 967.5f),
                            PathNode.QuadTo(156.9f, 961.0f, 156.4f, 952.5f),
                            PathNode.QuadTo(155.9f, 944.0f, 159.9f, 938.0f),
                            PathNode.LineTo(177.9f, 913.0f),
                            PathNode.QuadTo(184.9f, 904.0f, 185.4f, 896.5f),
                            PathNode.QuadTo(185.9f, 889.0f, 180.9f, 882.0f),
                            PathNode.QuadTo(118.9f, 802.0f, 91.9f, 706.0f),
                            PathNode.QuadTo(64.9f, 610.0f, 76.4f, 510.5f),
                            PathNode.QuadTo(87.9f, 411.0f, 139.9f, 322.0f),
                            PathNode.QuadTo(193.9f, 228.0f, 280.9f, 166.5f),
                            PathNode.QuadTo(367.9f, 105.0f, 472.4f, 83.0f),
                            PathNode.QuadTo(576.9f, 61.0f, 682.9f, 85.0f),
                            PathNode.QuadTo(698.9f, 89.0f, 704.4f, 101.5f),
                            PathNode.QuadTo(709.9f, 114.0f, 702.9f, 125.0f),
                            PathNode.Close,
                        ),
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        pathFillType = PathFillType.NonZero,
                    )
                }
            }.build()
}
