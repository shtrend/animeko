/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.torrent.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import me.him188.ani.app.domain.torrent.IRemoteAniTorrentEngine
import me.him188.ani.utils.logging.debug
import me.him188.ani.utils.logging.error
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.logging.warn

class TorrentServiceConnection(
    private val context: Context,
    private val onRequiredRestartService: () -> ComponentName?,
) : LifecycleEventObserver, ServiceConnection, BroadcastReceiver() {
    private val logger = logger<TorrentServiceConnection>()

    private var binder: CompletableDeferred<IRemoteAniTorrentEngine> = CompletableDeferred()
    val connected: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val lock = SynchronizedObject()

    /**
     * service 断开连接后是否需要立即重启,
     */
    private var shouldRestartServiceImmediately = false
    private val restartServiceIntentFilter = IntentFilter(AniTorrentService.INTENT_STARTUP)
    
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // 立即获取 bind 结果, 并更新 connected 状态
                // 如果在 ON_START 时 onServiceConnected 还没有回调
                // 那 ON_START 会尝试重启 service
                connected.value = bindService()
            }

            Lifecycle.Event.ON_START -> {
                shouldRestartServiceImmediately = true
                // 如果 app 在后台时断开了 service 连接, 需要在切回前台时重新启动
                if (!connected.value) {
                    restartService()
                }
            }

            Lifecycle.Event.ON_STOP -> {
                shouldRestartServiceImmediately = false
            }

            Lifecycle.Event.ON_DESTROY -> {
                try {
                    context.unbindService(this)
                } catch (ex: IllegalArgumentException) {
                    logger.warn { "Failed to unregister AniTorrentService service." }
                }
            }

            else -> {}
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        logger.debug { "AniTorrentService is connected, name = $name" }
        if (service != null) {
            val result = IRemoteAniTorrentEngine.Stub.asInterface(service)

            synchronized(lock) {
                if (binder.isCompleted) {
                    binder = CompletableDeferred(result)
                } else {
                    binder.complete(result)
                }
            }
            connected.value = true
        } else {
            logger.error { "Failed to get binder of AniTorrentService." }
            connected.value = false
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        logger.debug { "AniTorrentService is disconnected, name = $name" }
        synchronized(lock) {
            binder = CompletableDeferred()
        }
        connected.value = false

        // app activity 还存在时必须重启 service
        if (shouldRestartServiceImmediately) {
            restartService()
        }
    }

    /**
     * [AniTorrentService] 启动完成时发送广播, 随后 app 应该绑定服务获取接口
     *
     * 首次启动 [AniTorrentService] 完成不在此绑定接口, 由 [onStateChanged] 中的 [Lifecycle.Event.ON_CREATE] 触发绑定.
     *
     * [onServiceDisconnected] 断开连接后将会注册此广播接收 [AniTorrentService] 启动完成事件.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        logger.debug { "AniTorrentService is restarted, rebinding." }
        bindService()
        this.context.unregisterReceiver(this)
    }

    private fun restartService() {
        ContextCompat.registerReceiver(
            context,
            this,
            restartServiceIntentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        logger.debug { "AniTorrentService is disconnected while app is running, restarting." }
        onRequiredRestartService()
    }

    private fun bindService(): Boolean {
        val bindResult = context.bindService(
            Intent(context, AniTorrentService::class.java), this, Context.BIND_ABOVE_CLIENT,
        )
        if (!bindResult) logger.error { "Failed to bind AniTorrentService." }
        return bindResult
    }

    suspend fun awaitBinder(): IRemoteAniTorrentEngine {
        return binder.await()
    }
}