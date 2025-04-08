/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network.danmaku

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.him188.ani.app.data.network.protocol.AniUser
import me.him188.ani.app.data.network.protocol.BangumiLoginRequest
import me.him188.ani.app.data.network.protocol.BangumiLoginResponse
import me.him188.ani.app.domain.foundation.ServerListFeatureConfig.Companion.MAGIC_ANI_SERVER
import me.him188.ani.app.platform.currentAniBuildConfig
import me.him188.ani.app.ui.foundation.BackgroundScope
import me.him188.ani.app.ui.foundation.HasBackgroundScope
import me.him188.ani.app.ui.foundation.launchInBackground
import me.him188.ani.client.apis.DanmakuAniApi
import me.him188.ani.client.models.AniDanmakuInfo
import me.him188.ani.client.models.AniDanmakuLocation
import me.him188.ani.danmaku.api.DanmakuContent
import me.him188.ani.danmaku.api.DanmakuInfo
import me.him188.ani.danmaku.api.DanmakuLocation
import me.him188.ani.utils.coroutines.retryWithBackoffDelay
import me.him188.ani.utils.ktor.ApiInvoker
import me.him188.ani.utils.ktor.ScopedHttpClient
import me.him188.ani.utils.logging.error
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.platform.currentPlatform
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

sealed class SendDanmakuException : Exception()
class AuthorizationFailureException(override val cause: Throwable?) : SendDanmakuException()
class RequestFailedException(
    override val message: String?,
    override val cause: Throwable? = null
) : SendDanmakuException()

class NetworkErrorException(override val cause: Throwable?) : SendDanmakuException()

class AniDanmakuSender(
    private val client: ScopedHttpClient,
    private val aniDanmakuApi: ApiInvoker<DanmakuAniApi>,
    private val bangumiToken: Flow<String?>,
    parentCoroutineContext: CoroutineContext = EmptyCoroutineContext,
) : AutoCloseable, HasBackgroundScope by BackgroundScope(parentCoroutineContext) {
    private fun getBaseUrl() = MAGIC_ANI_SERVER

    companion object {
        private val logger = logger<AniDanmakuSender>()
    }

    private suspend fun getUserInfo(token: String): AniUser {
        return client.use {
            @Suppress("LEAKING_HTTP_RESPONSE")
            invokeRequest {
                get("${getBaseUrl()}/v1/me") {
                    configureTimeout()
                    bearerAuth(token)
                }
            }.body<AniUser>()
        }
    }

    private fun HttpRequestBuilder.configureTimeout() {
        timeout {
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 30_000
        }
    }

    private inline fun invokeRequest(
        block: () -> HttpResponse
    ): HttpResponse {
        val resp = try {
            block()
        } catch (e: Throwable) {
            throw NetworkErrorException(e)
        }
        if (resp.status.value == 401) {
            throw AuthorizationFailureException(null)
        }
        if (!resp.status.isSuccess()) {
            throw RequestFailedException(resp.toString())
        }
        return resp
    }

    private suspend fun authByBangumiToken(
        bangumiToken: String
    ): String {
        return client.use {
            @Suppress("LEAKING_HTTP_RESPONSE")
            invokeRequest {
                post("${getBaseUrl()}/v1/login/bangumi") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        BangumiLoginRequest(
                            bangumiToken,
                            clientVersion = currentAniBuildConfig.versionName,
                            clientOS = currentPlatform().name,
                            clientArch = currentPlatform().arch.displayName,
                        ),
                    )
                }
            }.body<BangumiLoginResponse>().token
        }
    }

    private suspend fun sendDanmaku(
        episodeId: Int,
        content: DanmakuContent,
    ) {
        val token = requireSession().token
        aniDanmakuApi {
            postDanmaku(
                episodeId.toString(),
                me.him188.ani.client.models.AniDanmakuPostRequest(
                    AniDanmakuInfo(
                        color = content.color,
                        location = content.location.toAniLocation(),
                        playTime = content.playTimeMillis,
                        text = content.text,
                    ),
                ),
            ).body()
        }
    }

    private class Session(
        val token: String,
        val selfInfo: AniUser,
    )

    private val session = MutableStateFlow<Session?>(null)

    private suspend inline fun requireSession() =
        session.value ?: run {
            login()
        }

    private val loginLock = Mutex()
    private suspend fun login(): Session = loginLock.withLock {
        session.value?.let { return it }
        return suspend {
            val bangumiToken = bangumiToken.first()
                ?: throw AuthorizationFailureException(null)

            val token = authByBangumiToken(bangumiToken)
            val selfInfo = getUserInfo(token)
            val session = Session(token, selfInfo)
            this.session.value = session
            session
        }.asFlow().retryWithBackoffDelay { e, _ ->
            e !is AuthorizationFailureException
        }.first()
    }

    val selfId = session.map { it?.selfInfo?.id }

    private val sendLock = Mutex()

    @Throws(SendDanmakuException::class, CancellationException::class)
    suspend fun send(episodeId: Int, info: DanmakuContent): DanmakuInfo = sendLock.withLock {
        val selfId = requireSession().selfInfo.id

        sendDanmaku(episodeId, info)

        DanmakuInfo(
            "self" + Random.nextInt(),
            AniDanmakuProvider.ID,
            selfId,
            DanmakuContent(
                info.playTimeMillis,
                info.color,
                info.text,
                info.location,
            ),
        )
    }

    init {
        launchInBackground {
            runCatching { login() }.onFailure {
                logger.error(it) { "Failed to login to danmaku sever (on startup). Will try later when sending danmaku." }
            }
        }
    }

    override fun close() {
        backgroundScope.cancel()
    }
}

private fun DanmakuLocation.toAniLocation(): AniDanmakuLocation {
    return when (this) {
        DanmakuLocation.TOP -> AniDanmakuLocation.TOP
        DanmakuLocation.BOTTOM -> AniDanmakuLocation.BOTTOM
        DanmakuLocation.NORMAL -> AniDanmakuLocation.NORMAL
    }
}
