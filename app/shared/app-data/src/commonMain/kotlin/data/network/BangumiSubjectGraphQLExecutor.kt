/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.him188.ani.app.data.repository.Repository
import me.him188.ani.datasources.bangumi.BangumiClient

object BangumiSubjectGraphQLExecutor {
    private const val SUBJECT_DETAILS_FRAGMENTS = """
fragment Ep on Episode {
  id
  type
  name
  name_cn
  airdate
  comment
  description
  sort
}

fragment SubjectFragment on Subject {
  id
  type
  name
  name_cn
  images{large, common}
  characters {
    order
    type
    character {
      id
      name
      comment
      collects
      infobox {
        key 
        values {k 
                v}
      }
      role
      images {
        large
        medium
      }
    }
  }
  infobox {
    values {
      k
      v
    }
    key
  }
  summary
  eps
  collection{collect , doing, dropped, on_hold, wish}
  airtime{date}
  rating{count, rank, score, total}
  nsfw
  tags{count, name}
  
  persons {
    person {
      career
      collects
      comment
      id
      images {
        large
        medium
      }
      infobox {
        key
        values {
          k
          v
        } 
      }
      last_post
      lock
      name
      nsfw
      redirect
      summary
      type
    }
    position
  }

  leadingEpisodes : episodes(limit: 100) { ...Ep }
  trailingEpisodes : episodes(limit: 1, offset: -1) { ...Ep }
  # episodes{id, type, name, name_cn, sort, airdate, comment, duration, description, disc, ep, }
}
        """

    private const val QUERY_1 = """
$SUBJECT_DETAILS_FRAGMENTS
query BatchGetSubjectQuery(${'$'}id: Int!) {
  s0:subject(id: ${'$'}id){...SubjectFragment}
}
"""

    // 服务器会缓存 query 编译, 用 variables 可以让查询更快
    private val QUERY_WHOLE_PAGE by lazy {
        buildString {
            appendLine(SUBJECT_DETAILS_FRAGMENTS)

            appendLine("query BatchGetSubjectQuery(")
            repeat(Repository.defaultPagingConfig.pageSize) { i ->
                append("\$id").append(i).append(": Int!")
                if (i != Repository.defaultPagingConfig.pageSize - 1) {
                    append(", ")
                }
            }
            appendLine(") {")

            repeat(Repository.defaultPagingConfig.pageSize) { i ->
                append('s')
                append(i)
                append(":subject(id: \$id").append(i).append("){...SubjectFragment}")
                appendLine()
            }

            append("}")
        }
    }

    suspend fun executeQuerySubjectDetails(client: BangumiClient, ids: List<Int>): JsonObject {
        val actionName = "SubjectCollectionRepositoryImpl.batchGetSubjectDetails"
        // 尽量使用 variables
        return when (ids.size) {
            1 -> {
                client.executeGraphQL(
                    actionName,
                    QUERY_1,
                    variables = buildJsonObject {
                        put("id", ids.first())
                    },
                )
            }

            Repository.defaultPagingConfig.pageSize -> {
                client.executeGraphQL(
                    actionName,
                    QUERY_WHOLE_PAGE,
                    variables = buildJsonObject {
                        repeat(ids.size) { i ->
                            put("id$i", ids[i])
                        }
                    },
                )
            }

            else -> {
                client.executeGraphQL(
                    actionName,
                    buildString(
                        capacity = SUBJECT_DETAILS_FRAGMENTS.length + 30 + 55 * ids.size, // big enough to avoid resizing
                    ) {
                        appendLine(SUBJECT_DETAILS_FRAGMENTS)
                        appendLine("query BatchGetSubjectQuery {")
                        for (id in ids) {
                            append('s')
                            append(id)
                            append(":subject(id: ").append(id).append("){...SubjectFragment}")
                            appendLine()
                        }
                        append("}")
                    },
                )
            }
        }
    }
}