/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

@file:OptIn(TestOnly::class)

package me.him188.ani.app.ui.subject.episode.video.sidesheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import me.him188.ani.app.ui.subject.episode.mediaFetch.emptyMediaSourceResultsPresentation
import me.him188.ani.app.ui.subject.episode.mediaFetch.rememberTestMediaSelectorPresentation
import me.him188.ani.app.ui.subject.episode.mediaFetch.rememberTestMediaSourceInfoProvider
import me.him188.ani.utils.platform.annotations.TestOnly

@Composable
@Preview
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
fun PreviewEpisodeVideoMediaSelectorSideSheet() {
    ProvideCompositionLocalsForPreview {
        EpisodeVideoMediaSelectorSideSheet(
            mediaSelectorState = rememberTestMediaSelectorPresentation(),
            mediaSourceResultsPresentation = emptyMediaSourceResultsPresentation(),
            rememberTestMediaSourceInfoProvider(),
            onDismissRequest = {},
            onRefresh = {},
        )
    }
}
