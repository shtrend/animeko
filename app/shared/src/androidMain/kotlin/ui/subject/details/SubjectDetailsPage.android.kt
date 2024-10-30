/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.details

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.MutableStateFlow
import me.him188.ani.app.ui.foundation.ProvideFoundationCompositionLocalsForPreview
import me.him188.ani.app.ui.foundation.layout.rememberConnectedScrollState
import me.him188.ani.app.ui.foundation.stateOf
import me.him188.ani.app.ui.subject.collection.components.EditableSubjectCollectionTypeButton
import me.him188.ani.app.ui.subject.collection.components.createTestAiringLabelState
import me.him188.ani.app.ui.subject.collection.components.rememberTestEditableSubjectCollectionTypeState
import me.him188.ani.app.ui.subject.collection.progress.TestSubjectProgressInfos
import me.him188.ani.app.ui.subject.collection.progress.rememberTestSubjectProgressState
import me.him188.ani.app.ui.subject.components.comment.generateUiComment
import me.him188.ani.app.ui.subject.components.comment.rememberTestCommentState
import me.him188.ani.app.ui.subject.details.components.CollectionData
import me.him188.ani.app.ui.subject.details.components.DetailsTab
import me.him188.ani.app.ui.subject.details.components.SelectEpisodeButtons
import me.him188.ani.app.ui.subject.details.components.SubjectCommentColumn
import me.him188.ani.app.ui.subject.details.components.SubjectDetailsDefaults
import me.him188.ani.app.ui.subject.rating.EditableRating
import me.him188.ani.app.ui.subject.rating.rememberTestEditableRatingState
import me.him188.ani.datasources.api.topic.UnifiedCollectionType
import me.him188.ani.utils.platform.annotations.TestOnly

@OptIn(TestOnly::class)
@Preview
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
internal fun PreviewSubjectDetails() {
    ProvideFoundationCompositionLocalsForPreview {
        val state = remember {
            SubjectDetailsState(
                info = TestSubjectInfo,
                selfCollectionTypeState = stateOf(UnifiedCollectionType.WISH),
                airingLabelState = createTestAiringLabelState(),
                charactersPager = createTestPager(TestSubjectCharacterList),
                totalCharactersCountState = stateOf(TestSubjectCharacterList.size),
                staffPager = createTestPager(emptyList()),
                totalStaffCountState = stateOf(0),
                relatedSubjectsPager = createTestPager(TestRelatedSubjects),
            )
        }
        val connectedScrollState = rememberConnectedScrollState()
        SubjectDetailsPage(
            state = state,
            onClickOpenExternal = {},
            collectionData = {
                SubjectDetailsDefaults.CollectionData(
                    collectionStats = state.info.collectionStats,
                )
            },
            collectionActions = {
                EditableSubjectCollectionTypeButton(
                    rememberTestEditableSubjectCollectionTypeState(),
                )
            },
            rating = {
                EditableRating(
                    state = rememberTestEditableRatingState(),
                )
            },
            selectEpisodeButton = {
                SubjectDetailsDefaults.SelectEpisodeButtons(
                    rememberTestSubjectProgressState(info = TestSubjectProgressInfos.ContinueWatching2),
                    onShowEpisodeList = {},
                    onPlay = {},
                )
            },
            connectedScrollState = connectedScrollState,
            detailsTab = {
                SubjectDetailsDefaults.DetailsTab(
                    info = TestSubjectInfo,
                    staff = rememberTestLazyPagingItems(TestSubjectStaffInfo),
                    totalStaffCount = TestSubjectStaffInfo.size,
                    characters = rememberTestLazyPagingItems(TestSubjectCharacterList),
                    totalCharactersCount = TestSubjectCharacterList.size,
                    relatedSubjects = rememberTestLazyPagingItems(TestRelatedSubjects),
                    Modifier.nestedScroll(connectedScrollState.nestedScrollConnection),
                )
            },
            commentsTab = {
                val lazyListState = rememberLazyListState()

                SubjectDetailsDefaults.SubjectCommentColumn(
                    state = rememberTestCommentState(commentList = generateUiComment(10)),
                    onClickUrl = { },
                    onClickImage = {},
                    connectedScrollState = connectedScrollState,
                    lazyListState = lazyListState,
                )
            },
            discussionsTab = {},
        )
    }
}

@TestOnly
@Composable
fun <T : Any> rememberTestLazyPagingItems(list: List<T>): LazyPagingItems<T> {
    return createTestPager(list).collectAsLazyPagingItems()
}

@TestOnly
fun <T : Any> createTestPager(list: List<T>) = MutableStateFlow(PagingData.from(list))
