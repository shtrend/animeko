/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.repository.episode

import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import me.him188.ani.app.data.models.episode.EpisodeCollectionInfo
import me.him188.ani.app.data.models.episode.EpisodeInfo
import me.him188.ani.app.data.network.BangumiEpisodeService
import me.him188.ani.app.data.network.toBangumiEpType
import me.him188.ani.app.data.persistent.database.dao.EpisodeCollectionDao
import me.him188.ani.app.data.persistent.database.dao.EpisodeCollectionEntity
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionDao
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionEntity
import me.him188.ani.app.data.repository.Repository
import me.him188.ani.app.data.repository.Repository.Companion.defaultPagingConfig
import me.him188.ani.app.data.repository.RepositoryException
import me.him188.ani.app.domain.episode.EpisodeCollections
import me.him188.ani.datasources.api.EpisodeType.MainStory
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.utils.platform.currentTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class EpisodeCollectionRepository(
    private val subjectDao: SubjectCollectionDao,
    private val episodeCollectionDao: EpisodeCollectionDao,
    private val bangumiEpisodeService: BangumiEpisodeService,
    private val enableAllEpisodeTypes: Flow<Boolean>,
    private val defaultDispatcher: CoroutineContext = Dispatchers.Default,
    private val cacheExpiry: Duration = 1.hours,
) : Repository {
    private val epTypeFilter get() = enableAllEpisodeTypes.map { if (it) null else MainStory }

    /**
     * 获取指定条目的指定剧集信息, 如果没有则从网络获取并缓存
     */
    fun episodeCollectionInfoFlow(subjectId: Int, episodeId: Int): Flow<EpisodeCollectionInfo> {
        return episodeCollectionDao.findByEpisodeId(episodeId).map { entity ->
            entity?.toEpisodeCollectionInfo()
                ?: kotlin.run {
                    bangumiEpisodeService.getEpisodeCollectionById(episodeId)
                        ?.also {
                            episodeCollectionDao.upsert(it.toEntity(subjectId))
                        }
                        ?: throw NoSuchElementException("Episode $episodeId not found")
                }
        }.flowOn(defaultDispatcher)
    }

    /**
     * 获取指定条目的所有剧集信息, 如果没有则从网络获取.
     *
     * 如果 [subjectId] 对应的 [SubjectCollectionEntity] 缓存存在, 则获取到的剧集信息还会插入到缓存 ([EpisodeCollectionEntity]). 否则不会操作缓存 (因为 foreign key).
     */
    fun subjectEpisodeCollectionInfosFlow(
        subjectId: Int,
        allowCached: Boolean = true,
    ): Flow<List<EpisodeCollectionInfo>> = epTypeFilter.flatMapLatest { epType ->
        if (subjectDao.findById(subjectId).first()?.totalEpisodes == 0) {
            return@flatMapLatest flowOf(emptyList())
        }
        episodeCollectionDao.filterBySubjectId(subjectId, epType).transformLatest { episodes ->
            if (allowCached &&
                episodes.isNotEmpty() &&
                (currentTimeMillis() - (episodes.maxOfOrNull { it.lastUpdated } ?: 0)).milliseconds <= cacheExpiry
            ) {
                // 有有效缓存则直接返回
                emit(episodes.map { it.toEpisodeCollectionInfo() })
                return@transformLatest
            }
            try {
                bangumiEpisodeService.getEpisodeCollectionInfosBySubjectId(subjectId, epType)
                    .toList() // 目前先直接全拿了, 反正一般情况下剧集数量很少
                    .also { list ->
                        if (subjectDao.findById(subjectId).first() != null) {
                            // 插入后会立即触发 filterBySubjectId 更新 (emit 新的)
                            episodeCollectionDao.upsert(list.map { it.toEntity(subjectId) })
                        }
                    }
            } catch (e: Exception) {
                // 如果网络请求失败, 则返回缓存
                emit(episodes.map { it.toEpisodeCollectionInfo() })
                throw RepositoryException.wrapOrThrowCancellation(e)
            }
        }
    }.flowOn(defaultDispatcher)

    fun subjectEpisodeCollectionsPager(
        subjectId: Int,
        pagingConfig: PagingConfig = defaultPagingConfig,
    ): Flow<PagingData<EpisodeCollectionInfo>> = Pager(
        config = pagingConfig,
        remoteMediator = EpisodeCollectionsRemoteMediator(
            episodeCollectionDao, bangumiEpisodeService,
            subjectId,
        ),
        pagingSourceFactory = {
            episodeCollectionDao.filterBySubjectIdPaging(subjectId)
        },
    ).flow.map { data ->
        data.map {
            it.toEpisodeCollectionInfo()
        }
    }.flowOn(defaultDispatcher)

    /**
     * 设置指定条目的所有剧集为已看.
     */
    suspend fun setAllEpisodesWatched(subjectId: Int) = withContext(defaultDispatcher) {
        val episodeIds = subjectEpisodeCollectionInfosFlow(subjectId)
            .first()
            .map { it.episodeId }

        bangumiEpisodeService.setEpisodeCollection(subjectId, episodeIds, UnifiedCollectionType.DONE)
        episodeCollectionDao.setAllEpisodesWatched(subjectId)
    }

    suspend fun setEpisodeCollectionType(
        subjectId: Int,
        episodeId: Int,
        collectionType: UnifiedCollectionType
    ) = withContext(defaultDispatcher) {
        bangumiEpisodeService.setEpisodeCollection(subjectId, listOf(episodeId), collectionType)
        episodeCollectionDao.updateSelfCollectionType(subjectId, episodeId, collectionType)
    }

    /**
     * Loads [EpisodeCollectionEntity]
     */
    private inner class EpisodeCollectionsRemoteMediator<T : Any>(
        private val episodeCollectionDao: EpisodeCollectionDao,
        private val episodeService: BangumiEpisodeService,
        val subjectId: Int,
    ) : RemoteMediator<Int, T>() {
        override suspend fun initialize(): InitializeAction {
            return withContext(defaultDispatcher) {
                if ((currentTimeMillis() - episodeCollectionDao.lastUpdated()).milliseconds > cacheExpiry) {
                    InitializeAction.LAUNCH_INITIAL_REFRESH
                } else {
                    InitializeAction.SKIP_INITIAL_REFRESH
                }
            }
        }

        override suspend fun load(
            loadType: LoadType,
            state: PagingState<Int, T>,
        ): MediatorResult = withContext(defaultDispatcher) {
            val offset = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return@withContext MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> state.pages.size * state.config.pageSize
            }

            try {
                val episodeType = epTypeFilter.first()
                val episodes = episodeService.getEpisodeCollectionInfosPaged(
                    subjectId,
                    episodeType = episodeType?.toBangumiEpType(),
                    offset = offset,
                    limit = state.config.pageSize,
                )
                episodes.page.takeIf { it.isNotEmpty() }?.let { list ->
                    episodeCollectionDao.upsert(
                        list.map { it.toEntity(subjectId) },
                    )
                }

                MediatorResult.Success(endOfPaginationReached = episodes.page.isEmpty())
            } catch (e: Exception) {
                return@withContext MediatorResult.Error(RepositoryException.wrapOrThrowCancellation(e))
            }

        }
    }
}

/**
 * 获取指定条目是否已经完结. 不是用户是否看完, 只要条目本身完结了就算.
 */
fun EpisodeCollectionRepository.subjectCompletedFlow(subjectId: Int): Flow<Boolean> {
    return subjectEpisodeCollectionInfosFlow(subjectId).map { epCollection ->
        EpisodeCollections.isSubjectCompleted(epCollection.map { it.episodeInfo })
    }
}

suspend inline fun EpisodeCollectionRepository.setEpisodeWatched(subjectId: Int, episodeId: Int, watched: Boolean) =
    setEpisodeCollectionType(
        subjectId,
        episodeId,
        if (watched) UnifiedCollectionType.DONE else UnifiedCollectionType.WISH,
    )

fun EpisodeCollectionInfo.toEntity(subjectId: Int): EpisodeCollectionEntity {
    return EpisodeCollectionEntity(
        subjectId = subjectId,
        episodeId = episodeId,
        episodeType = episodeInfo.type,
        name = episodeInfo.name,
        nameCn = episodeInfo.nameCn,
        airDate = episodeInfo.airDate,
        comment = episodeInfo.comment,
        desc = episodeInfo.desc,
        sort = episodeInfo.sort,
        ep = episodeInfo.ep,
        selfCollectionType = collectionType,
    )
}

fun EpisodeCollectionEntity.toEpisodeCollectionInfo() =
    EpisodeCollectionInfo(
        episodeInfo = toEpisodeInfo(),
        collectionType = selfCollectionType,
    )

private fun EpisodeCollectionEntity.toEpisodeInfo(): EpisodeInfo {
    return EpisodeInfo(
        episodeId = this.episodeId,
        type = this.episodeType ?: MainStory,
        name = this.name,
        nameCn = this.nameCn,
        airDate = this.airDate,
        comment = this.comment,
        desc = this.desc,
        sort = this.sort,
        ep = this.ep,
    )
}
