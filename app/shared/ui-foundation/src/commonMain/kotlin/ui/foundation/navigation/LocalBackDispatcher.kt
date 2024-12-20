/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import me.him188.ani.app.navigation.LocalNavigator

fun interface BackDispatcher {
    /**
     * Call the most recently registered [BackHandler], if any.
     *
     * If no [BackHandler] has been registered, this will call [AniNavigator.popBackStack].
     */
    fun onBackPressed()
}

/**
 * 可模拟点击返回键
 */
object LocalBackDispatcher {
    /**
     * @see BackDispatcher
     */
    val current: BackDispatcher
        @Composable
        get() {
            val backPressed by rememberUpdatedState(LocalOnBackPressedDispatcherOwner.current)
            val navigator by rememberUpdatedState(LocalNavigator.current)
            return remember {
                BackDispatcher {
                    backPressed?.onBackPressedDispatcher?.onBackPressed()
                        ?: kotlin.run {
                            navigator.popBackStack()
                        }
                }
            }
        }
}
