/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.exploration.recommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import me.him188.ani.app.data.models.recommend.RecommendedItemInfo
import me.him188.ani.app.data.models.recommend.RecommendedSubjectInfo
import me.him188.ani.app.data.models.recommend.TestRecommendedItemInfos
import me.him188.ani.app.data.models.recommend.id
import me.him188.ani.app.data.models.recommend.type
import me.him188.ani.app.ui.external.placeholder.placeholder
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import me.him188.ani.app.ui.foundation.animation.LocalAniMotionScheme
import me.him188.ani.app.ui.foundation.layout.BasicCarouselItem
import me.him188.ani.app.ui.foundation.layout.CarouselItemDefaults
import me.him188.ani.app.ui.foundation.layout.currentWindowAdaptiveInfo1
import me.him188.ani.app.ui.foundation.layout.isHeightAtLeastMedium
import me.him188.ani.app.ui.foundation.layout.isHeightCompact
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastBreakpoint
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastExpanded
import me.him188.ani.app.ui.foundation.layout.isWidthAtLeastMedium
import me.him188.ani.app.ui.foundation.layout.isWidthCompact
import me.him188.ani.app.ui.search.createTestPager
import me.him188.ani.utils.platform.annotations.TestOnly
import org.jetbrains.compose.ui.tooling.preview.PreviewScreenSizes

fun LazyGridScope.recommendationItems(
    data: LazyPagingItems<RecommendedItemInfo>,
    onClick: (RecommendedItemInfo) -> Unit,
    layoutParams: RecommendationLayoutParams,
) {
    items(
        data.itemCount,
        key = data.itemKey { it.id },
        contentType = data.itemContentType { it.type },
    ) { index ->
        val aniMotionScheme = LocalAniMotionScheme.current
        when (val item = data[index]) {
            null,// placeholder
            is RecommendedSubjectInfo -> {
                val animateItem = Modifier
                    .animateItem(
                        fadeInSpec = aniMotionScheme.feedItemFadeInSpec,
                        fadeOutSpec = aniMotionScheme.feedItemFadeOutSpec,
                        placementSpec = aniMotionScheme.feedItemPlacementSpec,
                    )
                RecommendedSubjectCard(
                    item = item,
                    onClick = { item?.let { onClick(it) } },
                    modifier = animateItem,
                    shape = layoutParams.cardShape,
                )
            }
        }
    }
}

@Composable
private fun RecommendedSubjectCard(
    item: RecommendedSubjectInfo?, // null means placeholder
    onClick: () -> Unit,
    shape: Shape = CarouselItemDefaults.shape,
    modifier: Modifier = Modifier,
) {
    BasicCarouselItem(
        label = { CarouselItemDefaults.Text(item?.nameCn ?: "", maxLines = 2) },
        modifier.placeholder(item == null, shape = shape),
        maskShape = shape,
    ) {
        if (item != null) {
            val image = @Composable {
                AsyncImage(
                    item.imageLarge,
                    modifier = Modifier.aspectRatio(9f / 16).fillMaxWidth(),
                    contentDescription = item.nameCn,
                    contentScale = ContentScale.Crop,
                )
            }
            Surface({ onClick() }, content = image)
        } else {
            Box(Modifier.aspectRatio(9f / 16).fillMaxWidth())
        }
    }
}

@Immutable
data class RecommendationLayoutParams(
    val gridCells: GridCells,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalItemArrangement: Arrangement.Vertical,
    val verticalItemSpacing: Dp,
    val cardShape: Shape,
)

@Stable
object RecommendationDefaults {
    @Composable
    fun layoutParameters(windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo1()): RecommendationLayoutParams {
        val windowSizeClass = windowAdaptiveInfo.windowSizeClass

        val useLargeCells = windowSizeClass.isWidthAtLeastMedium && windowSizeClass.isHeightAtLeastMedium
        val arrangement = when {
            windowSizeClass.isWidthAtLeastExpanded -> Arrangement.spacedBy(16.dp)
            windowSizeClass.isWidthAtLeastMedium -> Arrangement.spacedBy(12.dp)
            else -> Arrangement.spacedBy(8.dp)
        }
        return RecommendationLayoutParams(
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

@OptIn(TestOnly::class)
@Composable
@PreviewScreenSizes
private fun PreviewRecommendationVerticalGrid() {
    ProvideCompositionLocalsForPreview {
        val layoutParams = RecommendationDefaults.layoutParameters()
        val aniMotionScheme = LocalAniMotionScheme.current
        val data = createTestPager(TestRecommendedItemInfos).collectAsLazyPagingItems()
        LazyVerticalGrid(
            layoutParams.gridCells,
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = layoutParams.horizontalArrangement,
            verticalArrangement = layoutParams.verticalItemArrangement,
        ) {
            recommendationItems(
                data = data,
                onClick = {},
                layoutParams = layoutParams,
            )
        }
    }
}
