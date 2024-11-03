/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.torrent.parcel

import android.os.Build
import android.os.Parcelable
import android.os.SharedMemory
import androidx.annotation.RequiresApi
import kotlinx.parcelize.Parcelize
import me.him188.ani.app.torrent.api.files.EncodedTorrentInfo

@RequiresApi(Build.VERSION_CODES.O_MR1)
@Parcelize
class PEncodedTorrentInfo(
    val dataMem: SharedMemory,
    val length: Int
) : Parcelable {
    fun toEncodedTorrentInfo(): EncodedTorrentInfo {
        val buffer = dataMem.mapReadOnly()
        val data = ByteArray(length).apply { buffer.get(this) }
        dataMem.close()
        return EncodedTorrentInfo.createRaw(data)
    }
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
fun EncodedTorrentInfo.toParceled(): PEncodedTorrentInfo {
    val dataMem = SharedMemory.create("encoded_torrent_info${data.hashCode()}", data.size)
    dataMem.mapReadWrite().apply { put(data, 0, data.size) }
    return PEncodedTorrentInfo(dataMem, data.size)
}