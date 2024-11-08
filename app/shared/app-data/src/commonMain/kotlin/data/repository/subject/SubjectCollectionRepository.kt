/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.repository.subject

import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.map
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.him188.ani.app.data.models.episode.EpisodeCollectionInfo
import me.him188.ani.app.data.models.subject.SelfRatingInfo
import me.him188.ani.app.data.models.subject.SubjectAiringInfo
import me.him188.ani.app.data.models.subject.SubjectCollectionCounts
import me.him188.ani.app.data.models.subject.SubjectCollectionInfo
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.models.subject.SubjectProgressInfo
import me.him188.ani.app.data.network.BangumiSubjectService
import me.him188.ani.app.data.network.BatchSubjectDetails
import me.him188.ani.app.data.network.toSelfRatingInfo
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionDao
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionEntity
import me.him188.ani.app.data.persistent.database.dao.SubjectRelationsDao
import me.him188.ani.app.data.persistent.database.dao.filterMostRecentUpdated
import me.him188.ani.app.data.repository.Repository
import me.him188.ani.app.data.repository.Repository.Companion.defaultPagingConfig
import me.him188.ani.app.data.repository.RepositoryException
import me.him188.ani.app.data.repository.episode.EpisodeCollectionRepository
import me.him188.ani.app.domain.search.SubjectType
import me.him188.ani.datasources.api.PackedDate
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.datasources.bangumi.apis.DefaultApi
import me.him188.ani.datasources.bangumi.models.BangumiUserSubjectCollectionModifyPayload
import me.him188.ani.datasources.bangumi.processing.toCollectionType
import me.him188.ani.datasources.bangumi.processing.toSubjectCollectionType
import me.him188.ani.utils.coroutines.IO_
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.platform.currentTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

typealias BangumiSubjectApi = DefaultApi

/**
 * 条目信息和条目收藏的仓库.
 *
 * [SubjectInfo], [SubjectCollectionInfo], [SubjectCollectionCounts]
 */
sealed interface SubjectCollectionRepository : Repository {
    /**
     * 获取条目收藏统计信息 cold [Flow]. Flow 将会 emit 至少一个值, 失败时 emit `null`.
     */
    fun subjectCollectionCountsFlow(): Flow<SubjectCollectionCounts?>

    fun subjectCollectionFlow(subjectId: Int): Flow<SubjectCollectionInfo>

    fun subjectCollectionsPager(
        query: CollectionsFilterQuery = CollectionsFilterQuery.Empty,
        pagingConfig: PagingConfig = defaultPagingConfig,
    ): Flow<PagingData<SubjectCollectionInfo>>

    /**
     * 获取最近更新的条目收藏 cold [Flow].
     */
    fun mostRecentlyUpdatedSubjectCollectionsFlow(
        limit: Int,
        types: List<UnifiedCollectionType>? = null, // null for all
    ): Flow<List<SubjectCollectionInfo>>

    suspend fun updateRating(
        subjectId: Int,
        score: Int? = null, // 0 to remove rating
        comment: String? = null, // set empty to remove
        tags: List<String>? = null,
        isPrivate: Boolean? = null,
    )

    suspend fun setSubjectCollectionTypeOrDelete(
        subjectId: Int,
        type: UnifiedCollectionType?,
    )
}

class SubjectCollectionRepositoryImpl(
    private val api: Flow<BangumiSubjectApi>,
    private val bangumiSubjectService: BangumiSubjectService,
    private val subjectCollectionDao: SubjectCollectionDao,
    private val subjectRelationsDao: SubjectRelationsDao,
    private val episodeCollectionRepository: EpisodeCollectionRepository,
    private val getCurrentDate: () -> PackedDate = { PackedDate.now() },
    private val defaultDispatcher: CoroutineContext = Dispatchers.IO_,
) : SubjectCollectionRepository {
    override fun subjectCollectionCountsFlow(): Flow<SubjectCollectionCounts?> {
        return (bangumiSubjectService.subjectCollectionCountsFlow() as Flow<SubjectCollectionCounts?>)
            .retry(2)
            .catch {
                logger.error("Failed to get subject collection counts", it)
                emit(null)
            }
            .flowOn(defaultDispatcher)
//        return combine(
//            subjectCollectionDao.countCollected(UnifiedCollectionType.WISH),
//            subjectCollectionDao.countCollected(UnifiedCollectionType.DOING),
//            subjectCollectionDao.countCollected(UnifiedCollectionType.DONE),
//            subjectCollectionDao.countCollected(UnifiedCollectionType.ON_HOLD),
//            subjectCollectionDao.countCollected(UnifiedCollectionType.DROPPED),
//        ) { wish, doing, done, onHold, dropped ->
//            SubjectCollectionCounts(
//                wish = wish,
//                doing = doing,
//                done = done,
//                onHold = onHold,
//                dropped = dropped,
//                total = wish + doing + done + onHold + dropped,
//            )
//        }
    }

    override fun subjectCollectionFlow(subjectId: Int): Flow<SubjectCollectionInfo> =
        subjectCollectionDao.findById(subjectId).map { entity ->
            if (entity != null) {
                return@map entity.toSubjectCollectionInfo(
                    episodes = getSubjectEpisodeCollections(subjectId),
                    currentDate = getCurrentDate(),
                )
            } else {
                // cache miss
                fetchAndSaveSubject(subjectId)
            }
        }.flowOn(defaultDispatcher)

    private suspend fun fetchAndSaveSubject(subjectId: Int): SubjectCollectionInfo {
        val (batch, collection) = bangumiSubjectService.getSubjectCollection(subjectId)
        return batch
            .toEntity(
                collection?.type.toCollectionType(),
                selfRatingInfo = collection?.toSelfRatingInfo() ?: SelfRatingInfo.Empty,
            )
            .also { entity ->
                subjectCollectionDao.upsert(entity)
            }
            .toSubjectCollectionInfo(
                episodes = getSubjectEpisodeCollections(subjectId),
                currentDate = getCurrentDate(),
            )
    }

    private suspend fun getSubjectEpisodeCollections(
        subjectId: Int,
        allowCached: Boolean = true,
    ) = episodeCollectionRepository.subjectEpisodeCollectionInfosFlow(subjectId, allowCached).flowOn(defaultDispatcher)
        .first()

    override fun mostRecentlyUpdatedSubjectCollectionsFlow(
        limit: Int,
        types: List<UnifiedCollectionType>?, // null for all
    ): Flow<List<SubjectCollectionInfo>> =
        subjectCollectionDao.filterMostRecentUpdated(types, limit).map { list ->
            list.map { entity ->
                entity.toSubjectCollectionInfo(
                    episodes = getSubjectEpisodeCollections(entity.subjectId),
                    currentDate = getCurrentDate(),
                )
            }
        }.flowOn(defaultDispatcher)

    override fun subjectCollectionsPager(
        query: CollectionsFilterQuery,
        pagingConfig: PagingConfig,
    ): Flow<PagingData<SubjectCollectionInfo>> = Pager(
        config = pagingConfig,
        initialKey = 0,
        remoteMediator = SubjectCollectionRemoteMediator(query),
        pagingSourceFactory = {
            subjectCollectionDao.filterByCollectionTypePaging(query.type)
        },
    ).flow.map { data ->
        data.map { entity ->
            entity.toSubjectCollectionInfo(
                episodes = getSubjectEpisodeCollections(entity.subjectId), // 通常会读取缓存, 因为 [SubjectCollectionRemoteMediator] 会提前查询这个
                currentDate = getCurrentDate(),
            )
        }
    }.flowOn(defaultDispatcher)

    override suspend fun updateRating(
        subjectId: Int,
        score: Int?, // 0 to remove rating
        comment: String?, // set empty to remove
        tags: List<String>?,
        isPrivate: Boolean?,
    ) {
        withContext(defaultDispatcher) {
            bangumiSubjectService.patchSubjectCollection(
                subjectId,
                BangumiUserSubjectCollectionModifyPayload(
                    rate = score,
                    comment = comment,
                    tags = tags,
                    private = isPrivate,
                ),
            )

            subjectCollectionDao.updateRating(
                subjectId,
                score,
                comment,
                tags,
                isPrivate,
            )
        }
    }

    private inner class SubjectCollectionRemoteMediator<T : Any>(
        private val query: CollectionsFilterQuery,
    ) : RemoteMediator<Int, T>() {
        override suspend fun initialize(): InitializeAction = withContext(defaultDispatcher) {
            if ((currentTimeMillis() - subjectCollectionDao.lastUpdated()).milliseconds > 1.hours) {
                InitializeAction.LAUNCH_INITIAL_REFRESH
            } else {
                InitializeAction.SKIP_INITIAL_REFRESH
            }
        }

        override suspend fun load(
            loadType: LoadType,
            state: PagingState<Int, T>,
        ): MediatorResult {
            return try {
                withContext(defaultDispatcher) {
                    val offset = when (loadType) {
                        LoadType.REFRESH -> {
                            0
                        }

                        LoadType.PREPEND -> return@withContext MediatorResult.Success(
                            endOfPaginationReached = true,
                        )

                        LoadType.APPEND -> {
                            val lastLoadedPage = state.pages.lastOrNull()
                            if (lastLoadedPage != null) {
                                lastLoadedPage.itemsBefore + lastLoadedPage.data.size
                            } else {
                                0
                            }
                        }
                    }

                    val items = bangumiSubjectService.getSubjectCollections(
                        type = query.type?.toSubjectCollectionType(),
                        offset = offset,
                        limit = state.config.pageSize,
                    )

                    coroutineScope {
                        // 提前'批量'查询剧集收藏状态, 防止在收藏页显示结果时一个一个查导致太慢
                        val concurrency = Semaphore(4)
                        items.forEach { subjectCollection ->
                            launch {
                                try {
                                    subjectCollection.collection?.subjectId?.let { subjectId ->
                                        concurrency.withPermit {
                                            getSubjectEpisodeCollections(
                                                subjectId,
                                                allowCached = false, // SubjectCollectionRemoteMediator 是强制刷新
                                            ) // side-effect: update database
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    // 这里我们只是批量提前查询, 不一定需要成功. 后面等收藏页真正需要时再来重试和处理对应异常
                                    logger.error("Failed to fetch episode collections", e)
                                }
                            }
                        }

                        for (collection in items) {
                            val subject = collection.batchSubjectDetails
                            subjectCollectionDao.upsert(
                                subject.toEntity(
                                    collection.collection?.type.toCollectionType(),
                                    collection.collection.toSelfRatingInfo(),
                                ),
                            )
                        }
                    }

                    MediatorResult.Success(endOfPaginationReached = items.isEmpty())
                }
            } catch (e: RepositoryException) {
                MediatorResult.Error(e)
            } catch (e: ResponseException) {
                MediatorResult.Error(e)
            } catch (e: Exception) {
                MediatorResult.Error(e)
            }
        }
    }

    override suspend fun setSubjectCollectionTypeOrDelete(
        subjectId: Int,
        type: UnifiedCollectionType?,
    ) {
        return withContext(defaultDispatcher) {
            if (type == null) {
                deleteSubjectCollection(subjectId)
            } else {
                patchSubjectCollection(
                    subjectId,
                    BangumiUserSubjectCollectionModifyPayload(type.toSubjectCollectionType()),
                )
            }
        }
    }

    private suspend fun patchSubjectCollection(
        subjectId: Int,
        payload: BangumiUserSubjectCollectionModifyPayload,
    ) {
        withContext(defaultDispatcher) {
            api.first().postUserCollection(subjectId, payload)
            subjectCollectionDao.updateType(subjectId, payload.type.toCollectionType())
        }
    }

    private suspend fun deleteSubjectCollection(subjectId: Int) {
        // TODO: deleteSubjectCollection
    }

    private companion object {
        val logger = logger<SubjectCollectionRepository>()
    }
}

data class CollectionsFilterQuery(
    val type: UnifiedCollectionType?,
) {
    companion object {
        val Empty = CollectionsFilterQuery(null)
    }
}

private fun SubjectCollectionEntity.toSubjectInfo(): SubjectInfo {
    return SubjectInfo(
        subjectId = subjectId,
        subjectType = SubjectType.ANIME,
        name = name,
        nameCn = nameCn,
        summary = summary,
        nsfw = nsfw,
        imageLarge = imageLarge,
        totalEpisodes = totalEpisodes,
        airDate = airDate,
        tags = tags,
        aliases = aliases,
        ratingInfo = ratingInfo,
        collectionStats = collectionStats,
        completeDate = completeDate,
    )
}

private fun SubjectCollectionEntity.toSubjectCollectionInfo(
    episodes: List<EpisodeCollectionInfo>,
    currentDate: PackedDate,
): SubjectCollectionInfo {
    val subjectInfo = toSubjectInfo()
    return SubjectCollectionInfo(
        collectionType = collectionType,
        subjectInfo = subjectInfo,
        selfRatingInfo = selfRatingInfo,
        episodes = episodes,
        airingInfo = SubjectAiringInfo.computeFromEpisodeList(
            episodes.map { it.episodeInfo },
            airDate,
        ),
        progressInfo = SubjectProgressInfo.compute(subjectInfo, episodes, currentDate),
        cachedStaffUpdated = cachedStaffUpdated,
        cachedCharactersUpdated = cachedCharactersUpdated,
    )
}


internal fun BatchSubjectDetails.toEntity(
    collectionType: UnifiedCollectionType,
    selfRatingInfo: SelfRatingInfo,
): SubjectCollectionEntity =
    subjectInfo.run {
        SubjectCollectionEntity(
            subjectId = subjectId,
//            subjectType = SubjectType.ANIME,
            name = name,
            nameCn = nameCn,
            summary = summary,
            nsfw = nsfw,
            imageLarge = imageLarge,
            totalEpisodes = totalEpisodes,
            airDate = airDate,
            tags = tags,
            aliases = aliases,
            ratingInfo = ratingInfo,
            collectionStats = collectionStats,
            completeDate = completeDate,
            selfRatingInfo = selfRatingInfo,
            collectionType = collectionType,
        )
    }
