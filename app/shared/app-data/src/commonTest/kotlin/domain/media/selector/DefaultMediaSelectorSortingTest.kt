/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.media.selector

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.him188.ani.app.data.models.preference.MediaPreference
import me.him188.ani.app.data.models.subject.SubjectInfo
import me.him188.ani.datasources.api.DefaultMedia
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultMediaSelectorSortingTest : AbstractDefaultMediaSelectorTest() {

    @Test
    fun `sort by subjectName similarity`() = runTest {
        mediaSelectorContext.value = createMediaSelectorContextFromEmpty(
            subjectInfo = SubjectInfo.Empty.copy(nameCn = "孤独摇滚", name = "Bocchi The Rock!"),
        )
        savedDefaultPreference.value = MediaPreference.Any
        savedUserPreference.value = MediaPreference.Any
        val expectedSort = listOf(
            media(subjectName = "孤独摇滚"),
            media(subjectName = "Bocchi The Rock!"),
            media(subjectName = "孤独摇滚!"),
            media(subjectName = "孤单摇滚"),
            media(subjectName = "孤独摇滚 第二季"),
            media(subjectName = "孤单摇滚 第二季"),
        )
        assertSort(expectedSort)
    }

    private suspend fun assertSort(expectedSort: List<DefaultMedia>) {
        addMedia(*expectedSort.toTypedArray().apply { shuffle() })
        assertEquals(
            expectedSort.indices.toList(),
            selector.filteredCandidatesMedia.first().map { expectedSort.indexOf(it) },
        )
    }
}