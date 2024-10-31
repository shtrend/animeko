/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.exploration.followed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import me.him188.ani.app.data.models.subject.FollowedSubjectInfo
import me.him188.ani.app.data.models.subject.hasNewEpisodeToPlay
import me.him188.ani.app.data.models.subject.subjectInfo
import me.him188.ani.app.ui.external.placeholder.placeholder
import me.him188.ani.app.ui.foundation.AsyncImage
import me.him188.ani.app.ui.foundation.layout.BasicCarouselItem
import me.him188.ani.app.ui.foundation.layout.CarouselItemDefaults
import me.him188.ani.app.ui.foundation.layout.isHeightAtLeastMedium
import me.him188.ani.app.ui.foundation.stateOf
import me.him188.ani.app.ui.subject.AiringLabelState

// https://www.figma.com/design/LET1n9mmDa6npDTIlUuJjU/Animeko?node-id=62-4581&node-type=frame&t=Evw0PwXZHXQNgEm3-0
@Composable
fun FollowedSubjectsLazyRow(
    items: LazyPagingItems<FollowedSubjectInfo>, // null means placeholder
    onClick: (FollowedSubjectInfo) -> Unit,
    modifier: Modifier = Modifier,
    imageSize: DpSize = if (currentWindowAdaptiveInfo().isHeightAtLeastMedium) DpSize(160.dp, (160.dp) / 9 * 16)
    else DpSize(120.dp, (120.dp) / 9 * 16),
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    verticalAlignment: Alignment.Vertical = Alignment.Top,
) {
    LazyRow(
        modifier,
        lazyListState,
        contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        items(
            items.itemCount,
            key = items.itemKey { it.subjectInfo.subjectId },
            contentType = items.itemContentType { it.subjectProgressInfo.hasNewEpisodeToPlay },
        ) { index ->
            val item = items[index]
            BasicCarouselItem(
                label = { CarouselItemDefaults.Text(item?.subjectInfo?.displayName ?: "") },
                Modifier.placeholder(item == null, shape = CarouselItemDefaults.shape),
                supportingText = {
                    if (item != null) {
                        val airingState = remember(item) {
                            AiringLabelState(
                                stateOf(item.subjectAiringInfo),
                                stateOf(item.subjectProgressInfo),
                            )
                        }
                        airingState.progressText?.let {
                            Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                maskShape = MaterialTheme.shapes.large,
            ) {
                if (item != null) {
                    Surface({ onClick(item) }) {
                        AsyncImage(
                            item.subjectInfo.imageLarge,
                            modifier = Modifier.size(imageSize),
                            contentDescription = item.subjectInfo.displayName,
                            contentScale = ContentScale.Crop,
                        )
                    }
                } else {
                    Box(Modifier.size(imageSize))
                }
            }
        }
    }
}

