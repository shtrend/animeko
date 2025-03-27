/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.exploration.search

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.collectLatest
import me.him188.ani.app.data.models.preference.NsfwMode
import me.him188.ani.app.domain.search.SearchSort
import me.him188.ani.app.ui.foundation.animation.AniMotionScheme
import me.him188.ani.app.ui.foundation.animation.LocalAniMotionScheme
import me.him188.ani.app.ui.foundation.interaction.keyboardDirectionToSelectItem
import me.him188.ani.app.ui.foundation.interaction.keyboardPageToScroll
import me.him188.ani.app.ui.foundation.layout.currentWindowAdaptiveInfo1
import me.him188.ani.app.ui.foundation.layout.paneHorizontalPadding
import me.him188.ani.app.ui.foundation.layout.paneVerticalPadding
import me.him188.ani.app.ui.foundation.widgets.NsfwMask
import me.him188.ani.app.ui.foundation.widgets.SelectableDropdownMenuItem
import me.him188.ani.app.ui.search.LoadErrorCard
import me.him188.ani.app.ui.search.SearchDefaults.IconTextButton
import me.him188.ani.app.ui.search.SearchResultLazyVerticalStaggeredGrid
import me.him188.ani.app.ui.search.hasFirstPage
import me.him188.ani.app.ui.search.isFinishedAndEmpty


@Composable
internal fun SearchResultColumn(
    items: LazyPagingItems<SubjectPreviewItemInfo>,
    summary: @Composable SearchResultColumnScope.() -> Unit, // 可在还没发起任何搜索时不展示
    selectedItemIndex: () -> Int,
    onSelect: (index: Int) -> Unit,
    onPlay: (info: SubjectPreviewItemInfo) -> Unit,
    highlightSelected: Boolean = true,
    modifier: Modifier = Modifier,
    headers: LazyStaggeredGridScope.() -> Unit = {},
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    var height by rememberSaveable { mutableIntStateOf(0) }
    val bringIntoViewRequesters = remember { mutableStateMapOf<Int, BringIntoViewRequester>() }
    val nsfwBlurShape = SubjectItemLayoutParameters.calculate(currentWindowAdaptiveInfo1().windowSizeClass).shape
    val aniMotionScheme = LocalAniMotionScheme.current

    val itemsState = rememberUpdatedState(items)
    SearchResultLazyVerticalStaggeredGrid(
        items,
        error = {
            LoadErrorCard(
                error = it,
                onRetry = { items.retry() },
                modifier = Modifier.fillMaxWidth(), // noop
            )
        },
        modifier
            .focusGroup()
            .onSizeChanged { height = it.height }
            .keyboardDirectionToSelectItem(
                selectedItemIndex,
            ) {
                state.animateScrollToItem(it)
                onSelect(it)
            }
            .keyboardPageToScroll({ height.toFloat() }) {
                state.animateScrollBy(it)
            },
        lazyStaggeredGridState = state,
        horizontalArrangement = Arrangement.spacedBy(currentWindowAdaptiveInfo1().windowSizeClass.paneHorizontalPadding),
    ) {
        headers()

        item(span = StaggeredGridItemSpan.FullLine) {
            val scope = remember(this, itemsState, aniMotionScheme) {
                SearchResultColumnScopeImpl(itemsState, aniMotionScheme)
            }

            scope.summary()
        }

        items(
            items.itemCount,
            key = items.itemKey { it.subjectId },
            contentType = items.itemContentType { 1 },
        ) { index ->
            val info = items[index]
            val requester = remember { BringIntoViewRequester() }
            // 记录 item 对应的 requester
            if (info != null && !info.hide) {
                DisposableEffect(requester) {
                    bringIntoViewRequesters[info.subjectId] = requester
                    onDispose {
                        bringIntoViewRequesters.remove(info.subjectId)
                    }
                }

                var nsfwMaskState: NsfwMode by rememberSaveable(info) {
                    mutableStateOf(info.nsfwMode)
                }
                NsfwMask(
                    mode = nsfwMaskState,
                    onTemporarilyDisplay = { nsfwMaskState = NsfwMode.DISPLAY },
                    shape = nsfwBlurShape,
                ) {
                    SubjectPreviewItem(
                        selected = highlightSelected && index == selectedItemIndex(),
                        onClick = { onSelect(index) },
                        onPlay = { onPlay(info) },
                        info = info,
                        Modifier
//                        .sharedElement(
//                            rememberSharedContentState(SharedTransitionKeys.subjectBounds(info.subjectId)),
//                            animatedVisibilityScope,
//                        )
                            .animateItem(
                                fadeInSpec = aniMotionScheme.feedItemFadeInSpec,
                                placementSpec = aniMotionScheme.feedItemPlacementSpec,
                                fadeOutSpec = aniMotionScheme.feedItemFadeOutSpec,
                            )
                            .fillMaxWidth()
                            .bringIntoViewRequester(requester)
                            .padding(vertical = currentWindowAdaptiveInfo1().windowSizeClass.paneVerticalPadding / 2),
                        image = {
                            SubjectItemDefaults.Image(
                                info.imageUrl,
//                            Modifier.sharedElement(
//                                rememberSharedContentState(SharedTransitionKeys.subjectCoverImage(subjectId = info.subjectId)),
//                                animatedVisibilityScope,
//                            ),
                            )
                        },
                        title = { maxLines ->
                            Text(
                                info.title,
//                            Modifier.sharedElement(
//                                rememberSharedContentState(SharedTransitionKeys.subjectTitle(subjectId = info.subjectId)),
//                                animatedVisibilityScope,
//                            ),
                                maxLines = maxLines,
                            )
                        },
                    )
                }
            } else {
                Box(Modifier.size(Dp.Hairline))
                // placeholder
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow(selectedItemIndex)
            .collectLatest {
                bringIntoViewRequesters[items.itemSnapshotList.getOrNull(it)?.subjectId]?.bringIntoView()
            }
    }
}

@Suppress("FunctionName")
private fun LazyStaggeredGridItemScope.SearchResultColumnScopeImpl(
    itemsState: State<LazyPagingItems<SubjectPreviewItemInfo>>,
    aniMotionScheme: AniMotionScheme,
): SearchResultColumnScope = object : SearchResultColumnScope {
    @Composable
    override fun SearchSummary(currentSort: SearchSort, onSortChange: (SearchSort) -> Unit, modifier: Modifier) {
        val modifier1 = modifier.animateItem(
            fadeInSpec = aniMotionScheme.feedItemFadeInSpec,
            placementSpec = aniMotionScheme.feedItemPlacementSpec,
            fadeOutSpec = aniMotionScheme.feedItemFadeOutSpec,
        )
        when {
            itemsState.value.isFinishedAndEmpty -> {
                ListItem(
                    headlineContent = { Text("无搜索结果") },
                    modifier = modifier1,
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                )
            }

            itemsState.value.hasFirstPage -> {
                Surface(modifier1, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.aligned(Alignment.CenterVertically),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("已显示 ${itemsState.value.itemCount} 个结果", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.align(Alignment.Bottom),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SortButton(
                                currentSort,
                                onSortChange,
                            )
                        }
                    }
                }
            }

            else -> {
                Spacer(modifier1.height(Dp.Hairline)) // 如果空白内容, 它可能会有 bug
            }
        }
    }
}

interface SearchResultColumnScope {
    @Composable
    fun SearchSummary(
        currentSort: SearchSort,
        onSortChange: (SearchSort) -> Unit,
        modifier: Modifier = Modifier,
    )
}


@Composable
private fun SortButton(
    currentSort: SearchSort,
    onSortChange: (SearchSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier, contentAlignment = Alignment.BottomEnd,
    ) {
        var showDropdown by rememberSaveable {
            mutableStateOf(false)
        }
        IconTextButton(
            onClick = { showDropdown = true },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Rounded.Sort, null)
            },
        ) {
            Text(getSortText(currentSort), softWrap = false)
        }
        DropdownMenu(showDropdown, { showDropdown = false }) {
            for (sort in SearchSort.entries) {
                SelectableDropdownMenuItem(
                    selected = sort == currentSort,
                    text = {
                        Text(
                            getSortText(sort),
                            softWrap = false,
                        )
                    },
                    onClick = {
                        showDropdown = false
                        onSortChange(sort)
                    },
                )
            }
        }
    }
}

private fun getSortText(currentSort: SearchSort): String = when (currentSort) {
    SearchSort.MATCH -> "最佳匹配"
    SearchSort.COLLECTION -> "最多收藏"
    SearchSort.RANK -> "最高排名"
}
