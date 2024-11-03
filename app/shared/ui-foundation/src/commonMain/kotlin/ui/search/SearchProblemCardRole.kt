/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.search

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * @see SearchProblemCardRole.from
 * @see SearchProblem
 */
@Stable
sealed class SearchProblemCardRole {
    @Composable
    internal abstract fun Container(
        modifier: Modifier = Modifier,
        containerColor: Color = SearchDefaults.searchProblemContainerColor,
        shape: Shape = MaterialTheme.shapes.large, // behave like Dialogs.
        content: @Composable SearchProblemCardScope.() -> Unit,
    )

    @Stable
    data object Neural : SearchProblemCardRole() {
        @Composable
        override fun Container(
            modifier: Modifier,
            containerColor: Color,
            shape: Shape,
            content: @Composable SearchProblemCardScope.() -> Unit,
        ) {
            val colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor,
            )
            val colorsState by rememberUpdatedState(colors)
            val scope = remember {
                object : SearchProblemCardScope {
                    override val cardColors: CardColors @Composable get() = colorsState
                }
            }
            ElevatedCard(
                modifier, shape = shape,
                colors = colors,
            ) {
                scope.content()
            }
        }
    }

    @Stable
    data object Unimportant : SearchProblemCardRole() {
        @Composable
        override fun Container(
            modifier: Modifier,
            containerColor: Color,
            shape: Shape,
            content: @Composable SearchProblemCardScope.() -> Unit,
        ) {
            val colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            val colorsState by rememberUpdatedState(colors)
            val scope = remember {
                object : SearchProblemCardScope {
                    override val cardColors: CardColors @Composable get() = colorsState
                }
            }
            ElevatedCard(
                modifier,
                colors = colors, // no 'boxing'
                elevation = CardDefaults.cardElevation(), // no elevation
            ) {
                scope.content()
            }
        }
    }

    @Stable
    data object Important : SearchProblemCardRole() {
        @Composable
        override fun Container(
            modifier: Modifier,
            containerColor: Color,
            shape: Shape,
            content: @Composable SearchProblemCardScope.() -> Unit,
        ) {
            val colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.error,
            )
            val colorsState by rememberUpdatedState(colors)
            val scope = remember {
                object : SearchProblemCardScope {
                    override val cardColors: CardColors @Composable get() = colorsState
                }
            }
            ElevatedCard(
                modifier, shape = shape,
                colors = colors,
            ) {
                scope.content()
            }
        }
    }

    @Stable
    data object Suggestive : SearchProblemCardRole() {
        @Composable
        override fun Container(
            modifier: Modifier,
            containerColor: Color,
            shape: Shape,
            content: @Composable SearchProblemCardScope.() -> Unit,
        ) {
            val colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.primary,
            )
            val colorsState by rememberUpdatedState(colors)
            val scope = remember {
                object : SearchProblemCardScope {
                    override val cardColors: CardColors @Composable get() = colorsState
                }
            }
            ElevatedCard(
                modifier, shape = shape,
                colors = colors,
            ) {
                scope.content()
            }
        }
    }

    companion object {
        fun from(problem: SearchProblem): SearchProblemCardRole {
            return when (problem) {
                // Important error
                is SearchProblem.UnknownError,
                SearchProblem.ServiceUnavailable,
                SearchProblem.NetworkError,
                    -> Important

                // Suggestive message
                SearchProblem.RequiresLogin -> Suggestive

                // Neutral message
                SearchProblem.RateLimited -> Neural

                // Unimportant message
                SearchProblem.NoResults -> Unimportant
            }
        }
    }
}