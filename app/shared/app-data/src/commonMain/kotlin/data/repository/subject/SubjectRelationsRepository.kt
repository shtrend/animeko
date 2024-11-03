/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.repository.subject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.him188.ani.app.data.models.subject.CharacterInfo
import me.him188.ani.app.data.models.subject.PersonInfo
import me.him188.ani.app.data.models.subject.RelatedCharacterInfo
import me.him188.ani.app.data.models.subject.RelatedPersonInfo
import me.him188.ani.app.data.persistent.database.dao.RelatedCharacterView
import me.him188.ani.app.data.persistent.database.dao.RelatedPersonView
import me.him188.ani.app.data.persistent.database.dao.SubjectCollectionDao
import me.him188.ani.app.data.persistent.database.dao.SubjectRelationsDao
import me.him188.ani.app.data.persistent.database.entity.CharacterEntity
import me.him188.ani.app.data.persistent.database.entity.PersonEntity
import me.him188.ani.app.data.repository.Repository
import me.him188.ani.utils.platform.collections.mapToIntArray
import kotlin.coroutines.CoroutineContext

sealed interface SubjectRelationsRepository : Repository {
    fun subjectRelatedPersonsFlow(subjectId: Int): Flow<List<RelatedPersonInfo>>
    fun subjectRelatedCharactersFlow(subjectId: Int): Flow<List<RelatedCharacterInfo>>
}

class DefaultSubjectRelationsRepository(
    private val subjectCollectionDao: SubjectCollectionDao,
    private val subjectRelationsDao: SubjectRelationsDao,
    private val defaultDispatcher: CoroutineContext = Dispatchers.Default,
) : SubjectRelationsRepository {
    override fun subjectRelatedPersonsFlow(subjectId: Int): Flow<List<RelatedPersonInfo>> {
        return subjectRelationsDao.subjectRelatedPersonsFlow(subjectId).map { list ->
            list.mapTo(ArrayList(list.size)) {
                it.toRelatedPersonInfo()
            }.apply {
                sortWith(RelatedPersonInfo.ImportanceOrder)
            }
        }.flowOn(defaultDispatcher)
    }

    override fun subjectRelatedCharactersFlow(subjectId: Int): Flow<List<RelatedCharacterInfo>> {
        return subjectRelationsDao.subjectRelatedCharactersFlow(subjectId).flatMapLatest { list ->
            subjectRelationsDao.characterActorsFlow(list.mapToIntArray { it.character.characterId })
                .map { actors ->
                    list.mapTo(ArrayList(list.size)) { relatedCharacterView ->
                        val characterId = relatedCharacterView.character.characterId
                        relatedCharacterView.toRelatedCharacterInfo(
                            actors = actors
                                .asSequence()
                                .filter { it.characterId == characterId }
                                .map { it.person.toPersonInfo() }
                                .toList()
                        )
                    }.apply {
                        sortWith(RelatedCharacterInfo.ImportanceOrder)
                    }
                }
        }.flowOn(defaultDispatcher)
    }

}

private fun RelatedCharacterView.toRelatedCharacterInfo(
    actors: List<PersonInfo>,
): RelatedCharacterInfo {
    return RelatedCharacterInfo(
        index = index,
        character = character.toCharacterInfo(actors),
        role = role,
    )
}

private fun CharacterEntity.toCharacterInfo(actors: List<PersonInfo>): CharacterInfo {
    return CharacterInfo(
        id = characterId,
        name = name,
        nameCn = nameCn,
        actors = actors,
        imageLarge = imageLarge,
        imageMedium = imageMedium,
    )
}

private fun RelatedPersonView.toRelatedPersonInfo(): RelatedPersonInfo {
    return RelatedPersonInfo(
        index = index,
        personInfo = person.toPersonInfo(),
        position = position,
    )
}

private fun PersonEntity.toPersonInfo(): PersonInfo {
    return PersonInfo(
        id = personId,
        name = name,
        type = type,
        careers = emptyList(),
        imageLarge = imageLarge,
        imageMedium = imageMedium,
        summary = summary,
        locked = false,
        nameCn = nameCn,
    )
}
