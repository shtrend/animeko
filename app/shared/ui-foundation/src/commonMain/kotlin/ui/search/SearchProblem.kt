/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.paging.CombinedLoadStates
import androidx.paging.compose.LazyPagingItems
import me.him188.ani.app.data.repository.RepositoryAuthorizationException
import me.him188.ani.app.data.repository.RepositoryNetworkException
import me.him188.ani.app.data.repository.RepositoryRateLimitedException
import me.him188.ani.app.data.repository.RepositoryServiceUnavailableException
import me.him188.ani.app.tools.paging.exceptions

/**
 * 搜索时遇到的问题.
 */
sealed class SearchProblem {
    data object NoResults : SearchProblem()
    data object RequiresLogin : SearchProblem()
    data object NetworkError : SearchProblem()
    data object ServiceUnavailable : SearchProblem()
    data object RateLimited : SearchProblem()
    data class UnknownError(val throwable: Throwable?) : SearchProblem()

    companion object {
        fun fromCombinedLoadStates(states: CombinedLoadStates): SearchProblem? {
            if (!states.hasError) {
                return null
            }
            val exceptions = states.exceptions()
            for (e in exceptions) {
                when (e) {
                    is RepositoryAuthorizationException -> return RequiresLogin
                    is RepositoryNetworkException -> return NetworkError
                    is RepositoryServiceUnavailableException -> return ServiceUnavailable
                    is RepositoryRateLimitedException -> return RateLimited
                }
            }
            return UnknownError(exceptions.firstOrNull())
        }

        fun fromException(e: Throwable): SearchProblem {
            return when (e) {
                is RepositoryAuthorizationException -> RequiresLogin
                is RepositoryNetworkException -> NetworkError
                is RepositoryServiceUnavailableException -> ServiceUnavailable
                is RepositoryRateLimitedException -> RateLimited
                else -> UnknownError(e)
            }
        }
    }
}

@Composable
fun <T : Any> LazyPagingItems<T>.rememberSearchProblemState(): State<SearchProblem?> {
    return remember(this) {
        derivedStateOf {
            SearchProblem.fromCombinedLoadStates(loadState)
        }
    }
}

