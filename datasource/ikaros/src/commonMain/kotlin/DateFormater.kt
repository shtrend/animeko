/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.datasources.ikaros

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

internal class DateFormater {
    /**
     * UTC Date Str format.
     * Such as: 2023-10-13T00:00:00
     */
    private val utcDateFormat: DateFormat = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss",
        Locale.getDefault(),
    )

    fun utcDateStr2timeStamp(dateStr: String): Long {
        if (dateStr.isEmpty()) {
            return 0
        }
        return utcDateFormat.parse(dateStr).time
    }

    companion object {
        val default by lazy(LazyThreadSafetyMode.PUBLICATION) {
            DateFormater()
        }
    }

}
