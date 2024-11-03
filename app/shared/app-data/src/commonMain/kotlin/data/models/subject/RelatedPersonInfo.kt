/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.models.subject

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

data class RelatedPersonInfo(
    val index: Int,
    val personInfo: PersonInfo,
    val position: PersonPosition,
) {
    companion object {
        private val SORT_ORDER by lazy {
            listOf(
                PersonPosition.ANIMATION_PRODUCTION,
                PersonPosition.ORIGINAL_WORK,
                PersonPosition.DIRECTOR,
                PersonPosition.SCRIPT_WRITER,
                PersonPosition.MUSIC,
                PersonPosition.CHARACTER_DESIGN,
                PersonPosition.SERIES_COMPOSITION,
                PersonPosition.ART_DESIGN,
                PersonPosition.ACTION_ANIMATION_DIRECTOR,
            )
        }

        val ImportanceOrder = compareBy<RelatedPersonInfo> { info ->
            val index = SORT_ORDER.indexOfFirst { position ->
                info.position == position
            }
            if (index == -1) {
                Int.MAX_VALUE
            } else {
                index
            }
        }
    }
}

@JvmInline // save memory. Too many subclasses.
value class PersonPosition(val id: Int) {
    companion object {
        /**
         * 原作
         */
        val ORIGINAL_WORK = PersonPosition(1)

        /**
         * 导演
         */
        val DIRECTOR = PersonPosition(2)

        /**
         * 脚本
         */
        val SCRIPT_WRITER = PersonPosition(3)

        /**
         * 分镜
         */
        val STORYBOARD_ARTIST = PersonPosition(4)

        /**
         * 演出
         */
        val PERFORMANCE_DIRECTOR = PersonPosition(5)

        /**
         * 音乐
         */
        val MUSIC = PersonPosition(6)

        /**
         * 人物原案
         */
        val CHARACTER_ORIGINAL = PersonPosition(7)

        /**
         * 人物设定
         */
        val CHARACTER_DESIGN = PersonPosition(8)

        /**
         * 系列构成
         */
        val SERIES_COMPOSITION = PersonPosition(9)

        /**
         * 美术监督
         */
        val ART_DIRECTOR = PersonPosition(10)

        /**
         * 色彩设计
         */
        val COLOR_DESIGN = PersonPosition(11)

        /**
         * 总作画监督
         */
        val CHIEF_ANIMATION_DIRECTOR = PersonPosition(12)

        /**
         * 作画监督
         */
        val ANIMATION_DIRECTOR = PersonPosition(13)

        /**
         * 摄影监督
         */
        val CINEMATOGRAPHY_DIRECTOR = PersonPosition(14)

        /**
         * 道具设计
         */
        val PROP_DESIGN = PersonPosition(15)

        /**
         * 原画
         */
        val KEY_ANIMATOR = PersonPosition(16)

        /**
         * 剪辑
         */
        val EDITOR = PersonPosition(17)

        /**
         * 主题歌编曲
         */
        val THEME_SONG_ARRANGER = PersonPosition(18)

        /**
         * 主题歌作曲
         */
        val THEME_SONG_COMPOSER = PersonPosition(19)

        /**
         * 主题歌作词
         */
        val THEME_SONG_LYRICIST = PersonPosition(20)

        /**
         * 主题歌演出
         */
        val THEME_SONG_PERFORMER = PersonPosition(21)

        /**
         * 企画
         */
        val PLANNING = PersonPosition(22)

        /**
         * 制作管理
         */
        val PRODUCTION_MANAGER = PersonPosition(23)

        /**
         * 製作
         */
        val PRODUCTION = PersonPosition(24)

        /**
         * 设定
         */
        val SETTING = PersonPosition(25)

        /**
         * 音响监督
         */
        val SOUND_DIRECTOR = PersonPosition(26)

        /**
         * 制片人
         */
        val PRODUCER = PersonPosition(27)

        /**
         * 总制片人
         */
        val EXECUTIVE_PRODUCER = PersonPosition(28)

        /**
         * 音乐制作
         */
        val MUSIC_PRODUCTION = PersonPosition(29)

        /**
         * 动画制作
         */
        val ANIMATION_PRODUCTION = PersonPosition(30)

        /**
         * 机械作画监督
         */
        val MECHANICAL_ANIMATION_DIRECTOR = PersonPosition(31)

        /**
         * 美术设计
         */
        val ART_DESIGN = PersonPosition(32)

        /**
         * OP・ED 分镜
         */
        val OP_ED_STORYBOARD = PersonPosition(33)

        /**
         * 3DCG
         */
        val THREE_D_CG = PersonPosition(34)

        /**
         * 制作协力
         */
        val PRODUCTION_SUPPORT = PersonPosition(35)

        /**
         * 动作作画监督
         */
        val ACTION_ANIMATION_DIRECTOR = PersonPosition(36)

        /**
         * 设定制作
         */
        val SETTING_PRODUCTION = PersonPosition(37)

        /**
         * 音乐制作人
         */
        val MUSIC_PRODUCER = PersonPosition(38)

        /**
         * 主演出
         */
        val CHIEF_PERFORMANCE_DIRECTOR = PersonPosition(39)

        /**
         * 作画监督助理
         */
        val ASSISTANT_ANIMATION_DIRECTOR = PersonPosition(40)

        val range = 1..40
    }
}

private val names by lazy(LazyThreadSafetyMode.PUBLICATION) {
    mutableIntObjectMapOf<String>().apply {
        put(PersonPosition.ORIGINAL_WORK.id, "原作")
        put(PersonPosition.DIRECTOR.id, "导演")
        put(PersonPosition.SCRIPT_WRITER.id, "脚本")
        put(PersonPosition.STORYBOARD_ARTIST.id, "分镜")
        put(PersonPosition.PERFORMANCE_DIRECTOR.id, "演出")
        put(PersonPosition.MUSIC.id, "音乐")
        put(PersonPosition.CHARACTER_ORIGINAL.id, "人物原案")
        put(PersonPosition.CHARACTER_DESIGN.id, "人物设定")
        put(PersonPosition.SERIES_COMPOSITION.id, "系列构成")
        put(PersonPosition.ART_DIRECTOR.id, "美术监督")
        put(PersonPosition.COLOR_DESIGN.id, "色彩设计")
        put(PersonPosition.CHIEF_ANIMATION_DIRECTOR.id, "总作画监督")
        put(PersonPosition.ANIMATION_DIRECTOR.id, "作画监督")
        put(PersonPosition.CINEMATOGRAPHY_DIRECTOR.id, "摄影监督")
        put(PersonPosition.PROP_DESIGN.id, "道具设计")
        put(PersonPosition.KEY_ANIMATOR.id, "原画")
        put(PersonPosition.EDITOR.id, "剪辑")
        put(PersonPosition.THEME_SONG_ARRANGER.id, "主题歌编曲")
        put(PersonPosition.THEME_SONG_COMPOSER.id, "主题歌作曲")
        put(PersonPosition.THEME_SONG_LYRICIST.id, "主题歌作词")
        put(PersonPosition.THEME_SONG_PERFORMER.id, "主题歌演出")
        put(PersonPosition.PLANNING.id, "企画")
        put(PersonPosition.PRODUCTION_MANAGER.id, "制作管理")
        put(PersonPosition.PRODUCTION.id, "製作")
        put(PersonPosition.SETTING.id, "设定")
        put(PersonPosition.SOUND_DIRECTOR.id, "音响监督")
        put(PersonPosition.PRODUCER.id, "制片人")
        put(PersonPosition.EXECUTIVE_PRODUCER.id, "总制片人")
        put(PersonPosition.MUSIC_PRODUCTION.id, "音乐制作")
        put(PersonPosition.ANIMATION_PRODUCTION.id, "动画制作")
        put(PersonPosition.MECHANICAL_ANIMATION_DIRECTOR.id, "机械作画监督")
        put(PersonPosition.ART_DESIGN.id, "美术设计")
        put(PersonPosition.OP_ED_STORYBOARD.id, "OP・ED 分镜")
        put(PersonPosition.THREE_D_CG.id, "3DCG")
        put(PersonPosition.PRODUCTION_SUPPORT.id, "制作协力")
        put(PersonPosition.ACTION_ANIMATION_DIRECTOR.id, "动作作画监督")
        put(PersonPosition.SETTING_PRODUCTION.id, "设定制作")
        put(PersonPosition.MUSIC_PRODUCER.id, "音乐制作人")
        put(PersonPosition.CHIEF_PERFORMANCE_DIRECTOR.id, "主演出")
        put(PersonPosition.ASSISTANT_ANIMATION_DIRECTOR.id, "作画监督助理")
    }
}

@Stable
val PersonPosition.nameCn: String?
    get() = names[id]
