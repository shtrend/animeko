/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.layout.currentWindowAdaptiveInfo1
import me.him188.ani.app.ui.foundation.layout.isHeightAtLeastMedium
import me.him188.ani.app.ui.foundation.layout.isHeightCompact
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastBreakpoint
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastExpanded
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastMedium
import me.him188.ani.app.ui.foundation.layout.isWidthCompact


@Immutable
data class CoverSubjectGridLayoutParams(
    val gridCells: GridCells,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalItemArrangement: Arrangement.Vertical,
    val verticalItemSpacing: Dp,
    val cardShape: Shape,
)

object CoverSubjectGridDefaults {
    @Composable
    fun layoutParameters(windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo1()): CoverSubjectGridLayoutParams {
        val windowSizeClass = windowAdaptiveInfo.windowSizeClass

        val arrangement = when {
            windowSizeClass.isWidthAtLeastExpanded -> Arrangement.spacedBy(16.dp)
            windowSizeClass.isWidthAtLeastMedium -> Arrangement.spacedBy(12.dp)
            else -> Arrangement.spacedBy(8.dp)
        }
        return CoverSubjectGridLayoutParams(
            gridCells = when {
                windowSizeClass.isWidthCompact || windowSizeClass.isHeightCompact -> GridCells.Adaptive(minSize = 100.dp)

                windowSizeClass.isWidthAtLeastBreakpoint(1200) -> {
                    GridCells.Adaptive(minSize = 180.dp)
                }

                windowSizeClass.isWidthAtLeastExpanded -> {
                    GridCells.Adaptive(minSize = 150.dp)
                }

                windowSizeClass.isWidthAtLeastMedium -> {
                    GridCells.Adaptive(minSize = 128.dp)
                }

                else -> { // should not happen
                    GridCells.Adaptive(minSize = 100.dp)
                }
            },
            horizontalArrangement = arrangement,
            verticalItemArrangement = arrangement,
            verticalItemSpacing = if (windowSizeClass.isHeightAtLeastMedium) 16.dp else 8.dp,
            cardShape = MaterialTheme.shapes.large,
        )
    }
}
