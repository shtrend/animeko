/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.mediaFetch

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.him188.ani.app.domain.media.selector.MaybeExcludedMedia
import me.him188.ani.app.domain.media.selector.MediaExclusionReason
import me.him188.ani.app.domain.media.selector.UnsafeOriginalMediaAccess
import me.him188.ani.datasources.api.Media
import me.him188.ani.datasources.api.source.MediaSourceKind

@Immutable // only after build
class MediaGroup @MediaGroupBuilderApi internal constructor(
    val groupId: MediaGroupId,
) {
    // 原地 build, 节约内存
    private val _list: ArrayList<MaybeExcludedMedia> = ArrayList(4)
    val list: List<MaybeExcludedMedia> get() = _list

    @Stable
    val first get() = list.first()

    @Stable
    val isExcluded by lazy(LazyThreadSafetyMode.NONE) { list.all { it is MaybeExcludedMedia.Excluded } }

    @Stable
    val exclusionReason: MediaExclusionReason? by lazy(LazyThreadSafetyMode.NONE) { list.firstOrNull { it.exclusionReason != null }?.exclusionReason }

    @MediaGroupBuilderApi
    internal fun add(media: MaybeExcludedMedia) {
        _list.add(media)
    }
}

internal typealias MediaGroupId = String

@RequiresOptIn
internal annotation class MediaGroupBuilderApi

@OptIn(MediaGroupBuilderApi::class)
object MediaGrouper {
    fun getGroupId(media: Media): String {
        return when (media.kind) {
            MediaSourceKind.BitTorrent -> {
                var title = media.originalTitle
                if (title.startsWith('[')) {
                    title = title.substringAfter(']')
                }
                return title
            }

            MediaSourceKind.WEB,
            MediaSourceKind.LocalCache -> media.mediaId
        }
    }

    fun getItemIdWithinGroup(media: Media): String {
        val alliance = media.properties.alliance
        if (alliance.isNotEmpty()) return alliance

        val title = media.originalTitle
        if (title.startsWith('[')) {
            val index = title.indexOf(']')
            if (index != -1) {
                return title.substring(1, index)
            }
        }

        return title
    }

    @OptIn(UnsafeOriginalMediaAccess::class)
    fun buildGroups(list: List<MaybeExcludedMedia>): List<MediaGroup> {
        val groups = HashMap<String, MediaGroup>()
        for (media in list) {
            val groupId = getGroupId(media.original)
            groups.getOrPut(groupId) { MediaGroup(groupId) }
                .add(media)
        }
        return groups.values.toList()
    }
}
