/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset
import me.him188.ani.app.ui.foundation.theme.EasingDurations

/**
 * APP 统一动画方案
 *
 * @see NavigationMotionScheme
 */
@Immutable
// use object equality
class AniMotionScheme(
    /**
     * This pattern is used to navigate between top-level destinations of an app, like tapping a destination in a Navigation bar.
     *
     * Commonly used with: Navigation bar, navigation rail, and navigation drawer
     *
     * [M3 Spec](https://m3.material.io/styles/motion/transitions/transition-patterns#f852afd2-396f-49fd-a265-5f6d96680e16)
     */
    val topLevelTransition: ContentTransform = run {
        val outTime = 50
        val inTime = 150
        fadeIn(
            animationSpec = tween(
                durationMillis = inTime,
                delayMillis = outTime,
                easing = StandardDecelerateEasing,
            ),
        ).togetherWith(
            fadeOut(
                animationSpec = tween(
                    durationMillis = outTime,
                    delayMillis = 0,
                    easing = StandardAccelerateEasing,
                ),
            ),
        )
    },
    /**
     * @see LazyItemScope.animateItem
     */
    val feedItemFadeInSpec: FiniteAnimationSpec<Float>,
    /**
     * @see LazyItemScope.animateItem
     */
    val feedItemPlacementSpec: FiniteAnimationSpec<IntOffset>,
    /**
     * @see LazyItemScope.animateItem
     */
    val feedItemFadeOutSpec: FiniteAnimationSpec<Float>,

    val standardAnimatedContentTransition: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        // Follow M3 Clean fades
        val fadeIn = fadeIn(
            tween(
                EasingDurations.standardAccelerate,
                delayMillis = EasingDurations.standardDecelerate,
                easing = StandardAccelerateEasing,
            ),
        )
        val fadeOut = fadeOut(
            tween(EasingDurations.standardDecelerate, easing = StandardDecelerateEasing),
        )
        fadeIn.togetherWith(fadeOut)
    },
) {
    internal companion object {
        internal val Default = kotlin.run {
            val feedItemFadeOutTime = EasingDurations.standardAccelerate
            val feedItemFadeInTime = EasingDurations.standardDecelerate
            AniMotionScheme(
                feedItemFadeInSpec = tween(
                    durationMillis = feedItemFadeInTime,
                    delayMillis = feedItemFadeOutTime,
                    easing = StandardDecelerateEasing,
                ),
                feedItemFadeOutSpec = tween(
                    durationMillis = feedItemFadeOutTime,
                    delayMillis = 0,
                    easing = StandardAccelerateEasing,
                ),
                feedItemPlacementSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold,
                ),
            )
        }
    }
}

@Stable
val LocalAniMotionScheme: ProvidableCompositionLocal<AniMotionScheme> =
    staticCompositionLocalOf { AniMotionScheme.Default }
