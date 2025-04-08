/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.danmaku

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import me.him188.ani.app.data.network.danmaku.AniDanmakuProvider
import me.him188.ani.app.data.network.danmaku.AniDanmakuSender
import me.him188.ani.app.data.repository.RepositoryException
import me.him188.ani.app.data.repository.user.SettingsRepository
import me.him188.ani.app.domain.foundation.HttpClientProvider
import me.him188.ani.app.domain.foundation.ScopedHttpClientUserAgent
import me.him188.ani.app.domain.foundation.get
import me.him188.ani.app.domain.session.OpaqueSession
import me.him188.ani.app.domain.session.SessionManager
import me.him188.ani.app.domain.session.verifiedAccessToken
import me.him188.ani.app.platform.currentAniBuildConfig
import me.him188.ani.app.platform.getAniUserAgent
import me.him188.ani.app.ui.foundation.BackgroundScope
import me.him188.ani.app.ui.foundation.HasBackgroundScope
import me.him188.ani.client.apis.DanmakuAniApi
import me.him188.ani.danmaku.api.DanmakuContent
import me.him188.ani.danmaku.api.DanmakuInfo
import me.him188.ani.danmaku.api.DanmakuProviderConfig
import me.him188.ani.danmaku.api.provider.DanmakuFetchRequest
import me.him188.ani.danmaku.api.provider.DanmakuFetchResult
import me.him188.ani.danmaku.api.provider.DanmakuMatchInfo
import me.him188.ani.danmaku.api.provider.DanmakuMatchMethod
import me.him188.ani.danmaku.api.provider.DanmakuProvider
import me.him188.ani.danmaku.api.provider.MatchingDanmakuProvider
import me.him188.ani.danmaku.api.provider.SimpleDanmakuProvider
import me.him188.ani.danmaku.dandanplay.DandanplayDanmakuProvider
import me.him188.ani.utils.coroutines.closeOnReplacement
import me.him188.ani.utils.coroutines.mapAutoClose
import me.him188.ani.utils.ktor.ApiInvoker
import me.him188.ani.utils.logging.error
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * 管理多个弹幕源 [DanmakuProvider]
 */
class DanmakuManager(
    parentCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    val danmakuApi: ApiInvoker<DanmakuAniApi>,
) : KoinComponent, HasBackgroundScope by BackgroundScope(parentCoroutineContext) {
    private val settingsRepository: SettingsRepository by inject()
    private val sessionManager: SessionManager by inject()
    private val httpClientProvider: HttpClientProvider by inject()

    private val config = settingsRepository.danmakuSettings.flow.map { config ->
        DanmakuProviderConfig(
            userAgent = getAniUserAgent(),
            useGlobal = config.useGlobal,
            dandanplayAppId = currentAniBuildConfig.dandanplayAppId,
            dandanplayAppSecret = currentAniBuildConfig.dandanplayAppSecret,
        )
    }

    private val providers: Flow<List<DanmakuProvider>> = config.map { config ->
        val client = httpClientProvider.get()
        listOf(
            AniDanmakuProvider(danmakuApi),
            DandanplayDanmakuProvider(
                dandanplayAppId = currentAniBuildConfig.dandanplayAppId,
                dandanplayAppSecret = currentAniBuildConfig.dandanplayAppSecret,
                client,
            ),
        )
    }.shareInBackground(started = SharingStarted.Lazily)

    @OptIn(OpaqueSession::class)
    private val sender: Flow<AniDanmakuSender> = config.mapAutoClose { config ->
        AniDanmakuSender(
            httpClientProvider.get(
                userAgent = ScopedHttpClientUserAgent.ANI,
            ),
            danmakuApi,
            sessionManager.verifiedAccessToken, // TODO: Handle danmaku sender errors 
            backgroundScope.coroutineContext,
        )
    }.closeOnReplacement()
        .shareInBackground(started = SharingStarted.Lazily)

    private companion object {
        private val logger = logger<DanmakuManager>()
    }

    val selfId: Flow<String?> = sender.flatMapLatest { it.selfId }

    suspend fun fetch(
        request: DanmakuFetchRequest,
    ): List<DanmakuFetchResult> {
        logger.info { "Search for danmaku with filename='${request.filename}'" }
        val flows = providers.first().map { provider ->
            flow {
                emit(
                    withTimeout(60.seconds) {
                        when (provider) {
                            is MatchingDanmakuProvider -> TODO()
                            is SimpleDanmakuProvider -> {
                                provider.fetch(request = request)
                            }
                        }
                    },
                )
            }.retry(1) {
                if (it is CancellationException && !currentCoroutineContext().isActive) {
                    // collector was cancelled
                    return@retry false
                }
                logger.error(it) { "Failed to fetch danmaku from provider '${provider.mainServiceId}'" }
                true
            }.catch {
                emit(
                    listOf(
                        DanmakuFetchResult(
                            DanmakuMatchInfo(
                                provider.mainServiceId,
                                0,
                                DanmakuMatchMethod.NoMatch,
                            ),
                            list = emptyList(),
                        ),
                    ),
                )// 忽略错误, 否则一个源炸了会导致所有弹幕都不发射了
                // 必须要 emit 一个, 否则下面 .first 会出错
            }.take(1)
        }.merge().toList().flatten()
        return flows
    }

    suspend fun post(
        episodeId: Int,
        danmaku: DanmakuContent
    ): DanmakuInfo {
        return try {
            sender.first().send(episodeId, danmaku)
        } catch (e: Throwable) {
            throw RepositoryException.wrapOrThrowCancellation(e)
        }
    }
}
