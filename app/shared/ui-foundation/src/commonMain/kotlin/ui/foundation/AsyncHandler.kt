/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.him188.ani.app.domain.foundation.LoadError
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.foundation.widgets.showLoadError
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 一个简单的异步任务处理器. 设计为了处理 `onClick` 等回调时调用后台任务.
 *
 * 使用 [launch] 启用一个协程, 其异常将会被捕获, 然后被对应的 [rememberAsyncHandler] 里的 callback 处理.
 */
@Stable
interface AsyncHandler {
    /**
     * 此时是否有任何任务正在进行中
     */
    val isWorking: Boolean

    /**
     * 在 UI 线程启动一个异步任务.
     *
     * [block] 内的任何异常 [Throwable] 将被捕获, 通过 [rememberAsyncHandler] 时传入的 `onException` 处理.
     *
     * 如果 [coroutineContext] 没有指定 [ContinuationInterceptor], 且 [launch] 是在 UI 线程调用的, 则会立即在当前线程执行 [block] 直到第一个 suspension point.
     * 这样没有 dispatch latency, 可以协助完成 UI 的防抖等操作.
     */
    fun launch(
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Job
}

/**
 * Remember 一个使用 [me.him188.ani.app.ui.foundation.widgets.Toaster] 处理所有异常的 [AsyncHandler].
 *
 * @see AsyncHandler
 */
@Composable
fun rememberAsyncHandler(): AsyncHandler {
    val toaster by rememberUpdatedState(LocalToaster.current)
    return rememberAsyncHandler(
        onException = { e ->
            toaster.showLoadError(LoadError.fromException(e))
        },
    )
}

/**
 * Remember 一个自定义处理异常的 [AsyncHandler].
 *
 * @param onException 处理异常的函数. 在 [AsyncHandler.launch] 发生任何异常时都会被调用.
 *
 * @see AsyncHandler
 * @see LoadError
 */
@Composable
fun rememberAsyncHandler(
    onException: (Throwable) -> Unit,
): AsyncHandler {
    val scope = rememberCoroutineScope()
    val onExceptionState = rememberUpdatedState(onException)
    val handler = remember(scope, onExceptionState) {
        object : AsyncHandler {
            private val workingTaskCount = mutableIntStateOf(0)
            override val isWorking: Boolean get() = workingTaskCount.intValue > 0

            override fun launch(
                coroutineContext: CoroutineContext,
                block: suspend CoroutineScope.() -> Unit,
            ): Job {
                workingTaskCount.intValue++
                return scope.launch(
                    Dispatchers.Main.immediate + coroutineContext, // 保证默认使用 Main
                    start = CoroutineStart.UNDISPATCHED,
                ) {
                    // UNDISPATCHED, 所以会在当前线程立即执行, 注意, 这可能是非期望的线程.
                    try {
                        val dispatcher = coroutineContext[ContinuationInterceptor] ?: Dispatchers.Main.immediate
                        withContext(dispatcher) {
                            // 如果 [coroutineContext] 没有指定 ContinuationInterceptor, 而且 launch 是在 Main 线程调用的, 则此处会立即开始执行 block, 没有 dispatch latency.
                            block()
                        }
                    } catch (e: Throwable) {
                        onExceptionState.value(e)
                    } finally {
                        workingTaskCount.intValue--
                    }
                }
            }
        }
    }
    return handler
}
