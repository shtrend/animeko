/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.cache.details

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ktor.http.Url
import me.him188.ani.app.domain.media.TestMediaList
import me.him188.ani.app.tools.formatDateTime
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.app.ui.media.MediaDetailsRenderer
import me.him188.ani.app.ui.settings.rendering.MediaSourceIcon
import me.him188.ani.datasources.api.Media
import me.him188.ani.datasources.api.MediaExtraFiles
import me.him188.ani.datasources.api.MediaProperties
import me.him188.ani.datasources.api.source.MediaSourceInfo
import me.him188.ani.datasources.api.source.MediaSourceKind
import me.him188.ani.datasources.api.topic.EpisodeRange
import me.him188.ani.datasources.api.topic.FileSize
import me.him188.ani.datasources.api.topic.ResourceLocation
import me.him188.ani.datasources.api.topic.isSingleEpisode
import me.him188.ani.datasources.mikan.MikanCNMediaSource
import me.him188.ani.utils.platform.annotations.TestOnly

@Immutable
data class MediaDetails(
    val originalTitle: String,
    val episodeRange: EpisodeRange?,
    val kind: MediaSourceKind,
    val originalUrl: String,
    val properties: MediaProperties,
    val publishedTimeMillis: Long,
    val download: ResourceLocation,
    val extraFiles: MediaExtraFiles,
    val sourceInfo: MediaSourceInfo?,
) {
    val isUrlLegal = originalUrl.startsWith("http://", ignoreCase = true)
            || originalUrl.startsWith("https://", ignoreCase = true)

    companion object {
        fun from(
            media: Media,
            sourceInfo: MediaSourceInfo?,
        ): MediaDetails {
            return MediaDetails(
                media.originalTitle,
                media.episodeRange,
                media.kind,
                media.originalUrl,
                media.properties,
                media.publishedTime,
                media.download,
                media.extraFiles,
                sourceInfo,
            )
        }
    }
}

@Composable
fun MediaDetailsLazyGrid(
    details: MediaDetails,
    modifier: Modifier = Modifier,
    showSourceInfo: Boolean = true,
) {
    val browser = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current


    val toaster = LocalToaster.current
    LazyVerticalGrid(
        GridCells.Adaptive(minSize = 300.dp),
        modifier,
    ) {
        val copyContent = @Composable { value: () -> String ->
            IconButton(
                {
                    clipboard.setText(AnnotatedString(value()))
                    toaster.toast("已复制")
                },
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制")
            }
        }
        val browseContent = @Composable { url: String ->
            IconButton(
                {
                    if (runCatching { Url(url) }.isSuccess) {
                        browser.openUri(url)
                    } else {
                        clipboard.setText(AnnotatedString(url))
                        toaster.toast("已复制")
                    }
                },
            ) {
                Icon(Icons.Rounded.ArrowOutward, contentDescription = "打开链接")
            }
        }

        item {
            ListItem(
                headlineContent = { SelectionContainer { Text(details.originalTitle) } },
                trailingContent = { copyContent { details.originalTitle } },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("剧集范围") },
                leadingContent = { Icon(Icons.Rounded.Layers, contentDescription = null) },
                supportingContent = {
                    val range = details.episodeRange
                    SelectionContainer {
                        Text(
                            when {
                                range == null -> "未知"
                                range.isSingleEpisode() -> range.knownSorts.firstOrNull().toString()
                                else -> range.toString()
                            },
                        )
                    }
                },
            )
        }
        if (showSourceInfo) {
            item {
                ListItem(
                    headlineContent = { Text("数据源") },
                    leadingContent = { MediaSourceIcon(details.sourceInfo, Modifier.size(24.dp)) },
                    supportingContent = {
                        val kind = when (details.kind) {
                            MediaSourceKind.WEB -> "在线"
                            MediaSourceKind.BitTorrent -> "BT"
                            MediaSourceKind.LocalCache -> "本地"
                        }
                        SelectionContainer { Text("[$kind] ${details.sourceInfo?.displayName ?: "未知"}") }
                    },
                    trailingContent = kotlin.run {
                        val originalUrl by rememberUpdatedState(details.originalUrl)
                        if (details.isUrlLegal) {
                            {
                                browseContent(originalUrl)
                            }
                        } else {
                            {
                                copyContent { originalUrl }
                            }
                        }
                    },
                )
            }
        }
        item {
            ListItem(
                headlineContent = { Text("字幕组") },
                leadingContent = { Icon(Icons.Rounded.Subtitles, contentDescription = null) },
                supportingContent = { SelectionContainer { Text(details.properties.alliance) } },
                trailingContent = { copyContent { details.properties.alliance } },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("字幕语言") },
                leadingContent = { Icon(Icons.Rounded.Subtitles, contentDescription = null) },
                supportingContent = {
                    SelectionContainer {
                        Text(
                            remember(details) {
                                MediaDetailsRenderer.renderSubtitleLanguages(
                                    details.properties.subtitleKind,
                                    details.properties.subtitleLanguageIds,
                                )
                            },
                        )
                    }
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("发布时间") },
                leadingContent = { Icon(Icons.Rounded.Event, contentDescription = null) },
                supportingContent = { SelectionContainer { Text(formatDateTime(details.publishedTimeMillis)) } },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("分辨率") },
                leadingContent = { Icon(Icons.Rounded.Hd, contentDescription = null) },
                supportingContent = { SelectionContainer { Text(details.properties.resolution) } },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("文件大小") },
                leadingContent = { Icon(Icons.Rounded.Description, contentDescription = null) },
                supportingContent = {
                    SelectionContainer {
                        if (details.properties.size == FileSize.Unspecified) {
                            Text("未知")
                        } else {
                            Text(details.properties.size.toString())
                        }
                    }
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text("原始下载方式") },
                leadingContent = { Icon(Icons.Rounded.VideoFile, contentDescription = null) },
                supportingContent = {
                    SelectionContainer {
                        Text(details.download.contentUri, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                },
                trailingContent = { copyContent { details.download.contentUri } },
            )
        }
        details.extraFiles.subtitles.forEachIndexed { index, subtitle ->
            item {
                ListItem(
                    headlineContent = {
                        SelectionContainer {
                            Text(
                                remember(subtitle) {
                                    buildString {
                                        append("外挂字幕 ${index + 1}")
                                        subtitle.language?.let {
                                            append(": ")
                                            append(it)
                                        }
                                    }
                                },
                            )
                        }
                    },
                    leadingContent = { Icon(Icons.Rounded.FilePresent, contentDescription = null) },
                    supportingContent = { SelectionContainer { Text(subtitle.uri) } },
                    trailingContent = { browseContent(subtitle.uri) },
                )
            }
        }
    }
}

@Stable
private val ResourceLocation.contentUri: String
    get() = when (this) {
        is ResourceLocation.HttpStreamingFile -> this.uri
        is ResourceLocation.HttpTorrentFile -> this.uri
        is ResourceLocation.LocalFile -> this.filePath
        is ResourceLocation.MagnetLink -> this.uri
        is ResourceLocation.WebVideo -> this.uri
    }


@TestOnly
val TestMediaDetails
    get() = MediaDetails.from(
        TestMediaList[0],
        MikanCNMediaSource.INFO,
    )
