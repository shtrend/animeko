/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

@file:Suppress("SameParameterValue")

package me.him188.ani.app.domain.media.selector.domain.media.selector

import me.him188.ani.app.domain.media.selector.MediaExclusionReason.FromSeriesSeason
import me.him188.ani.app.domain.media.selector.checkSubjectExclusion
import me.him188.ani.app.domain.media.selector.testFramework.addSimpleMediaSelectorTest
import me.him188.ani.app.domain.media.selector.testFramework.assertMedias
import me.him188.ani.app.domain.media.selector.testFramework.runSimpleMediaSelectorTestSuite
import me.him188.ani.datasources.api.EpisodeSort
import me.him188.ani.datasources.api.EpisodeType
import me.him188.ani.datasources.api.topic.EpisodeRange
import me.him188.ani.test.TestContainer
import me.him188.ani.test.TestFactory
import me.him188.ani.test.runDynamicTests
import kotlin.test.Test

/**
 * 测试 [me.him188.ani.datasources.api.EpisodeType.SP]
 */
@TestContainer
class MediaSelectorSpTest {
    @Test
    fun `when watching SP23 - match SP23`() = runSimpleMediaSelectorTestSuite(
        buildTest = {
            initSubject("A") {
                episodeSort = EpisodeSort(23, EpisodeType.SP)
                episodeEp = EpisodeSort(0) // This is a normal scenario!
            }
            mediaApi.addSimpleWebMedia(
                subjectName = "A",
                episodeSort = EpisodeSort(23, EpisodeType.SP),
            )
        },
    ) {
        assertMedias {
            single().assert(included = true)
        }
    }

    @Test
    fun `when watching SP23 - match main 23`() = runSimpleMediaSelectorTestSuite(
        buildTest = {
            initSubject("玉子市场") {
                episodeSort = EpisodeSort(23, EpisodeType.SP)
                episodeEp = EpisodeSort(0) // This is a normal scenario!
            }
            mediaApi.addSimpleWebMedia(
                subjectName = "玉子市场",
                episodeSort = EpisodeSort(23, EpisodeType.MainStory),
            )
        },
    ) {
        assertMedias {
            single().assert(included = true)
        }
    }

    @Test
    fun `when watching SP23 - match both SP23 and main 23`() = runSimpleMediaSelectorTestSuite(
        buildTest = {
            initSubject("玉子市场") {
                episodeSort = EpisodeSort(23, EpisodeType.SP)
                episodeEp = EpisodeSort(0) // This is a normal scenario!
            }
            mediaApi.addSimpleWebMedia(
                subjectName = "玉子市场",
                episodeSort = EpisodeSort(23, EpisodeType.SP),
            )
            mediaApi.addSimpleWebMedia(
                subjectName = "玉子市场",
                episodeSort = EpisodeSort(23, EpisodeType.MainStory),
            )
        },
    ) {
        assertMedias {
            onSingle(episodeRange = EpisodeRange.single(EpisodeSort(23, EpisodeType.SP))).assert(included = true)
            onSingle(episodeRange = EpisodeRange.single(EpisodeSort(23, EpisodeType.MainStory))).assert(included = true)
        }
    }


    @TestFactory
    fun `exclude main subject when playing movie`() = runDynamicTests {
        addSimpleMediaSelectorTest(
            "玉子市场剧场版",
            {
                initSubject("玉子市场 剧场版") {
                    aliases(
                        "玉子爱情故事",
                        "Tamako Love Story",
                        "たまこラブストーリー",
                    )
                    seriesInfo(seasonSort = 1) {
                        series(
                            // 主番:
                            "玉子市场",
                            "Tamako Market",
                            "たまこまーけっと",
                        )
                    }
                }
            },
        ) {
            checkSubjectExclusion {
                expect(
                    "玉子市场" to FromSeriesSeason,
                    "玉子市场 剧场版" to null,
                    "玉子爱情故事" to null,
                    "Tamako Market" to FromSeriesSeason,
                    "Tamako Love Story" to null,
                    "たまこまーけっと" to FromSeriesSeason,
                    "たまこラブストーリー" to null,
                )
            }
        }
    }
}