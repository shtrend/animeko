/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.mediasource.selector.test

import androidx.annotation.UiThread
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.him188.ani.app.domain.mediasource.test.web.SelectorMediaSourceTester
import me.him188.ani.app.domain.mediasource.test.web.SelectorTestEpisodeListResult
import me.him188.ani.app.domain.mediasource.test.web.SelectorTestEpisodePresentation
import me.him188.ani.app.domain.mediasource.test.web.SelectorTestSearchSubjectResult
import me.him188.ani.app.domain.mediasource.test.web.SelectorTestSubjectPresentation
import me.him188.ani.app.domain.mediasource.web.SelectorSearchConfig
import me.him188.ani.app.ui.settings.mediasource.AbstractMediaSourceTestState
import me.him188.ani.datasources.api.EpisodeSort
import me.him188.ani.utils.coroutines.flows.combine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@Immutable
data class SelectorTestPresentation(
    val isSearchingSubject: Boolean,
    val isSearchingEpisode: Boolean,
    val subjectSearchResult: SelectorTestSearchSubjectResult?,
    val episodeListSearchResult: SelectorTestEpisodeListResult?,
    val selectedSubject: SelectorTestSubjectPresentation?,
    val filteredEpisodes: List<SelectorTestEpisodePresentation>?,
    val filterByChannel: String?,
    val selectedSubjectIndex: Int,
    val isPlaceholder: Boolean = false,
) {
    @Stable
    companion object {
        @Stable
        val Placeholder = SelectorTestPresentation(
            isSearchingSubject = false,
            isSearchingEpisode = false,
            subjectSearchResult = null,
            episodeListSearchResult = null,
            selectedSubject = null,
            filteredEpisodes = null,
            filterByChannel = null,
            selectedSubjectIndex = 0,
            isPlaceholder = true,
        )
    }
}

@Stable
class SelectorTestState(
    private val searchConfigState: State<SelectorSearchConfig?>,
    private val tester: SelectorMediaSourceTester,
    backgroundScope: CoroutineScope,
) : AbstractMediaSourceTestState() {
    private val selectedSubjectIndexFlow = MutableStateFlow<Int>(0)

    fun selectSubjectIndex(index: Int) {
        selectedSubjectIndexFlow.value = index
        tester.setSubjectIndex(index)
    }

    private val filterByChannelFlow = MutableStateFlow<String?>(null)
    fun filterByChannel(channel: String?) {
        filterByChannelFlow.value = channel
    }

    private val selectedSubjectFlow = tester.subjectSelectionResultFlow
        .combine(selectedSubjectIndexFlow) { subjectSearchSelectResult, selectedSubjectIndex ->
            val success = subjectSearchSelectResult as? SelectorTestSearchSubjectResult.Success
                ?: return@combine null
            success.subjects.getOrNull(selectedSubjectIndex)
        }

    private val searchUrl by derivedStateOf {
        searchConfigState.value?.searchUrl
    }
    private val useOnlyFirstWord by derivedStateOf {
        searchConfigState.value?.searchUseOnlyFirstWord
    }

    private val filteredEpisodesFlow = tester.episodeListSelectionResultFlow
        .combine(filterByChannelFlow) { result, filterByChannel ->
            when (result) {
                is SelectorTestEpisodeListResult.Success -> result.episodes.filter {
                    filterByChannel == null || it.channel == filterByChannel
                }

                is SelectorTestEpisodeListResult.ApiError -> null
                SelectorTestEpisodeListResult.InvalidConfig -> null
                is SelectorTestEpisodeListResult.UnknownError -> null
            }
        }

    val presentation = combine(
        tester.subjectSearchRunning.isRunning,
        tester.episodeSearchRunning.isRunning,
        tester.subjectSelectionResultFlow,
        tester.episodeListSelectionResultFlow,
        selectedSubjectFlow,
        filteredEpisodesFlow,
        filterByChannelFlow,
        selectedSubjectIndexFlow,
        transform = ::SelectorTestPresentation,
    ).shareIn(backgroundScope, SharingStarted.WhileSubscribed(5000), 1)

    val gridState = LazyGridState()

    @UiThread
    suspend fun observeChanges() {
        // 监听各 UI 编辑属性的变化, 发起重新搜索

        try {
            coroutineScope {
                launch {
                    combine(
                        snapshotFlow { searchKeyword },
                        snapshotFlow { searchUrl },
                        snapshotFlow { useOnlyFirstWord },
                        snapshotFlow { searchConfigState.value },
                    ) { searchKeyword, searchUrl, useOnlyFirstWord, searchConfigState ->
                        SelectorMediaSourceTester.SubjectQuery(
                            searchKeyword,
                            searchUrl,
                            useOnlyFirstWord,
                            searchConfigState?.searchRemoveSpecial,
                        )
                    }.distinctUntilChanged().debounce(0.5.seconds).collect { query ->
                        tester.setSubjectQuery(query)
                    }
                }

                launch {
                    snapshotFlow { searchConfigState.value }.distinctUntilChanged().debounce(0.5.seconds)
                        .collect { config ->
                            tester.setSelectorSearchConfig(config)
                        }
                }

                launch {
                    snapshotFlow { sort }.distinctUntilChanged().debounce(0.5.seconds).collect { sort ->
                        tester.setEpisodeQuery(SelectorMediaSourceTester.EpisodeQuery(EpisodeSort(sort)))
                    }
                }
            }
        } catch (e: CancellationException) {
            tester.clearSubjectQuery()
            throw e
        }
    }


    fun restartCurrentSubjectSearch() {
        tester.subjectSearchLifecycle.restart()
    }

    fun restartCurrentEpisodeSearch() {
        tester.episodeSearchLifecycle.restart()
    }
}

