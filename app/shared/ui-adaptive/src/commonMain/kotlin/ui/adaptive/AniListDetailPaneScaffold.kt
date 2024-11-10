/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.layout.AnimatedPane1
import me.him188.ani.app.ui.foundation.layout.paneHorizontalPadding
import me.him188.ani.app.ui.foundation.navigation.BackHandler


/**
 * 自动适应单页模式和双页模式的布局的 paddings
 *
 * Pane 内可以访问 [PaneScope]. 其中有几个非常实用的属性:
 * - [PaneScope.paneContentPadding]: 用于为 pane 增加自动的 content padding. 通常你需要为 pane 的内容直接添加这个 modifier.
 *   如果你不期望为整个容器添加 padding, 可以使用 [PaneScope.listDetailLayoutParameters] [ListDetailLayoutParameters.listPaneContentPaddingValues]
 * - [PaneScope.listDetailLayoutParameters] 用于获取当前的布局参数.
 *
 * @param listPaneTopAppBar 通常可以放 [AniTopAppBar], 可以留空. 留空时不会占额外空间.
 * @param listPaneContent 列表内容, 可以是 Column 或者 Grid. 需要自行实现 vertical scroll.
 * @param detailPane 详情内容.
 * @param listPanePreferredWidth See also [androidx.compose.material3.adaptive.layout.PaneScaffoldScope.preferredWidth]
 * @param useSharedTransition 是否在[单页模式][ListDetailLayoutParameters.isSinglePane]时使用 Container Transform 等 [SharedTransitionLayout] 的动画.
 * 启用后将会调整切换 pane 时的 fade 动画逻辑来适配 Container Transform.
 */
@Composable
fun <T> AniListDetailPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<T>,
    listPaneTopAppBar: @Composable (PaneScope.() -> Unit),
    listPaneContent: @Composable (PaneScope.() -> Unit),
    detailPane: @Composable (PaneScope.() -> Unit),
    modifier: Modifier = Modifier,
    useSharedTransition: Boolean = false,
    listPanePreferredWidth: Dp = Dp.Unspecified,
    layoutParameters: ListDetailLayoutParameters = ListDetailLayoutParameters.calculate(navigator.scaffoldDirective),
) {
    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }
    val layoutParametersState by rememberUpdatedState(layoutParameters)

    SharedTransitionLayout {
        ListDetailPaneScaffold(
            navigator.scaffoldDirective,
            navigator.scaffoldValue,
            listPane = {
                val threePaneScaffoldScope = this
                AnimatedPane1(Modifier.preferredWidth(listPanePreferredWidth), useSharedTransition) {
                    Column {
                        val scope =
                            remember(threePaneScaffoldScope, this@SharedTransitionLayout, this@AnimatedPane1) {
                                object : PaneScope, SharedTransitionScope by this@SharedTransitionLayout {
                                    override val listDetailLayoutParameters: ListDetailLayoutParameters
                                        get() = layoutParametersState
                                    override val animatedVisibilityScope: AnimatedVisibilityScope
                                        get() = this@AnimatedPane1

                                    override fun Modifier.paneContentPadding(): Modifier =
                                        Modifier.padding(layoutParametersState.listPaneContentPaddingValues)
                                }
                            }
                        listPaneTopAppBar(scope)
                        listPaneContent(scope)
                    }
                }
            },
            detailPane = {
                val threePaneScaffoldScope = this
                AnimatedPane1(useSharedTransition = useSharedTransition) {
                    Card(
                        shape = layoutParameters.detailPaneShape,
                        colors = layoutParameters.detailPaneColors,
                    ) {
                        val scope =
                            remember(threePaneScaffoldScope, this@SharedTransitionLayout, this@AnimatedPane1) {
                                object : PaneScope, SharedTransitionScope by this@SharedTransitionLayout {
                                    override val listDetailLayoutParameters: ListDetailLayoutParameters
                                        get() = layoutParametersState
                                    override val animatedVisibilityScope: AnimatedVisibilityScope
                                        get() = this@AnimatedPane1

                                    override fun Modifier.paneContentPadding(): Modifier =
                                        Modifier.padding(layoutParametersState.detailPaneContentPaddingValues)
                                }
                            }
                        detailPane(scope)
                    }
                }
            },
            modifier,
        )
    }
}

@Stable
interface PaneScope : SharedTransitionScope {
    /**
     * 获取当前的布局参数.
     *
     * 若要为 pane 增加 padding, 可优先使用 [paneContentPadding].
     */
    val listDetailLayoutParameters: ListDetailLayoutParameters
    val animatedVisibilityScope: AnimatedVisibilityScope

    /**
     * 为 pane 增加自动的 content padding. 通常应用于 pane 的最外层容器:
     * @sample me.him188.ani.app.ui.foundation.samples.paneContentPadding
     */
    @Stable
    fun Modifier.paneContentPadding(): Modifier
}

@Immutable
data class ListDetailLayoutParameters(
    val listPaneContentPaddingValues: PaddingValues,
    val detailPaneContentPaddingValues: PaddingValues,
    val detailPaneShape: Shape,
    val detailPaneColors: CardColors,
    val isSinglePane: Boolean,
) {
    companion object {
        @Composable
        fun calculate(directive: PaneScaffoldDirective): ListDetailLayoutParameters {
            val isTwoPane = directive.maxHorizontalPartitions > 1
            val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
            return if (isTwoPane) {
                ListDetailLayoutParameters(
                    listPaneContentPaddingValues = PaddingValues(
                        start = windowSizeClass.paneHorizontalPadding,
                        end = 0.dp, // ListDetail 两个 pane 之间自带 24.dp
                    ),
                    detailPaneContentPaddingValues = PaddingValues(0.dp),
                    detailPaneShape = MaterialTheme.shapes.extraLarge.copy(
                        topEnd = ZeroCornerSize,
                        bottomEnd = ZeroCornerSize,
                    ),
                    detailPaneColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    isSinglePane = false,
                )
            } else {
                ListDetailLayoutParameters(
                    listPaneContentPaddingValues = PaddingValues(horizontal = windowSizeClass.paneHorizontalPadding),
                    detailPaneContentPaddingValues = PaddingValues(horizontal = windowSizeClass.paneHorizontalPadding),
                    detailPaneShape = RectangleShape,
                    detailPaneColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    isSinglePane = true,
                )
            }
        }
    }
}
