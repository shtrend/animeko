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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.withContext
import me.him188.ani.app.data.models.episode.EpisodeCollectionInfo
import me.him188.ani.app.data.models.episode.isKnownCompleted
import me.him188.ani.app.data.models.subject.CharacterInfo
import me.him188.ani.app.data.models.subject.PersonInfo
import me.him188.ani.app.data.models.subject.RelatedCharacterInfo
import me.him188.ani.app.data.models.subject.RelatedPersonInfo
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
import me.him188.ani.app.data.persistent.database.entity.CharacterEntity
import me.him188.ani.app.data.persistent.database.entity.PersonEntity
import me.him188.ani.app.data.persistent.database.entity.SubjectCharacterRelationEntity
import me.him188.ani.app.data.persistent.database.entity.SubjectPersonRelationEntity
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
    private val ioDispatcher: CoroutineContext = Dispatchers.IO_,
) : SubjectCollectionRepository {
    override fun subjectCollectionCountsFlow(): Flow<SubjectCollectionCounts?> {
        return (bangumiSubjectService.subjectCollectionCountsFlow() as Flow<SubjectCollectionCounts?>)
            .retry(2)
            .catch {
                logger.error("Failed to get subject collection counts", it)
                emit(null)
            }
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
                val (batch, collection) = bangumiSubjectService.getSubjectCollection(subjectId)
                batch
                    .toEntity(
                        collection?.type.toCollectionType(),
                        selfRatingInfo = collection?.toSelfRatingInfo() ?: SelfRatingInfo.Empty,
                    )
                    .also {
                        subjectCollectionDao.upsert(it)
                    }
                    .toSubjectCollectionInfo(
                        episodes = getSubjectEpisodeCollections(subjectId),
                        currentDate = getCurrentDate(),
                    )
            }
        }

    private suspend fun getSubjectEpisodeCollections(subjectId: Int) =
        episodeCollectionRepository.subjectEpisodeCollectionInfosFlow(subjectId).first()

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
        }

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
                episodes = getSubjectEpisodeCollections(entity.subjectId),
                currentDate = getCurrentDate(),
            )
        }
    }

    override suspend fun updateRating(
        subjectId: Int,
        score: Int?, // 0 to remove rating
        comment: String?, // set empty to remove
        tags: List<String>?,
        isPrivate: Boolean?,
    ) {
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

    private inner class SubjectCollectionRemoteMediator<T : Any>(
        private val query: CollectionsFilterQuery,
    ) : RemoteMediator<Int, T>() {
        override suspend fun initialize(): InitializeAction = withContext(ioDispatcher) {
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
                withContext(ioDispatcher) {
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

                    for (batch in items) {
                        val subjectId = batch.batchSubjectDetails.subjectInfo.subjectId
                        subjectRelationsDao.upsertPersons(batch.batchSubjectDetails.relatedPersonInfoList.map { it.personInfo.toEntity() })
                        subjectRelationsDao.upsertCharacters(batch.batchSubjectDetails.relatedCharacterInfoList.map { it.character.toEntity() })
                        subjectCollectionDao.upsert(
                            batch.batchSubjectDetails.toEntity(
                                batch.collection?.type.toCollectionType(),
                                batch.collection.toSelfRatingInfo(),
                            ),
                        )
                        // 必须先插入前三个, 再插入 relations, 否则会 violate foreign key constraint

                        subjectRelationsDao.upsertSubjectPersonRelations(
                            batch.batchSubjectDetails.relatedPersonInfoList.map { it.toRelationEntity(subjectId) },
                        )
                        subjectRelationsDao.upsertSubjectCharacterRelations(
                            batch.batchSubjectDetails.relatedCharacterInfoList.map { it.toRelationEntity(subjectId) },
                        )
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
        return if (type == null) {
            deleteSubjectCollection(subjectId)
        } else {
            patchSubjectCollection(
                subjectId,
                BangumiUserSubjectCollectionModifyPayload(type.toSubjectCollectionType()),
            )
        }
    }

    private suspend fun patchSubjectCollection(
        subjectId: Int,
        payload: BangumiUserSubjectCollectionModifyPayload,
    ) {
        api.first().postUserCollection(subjectId, payload)
        subjectCollectionDao.updateType(subjectId, payload.type.toCollectionType())
    }

    private suspend fun deleteSubjectCollection(subjectId: Int) {
        // TODO: deleteSubjectCollection
    }

    private companion object {
        val logger = logger<SubjectCollectionRepository>()
    }
}

private fun CharacterInfo.toEntity(): CharacterEntity {
    return CharacterEntity(
        characterId = id,
        name = name,
        nameCn = nameCn,
        imageLarge = imageLarge,
        imageMedium = imageMedium,
    )
}

private fun PersonInfo.toEntity(): PersonEntity {
    return PersonEntity(
        personId = id,
        name = name,
        nameCn = nameCn,
        type = type,
        imageLarge = imageLarge,
        imageMedium = imageMedium,
        summary = summary,
    )
}

private fun RelatedPersonInfo.toRelationEntity(subjectId: Int): SubjectPersonRelationEntity {
    return SubjectPersonRelationEntity(
        subjectId = subjectId,
        index = index,
        personId = personInfo.id,
        position = position,
    )
}

private fun RelatedCharacterInfo.toRelationEntity(subjectId: Int): SubjectCharacterRelationEntity {
    return SubjectCharacterRelationEntity(
        subjectId = subjectId,
        index = index,
        characterId = character.id,
        role = role,
    )
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
        airingInfo = SubjectAiringInfo.computeFromEpisodeList(
            episodes.map { it.episodeInfo },
            airDate,
        ),
        episodes = episodes,
        progressInfo = SubjectProgressInfo.compute(
            subjectStarted = currentDate > subjectInfo.airDate,
            episodes = episodes.map {
                SubjectProgressInfo.Episode(
                    it.episodeId,
                    it.collectionType,
                    it.episodeInfo.sort,
                    it.episodeInfo.airDate,
                    it.episodeInfo.isKnownCompleted,
                )
            },
            subjectAirDate = subjectInfo.airDate,
        ),
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
