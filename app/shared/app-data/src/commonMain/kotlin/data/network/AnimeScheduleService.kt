/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import me.him188.ani.app.data.models.schedule.AnimeRecurrence
import me.him188.ani.app.data.models.schedule.AnimeScheduleInfo
import me.him188.ani.app.data.models.schedule.AnimeSeason
import me.him188.ani.app.data.models.schedule.AnimeSeasonId
import me.him188.ani.app.data.models.schedule.OnAirAnimeInfo
import me.him188.ani.client.apis.ScheduleAniApi
import me.him188.ani.client.models.AniAnimeRecurrence
import me.him188.ani.client.models.AniAnimeSeason
import me.him188.ani.client.models.AniAnimeSeasonId
import me.him188.ani.client.models.AniOnAirAnimeInfo
import me.him188.ani.utils.coroutines.IO_
import kotlin.coroutines.CoroutineContext

class AnimeScheduleService(
    apiLazy: Lazy<ScheduleAniApi>,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO_
) {
    private val api by apiLazy

    suspend fun getSeasonIds(): List<AnimeSeasonId> {
        return withContext(ioDispatcher) {
            api.getAnimeSeasons().body().list.map {
                it.toAnimeSeasonId()
            }
        }
    }

    suspend fun getScheduleInfo(seasonId: AnimeSeasonId): AnimeScheduleInfo {
        return withContext(ioDispatcher) {
            AnimeScheduleInfo(
                api.getAnimeSeason(seasonId.id).body().list.map {
                    it.toAnimeScheduleInfo()
                },
            )
        }
    }
}

private fun AniOnAirAnimeInfo.toAnimeScheduleInfo(): OnAirAnimeInfo {
    return OnAirAnimeInfo(
        bangumiId = bangumiId,
        name = name,
        aliases = aliases,
        begin = begin?.let { Instant.parse(it) },
        recurrence = recurrence?.toAnimeRecurrence(),
        end = end?.let { Instant.parse(it) },
        mikanId = mikanId,
    )
}

private fun AniAnimeRecurrence?.toAnimeRecurrence(): AnimeRecurrence? {
    return this?.let {
        AnimeRecurrence(
            startTime = Instant.parse(it.startTime),
            intervalMillis = it.intervalMillis,
        )
    }
}

private fun AniAnimeSeasonId.toAnimeSeasonId(): AnimeSeasonId {
    return AnimeSeasonId(
        year = year,
        season = AnimeSeason.fromQuarterNumber(season.quarterNumber)
            ?: throw IllegalArgumentException("Unknown season: $season"),
    )
}

private val AniAnimeSeason.quarterNumber: Int
    get() = when (this) {
        AniAnimeSeason.SPRING -> 1
        AniAnimeSeason.SUMMER -> 2
        AniAnimeSeason.AUTUMN -> 3
        AniAnimeSeason.WINTER -> 4
    }
