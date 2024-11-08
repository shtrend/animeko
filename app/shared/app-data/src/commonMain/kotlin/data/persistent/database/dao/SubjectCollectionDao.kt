/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.persistent.database.dao

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.him188.ani.app.data.models.subject.RatingInfo
import me.him188.ani.app.data.models.subject.SelfRatingInfo
import me.him188.ani.app.data.models.subject.SubjectCollectionStats
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.data.models.subject.Tag
import me.him188.ani.app.data.persistent.database.converters.PackedDateConverter
import me.him188.ani.datasources.api.PackedDate
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.utils.platform.currentTimeMillis

/**
 * @see SubjectInfo
 */
@Entity(
    tableName = "subject_collection",
    indices = [
        Index(value = ["lastUpdated"], unique = false, orders = [Index.Order.DESC]),
    ],
)
@TypeConverters(PackedDateConverter::class)
data class SubjectCollectionEntity(
    @PrimaryKey val subjectId: Int,

    // SubjectInfo
    val name: String,
    val nameCn: String,
    val summary: String,
    val nsfw: Boolean,
    val imageLarge: String,
    val totalEpisodes: Int,
    val airDate: PackedDate,
    val aliases: List<String>,
    val tags: List<Tag>,
    @Embedded(prefix = "collection_stats_")
    val collectionStats: SubjectCollectionStats,
    @Embedded(prefix = "rating_")
    val ratingInfo: RatingInfo,
    val completeDate: PackedDate,
    // SubjectCollectionInfo

    @Embedded(prefix = "self_rating_")
    val selfRatingInfo: SelfRatingInfo,
    val collectionType: UnifiedCollectionType,

    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val lastUpdated: Long = currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val cachedStaffUpdated: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val cachedCharactersUpdated: Long = 0,
)

@Dao
interface SubjectCollectionDao {
    @Upsert
    suspend fun upsert(item: SubjectCollectionEntity)

    @Upsert
    @Transaction
    suspend fun upsert(item: List<SubjectCollectionEntity>)

    @Query("""UPDATE subject_collection SET collectionType = :collectionType, lastUpdated = :lastUpdated WHERE subjectId = :subjectId""")
    suspend fun updateType(
        subjectId: Int,
        collectionType: UnifiedCollectionType,
        lastUpdated: Long = currentTimeMillis(),
    )

    @Query("""DELETE FROM subject_collection WHERE subjectId = :subjectId""")
    suspend fun delete(subjectId: Int)

    @Query("""DELETE FROM subject_collection WHERE collectionType = :type""")
    suspend fun deleteAll(type: UnifiedCollectionType)

    @Query("""DELETE FROM subject_collection""")
    suspend fun deleteAll()

    /**
     * Retrieves a paginated list of `SubjectCollectionEntity` items, optionally filtered by type.
     *
     * @param collectionTypes Optional filter for the `type` of items. If `null`, all items are retrieved.
     * @param limit Specifies the maximum number of items to retrieve.
     * @param offset Defines the starting position within the result set, allowing for pagination.
     * @return A `Flow` of a list of `SubjectCollectionEntity` items.
     */
    @Query(
        """
    SELECT * FROM subject_collection 
    WHERE collectionType IS NOT NULL 
    AND (collectionType IN (:collectionTypes))
    ORDER BY lastUpdated DESC
    LIMIT :limit
    OFFSET :offset
    """,
    )
    fun filterMostRecentUpdated(
        collectionTypes: List<UnifiedCollectionType>,
        limit: Int,
        offset: Int = 0,
    ): Flow<List<SubjectCollectionEntity>>

    @Query(
        """
    SELECT * FROM subject_collection 
    WHERE collectionType IS NOT NULL 
    ORDER BY lastUpdated DESC
    LIMIT :limit
    OFFSET :offset
    """,
    )
    fun mostRecentUpdated(
        limit: Int,
        offset: Int = 0,
    ): Flow<List<SubjectCollectionEntity>>

    /**
     * Retrieves a paginated list of `SubjectCollectionEntity` items, optionally filtered by type.
     *
     * @param collectionType Optional filter for the `type` of items. If `null`, all items are retrieved. If empty, no item will be returned.
     * @return A `Flow` of a list of `SubjectCollectionEntity` items.
     */
    @Query(
        """
        select * from subject_collection 
        where (collectionType is NOT NULL AND (:collectionType IS NULL OR collectionType = :collectionType))
        order by lastUpdated DESC
        """,
    )
    @Transaction
    fun filterByCollectionTypePaging(
        collectionType: UnifiedCollectionType? = null,
    ): PagingSource<Int, SubjectCollectionAndEpisodes>

    @Query("""SELECT * FROM subject_collection WHERE subjectId = :subjectId""")
    fun findById(subjectId: Int): Flow<SubjectCollectionEntity?>

    @Query("""SELECT lastUpdated FROM subject_collection ORDER BY lastUpdated DESC LIMIT 1""")
    suspend fun lastUpdated(): Long

    @Query("""UPDATE subject_collection SET self_rating_score = :score, self_rating_comment = :comment, self_rating_tags = :tags, self_rating_isPrivate = :private WHERE subjectId = :subjectId""")
    suspend fun updateRating(subjectId: Int, score: Int?, comment: String?, tags: List<String>?, private: Boolean?)

    /**
     * 只包含保存在数据库的, 可能不完整
     */
    @Query("""SELECT COUNT(*) FROM subject_collection WHERE (collectionType is NOT NULL AND (:collectionType IS NULL OR collectionType = :collectionType))""")
    fun countCollected(collectionType: UnifiedCollectionType?): Flow<Int>

    @Query("""UPDATE subject_collection SET cachedStaffUpdated = :time, cachedCharactersUpdated = :time WHERE subjectId = :subjectId""")
    suspend fun updateCachedRelationsUpdated(subjectId: Int, time: Long = currentTimeMillis())
}

suspend inline fun SubjectCollectionDao.deleteAll(type: UnifiedCollectionType?) {
    if (type == null) {
        deleteAll()
    } else {
        deleteAll(type)
    }
}

data class SubjectCollectionAndEpisodes(
    @Embedded
    val collection: SubjectCollectionEntity,
    @Relation(
        entity = EpisodeCollectionEntity::class,
        parentColumn = "subjectId",
        entityColumn = "subjectId",
    )
    val episodes: List<EpisodeCollectionEntity>,
) {
    override fun toString(): String {
        return "SubjectCollectionAndEpisodes(collection.nameCn=${collection.nameCn}, episodes.size=${episodes.size})"
    }
}

fun SubjectCollectionDao.filterMostRecentUpdated(
    collectionTypes: List<UnifiedCollectionType>?,
    limit: Int,
    offset: Int = 0,
): Flow<List<SubjectCollectionEntity>> = if (collectionTypes == null) {
    mostRecentUpdated(limit, offset)
} else {
    filterMostRecentUpdated(collectionTypes, limit, offset)
}

fun SubjectCollectionDao.filterMostRecentUpdated(
    collectionType: UnifiedCollectionType? = null,
    limit: Int,
): Flow<List<SubjectCollectionEntity>> = filterMostRecentUpdated(listOfNotNull(collectionType), limit)
