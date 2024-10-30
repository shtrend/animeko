/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.details

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.him188.ani.app.data.models.subject.RelatedCharacterInfo
import me.him188.ani.app.data.models.subject.RelatedPersonInfo
import me.him188.ani.app.data.models.subject.RelatedSubjectInfo
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.app.ui.subject.collection.components.AiringLabelState
import me.him188.ani.datasources.api.topic.UnifiedCollectionType

/**
 * 条目详情页 UI 状态. 所有属性 null 都表示正在加载中.
 */
@Stable
class SubjectDetailsState(
    val info: SubjectInfo,
    selfCollectionTypeState: State<UnifiedCollectionType>,
    val airingLabelState: AiringLabelState,

    // 附加信息, pager
    val staffPager: Flow<PagingData<RelatedPersonInfo>>,
    val totalStaffCountState: State<Int?>,
    val charactersPager: Flow<PagingData<RelatedCharacterInfo>>,
    val totalCharactersCountState: State<Int?>,
    val relatedSubjectsPager: Flow<PagingData<RelatedSubjectInfo>>,
) {
    val coverImageUrl get() = this.info.imageLarge
    private val selfCollectionTypeOrNull by selfCollectionTypeState
    val selfCollectionType by derivedStateOf { selfCollectionTypeOrNull }

    val selfCollected by derivedStateOf { this.selfCollectionType != UnifiedCollectionType.NOT_COLLECTED }
}
