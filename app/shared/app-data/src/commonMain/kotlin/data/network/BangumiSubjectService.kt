/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import me.him188.ani.app.data.models.subject.RatingCounts
import me.him188.ani.app.data.models.subject.RatingInfo
import me.him188.ani.app.data.models.subject.RelatedCharacterInfo
import me.him188.ani.app.data.models.subject.RelatedPersonInfo
import me.him188.ani.app.data.models.subject.SelfRatingInfo
import me.him188.ani.app.data.models.subject.SubjectCollectionCounts
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.repository.RepositoryUsernameProvider
import me.him188.ani.app.data.repository.getOrThrow
import me.him188.ani.app.domain.search.SubjectType
import me.him188.ani.app.domain.session.OpaqueSession
import me.him188.ani.app.domain.session.SessionManager
import me.him188.ani.app.domain.session.username
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.datasources.bangumi.BangumiClient
import me.him188.ani.datasources.bangumi.apis.DefaultApi
import me.him188.ani.datasources.bangumi.models.BangumiCount
import me.him188.ani.datasources.bangumi.models.BangumiRating
import me.him188.ani.datasources.bangumi.models.BangumiSubject
import me.him188.ani.datasources.bangumi.models.BangumiSubjectCollectionType
import me.him188.ani.datasources.bangumi.models.BangumiSubjectType
import me.him188.ani.datasources.bangumi.models.BangumiUserSubjectCollection
import me.him188.ani.datasources.bangumi.models.BangumiUserSubjectCollectionModifyPayload
import me.him188.ani.datasources.bangumi.processing.toCollectionType
import me.him188.ani.datasources.bangumi.processing.toSubjectCollectionType
import me.him188.ani.utils.coroutines.IO_
import me.him188.ani.utils.logging.error
import me.him188.ani.utils.logging.logger
import me.him188.ani.utils.serialization.getOrFail
import org.koin.core.component.KoinComponent
import kotlin.coroutines.CoroutineContext

/**
 * Performs network requests.
 * Use [SubjectManager] instead.
 */
interface BangumiSubjectService {
    suspend fun getSubject(id: Int): BangumiSubject

    suspend fun getSubjectCollections(
        type: BangumiSubjectCollectionType?,
        offset: Int,
        limit: Int
    ): List<BatchSubjectCollection>

    suspend fun getSubjectCollection(subjectId: Int): BatchSubjectCollection

    suspend fun batchGetSubjectDetails(ids: List<Int>): List<BatchSubjectDetails>

    /**
     * 获取用户对这个条目的收藏状态. flow 一定会 emit 至少一个值或抛出异常. 当用户没有收藏这个条目时 emit `null`.
     */
    fun subjectCollectionById(subjectId: Int): Flow<BangumiUserSubjectCollection?>

    fun subjectCollectionTypeById(subjectId: Int): Flow<UnifiedCollectionType>

    suspend fun patchSubjectCollection(subjectId: Int, payload: BangumiUserSubjectCollectionModifyPayload)
    suspend fun deleteSubjectCollection(subjectId: Int)

    /**
     * 获取各个收藏分类的数量.
     */
    fun subjectCollectionCountsFlow(): Flow<SubjectCollectionCounts>
}

data class BatchSubjectCollection(
    val batchSubjectDetails: BatchSubjectDetails,
    /**
     * `null` 表示未收藏
     */
    val collection: BangumiUserSubjectCollection?,
)

suspend inline fun BangumiSubjectService.setSubjectCollectionTypeOrDelete(
    subjectId: Int,
    type: BangumiSubjectCollectionType?
) {
    return if (type == null) {
        deleteSubjectCollection(subjectId)
    } else {
        patchSubjectCollection(subjectId, BangumiUserSubjectCollectionModifyPayload(type))
    }
}

class RemoteBangumiSubjectService(
    private val client: BangumiClient,
    private val api: Flow<DefaultApi>,
    private val sessionManager: SessionManager,
    private val usernameProvider: RepositoryUsernameProvider,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO_,
) : BangumiSubjectService, KoinComponent {
    private val logger = logger(this::class)

    override suspend fun getSubject(id: Int): BangumiSubject = withContext(ioDispatcher) {
        client.getApi().getSubjectById(id).body()
    }

    override suspend fun getSubjectCollections(
        type: BangumiSubjectCollectionType?,
        offset: Int,
        limit: Int
    ): List<BatchSubjectCollection> = withContext(ioDispatcher) {
        val username = usernameProvider.getOrThrow()
        val resp = api.first().getUserCollectionsByUsername(
            username,
            subjectType = BangumiSubjectType.Anime,
            type = type,
            limit = limit,
            offset = offset,
        ).body()

        val collections = resp.data.orEmpty()
        val list = batchGetSubjectDetails(collections.map { it.subjectId })

        list.map {
            val subjectId = it.subjectInfo.subjectId
            val dto = collections.firstOrNull { it.subjectId == subjectId }
                ?: error("Subject $subjectId not found in collections")
            BatchSubjectCollection(
                batchSubjectDetails = it,
                collection = dto,
            )
        }
    }

    override suspend fun getSubjectCollection(subjectId: Int): BatchSubjectCollection {
        val collection = subjectCollectionById(subjectId).first()
        return BatchSubjectCollection(
            batchGetSubjectDetails(listOf(subjectId)).first(),
            collection,
        )
    }

    override suspend fun batchGetSubjectDetails(ids: List<Int>): List<BatchSubjectDetails> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return withContext(ioDispatcher) {
            val resp = client.executeGraphQL(
                "SubjectCollectionRepositoryImpl.batchGetSubjectDetails",
                """
                fragment Ep on Episode {
                  id
                  type
                  name
                  name_cn
                  airdate
                  comment
                  description
                  sort
                }
                
                fragment SubjectFragment on Subject {
                  id
                  type
                  name
                  name_cn
                  images{large, common}
                  characters {
                    order
                    type
                    character {
                      id
                      name
                      comment
                      collects
                      infobox {
                        key 
                        values {k 
                                v}
                      }
                      role
                      images {
                        large
                        medium
                      }
                    }
                  }
                  infobox {
                    values {
                      k
                      v
                    }
                    key
                  }
                  summary
                  eps
                  collection{collect , doing, dropped, on_hold, wish}
                  airtime{date}
                  rating{count, rank, score, total}
                  nsfw
                  tags{count, name}
                  
                  persons {
                    person {
                      career
                      collects
                      comment
                      id
                      images {
                        large
                        medium
                      }
                      infobox {
                        key
                        values {
                          k
                          v
                        } 
                      }
                      last_post
                      lock
                      name
                      nsfw
                      redirect
                      summary
                      type
                    }
                    position
                  }
                
                  leadingEpisodes : episodes(limit: 100) { ...Ep }
                  trailingEpisodes : episodes(limit: 1, offset: -1) { ...Ep }
                  # episodes{id, type, name, name_cn, sort, airdate, comment, duration, description, disc, ep, }
                }

            query BatchGetSubjectQuery {
              ${
                    ids.joinToString(separator = "\n") { id ->
                        """
                        s$id:subject(id: $id){...SubjectFragment}
                """
                    }
                }
            }
        """.trimIndent(),
            )
            resp["errors"]?.let {
                logger.error("batchGetSubjectDetails failed for query $ids: $it")
            }
            val list = when (val element = resp.getOrFail("data")) {
                is JsonObject -> {
                    element.values.mapIndexed { index, it ->
                        if (it is JsonNull) { // error
                            val id = ids[index]
                            BatchSubjectDetails(
                                SubjectInfo.Empty.copy(
                                    subjectId = id, subjectType = SubjectType.ANIME,
                                    nameCn = "<$id 错误>",
                                    name = "<$id 错误>",
                                    summary = resp["errors"].toString(),
                                ),
                                relatedCharacterInfoList = emptyList(),
                                relatedPersonInfoList = emptyList(),
                            )
                        } else {
                            BangumiSubjectGraphQLParser.parseBatchSubjectDetails(it.jsonObject)
                        }
                    }
                }

                is JsonNull -> throw IllegalStateException("batchGetSubjectDetails response data is null for ids $ids: $resp")
                else -> throw IllegalStateException("Unexpected response: $element")
            }

            list
        }
    }

    override suspend fun patchSubjectCollection(subjectId: Int, payload: BangumiUserSubjectCollectionModifyPayload) {
        withContext(ioDispatcher) {
            client.getApi().postUserCollection(subjectId, payload)
        }
    }

    override suspend fun deleteSubjectCollection(subjectId: Int) {
        // TODO:  deleteSubjectCollection
    }

    @OptIn(OpaqueSession::class)
    override fun subjectCollectionCountsFlow(): Flow<SubjectCollectionCounts> {
        return sessionManager.username.filterNotNull().map { username ->
            val types = UnifiedCollectionType.entries - UnifiedCollectionType.NOT_COLLECTED
            val totals = IntArray(types.size) { type ->
                client.getApi().getUserCollectionsByUsername(
                    username,
                    subjectType = BangumiSubjectType.Anime,
                    type = types[type].toSubjectCollectionType(),
                    limit = 1, // we only need the total count. API requires at least 1
                ).body().total ?: 0
            }
            SubjectCollectionCounts(
                wish = totals[UnifiedCollectionType.WISH.ordinal],
                doing = totals[UnifiedCollectionType.DOING.ordinal],
                done = totals[UnifiedCollectionType.DONE.ordinal],
                onHold = totals[UnifiedCollectionType.ON_HOLD.ordinal],
                dropped = totals[UnifiedCollectionType.DROPPED.ordinal],
                total = totals.sum(),
            )
        }.flowOn(ioDispatcher)
    }

    override fun subjectCollectionById(subjectId: Int): Flow<BangumiUserSubjectCollection?> {
        return flow {
            emit(
                try {
                    @OptIn(OpaqueSession::class)
                    client.getApi().getUserCollection(sessionManager.username.first() ?: "-", subjectId).body()
                } catch (e: ResponseException) {
                    if (e.response.status == HttpStatusCode.NotFound) {
                        null
                    } else {
                        throw e
                    }
                },
            )
        }.flowOn(ioDispatcher)
    }

    override fun subjectCollectionTypeById(subjectId: Int): Flow<UnifiedCollectionType> {
        return flow {
            emit(
                try {
                    @OptIn(OpaqueSession::class)
                    val username = sessionManager.username.first() ?: "-"
                    client.getApi().getUserCollection(username, subjectId).body().type.toCollectionType()
                } catch (e: ResponseException) {
                    if (e.response.status == HttpStatusCode.NotFound) {
                        UnifiedCollectionType.NOT_COLLECTED
                    } else {
                        throw e
                    }
                },
            )
        }.flowOn(ioDispatcher)
    }
}


private fun BangumiRating.toRatingInfo(): RatingInfo = RatingInfo(
    rank = rank,
    total = total,
    count = count.toRatingCounts(),
    score = score.toString(),
)

private fun BangumiCount.toRatingCounts() = RatingCounts(
    _1 ?: 0,
    _2 ?: 0,
    _3 ?: 0,
    _4 ?: 0,
    _5 ?: 0,
    _6 ?: 0,
    _7 ?: 0,
    _8 ?: 0,
    _9 ?: 0,
    _10 ?: 0,
)


class BatchSubjectDetails(
    val subjectInfo: SubjectInfo,
    val relatedCharacterInfoList: List<RelatedCharacterInfo>,
    val relatedPersonInfoList: List<RelatedPersonInfo>,
)

internal fun BangumiUserSubjectCollection?.toSelfRatingInfo(): SelfRatingInfo {
    if (this == null) {
        return SelfRatingInfo.Empty
    }
    return SelfRatingInfo(
        score = rate,
        comment = comment.takeUnless { it.isNullOrBlank() },
        tags = tags,
        isPrivate = private,
    )
}
