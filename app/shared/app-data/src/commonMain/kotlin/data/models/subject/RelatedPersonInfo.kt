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

class RelatedPersonInfo(
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

        fun sortList(personList: List<RelatedPersonInfo>): List<RelatedPersonInfo> {
            /*
           中文名="葬送的芙莉莲"
           别名=[{"v":"Frieren: Beyond Journey's End"},{"v":"Sousou no Frieren"},{"v":"葬送的芙莉蓮"}]
           话数="28"
           放送开始="2023年9月29日"
           放送星期="星期五"
           官方网站="https://frieren-anime.jp/"
           播放电视台="日本テレビ系"
           其他电视台="BS日テレ / AT-X"
           Copyright="© 山田鐘人・アベツカサ／小学館／「葬送のフリーレン」製作委員会"
           原作="山田鐘人・アベツカサ（小学館「週刊少年サンデー」連載中）"
           导演="斎藤圭一郎"
           音乐="Evan Call"
           人物设定="長澤礼子"
           系列构成="鈴木智尋"
           动画制作="MADHOUSE"
           美术设计="杉山晋史"
           动作作画监督="岩澤亨(动作导演)"
           概念艺术="吉岡誠子"
           设定="原科大樹(魔物设计)"
           主题歌编曲="Ayase（OP1） / Evan Call（ED1,SPED）/ n-buna（OP2）"
           主题歌作曲="Ayase（OP1） / milet、野村陽一郎、中村泰輔（ED1） / Evan Call（SPED）/ n-buna（OP2）"
           主题歌作词="Ayase（OP1） / milet（ED1,SPED）/ n-buna（OP2）"
           主题歌演出="YOASOBI（OP1） / milet（ED1,SPED）/ ヨルシカ（OP2）"
           製作="「葬送のフリーレン」製作委員会【東宝（藤田雅規、齋藤雅哉、山田祥子、佐野航）、小学館（島村英司）、日本テレビ（稲毛弘之、中谷敏夫、吉田和生、今井蘭泉）、MADHOUSE、小学館集英社プロダクション、Aniplex、電通】"
           企画="大田圭二、沢辺伸政；共同企划：佐藤貴博、田代早苗、佐藤龍伸、三宅将典、東山敦"
           执行制片人="山中一孝、備前島幹人"
           总制片人="高橋敦司、武井克弘"
           制片人="田口翔一朗、四竈泰介、岩佐直樹、田口亜有理、伊藤悠公、青木遥、菊池瑠梨子"
           副制片人="竹田晃洋、野呂瀬友里、平原唯灯"
           原作协力="大嶋一範、小倉功雅、芳仲宏暢"
           音乐制作="東宝ミュージック、ミラクル・バス"
           音乐制作人="有馬由衣"
           OP・ED 分镜="斎藤圭一郎（OP1）、hohobun（ED1）、吉成鋼（SPED）"
           原画="こはく(中村豊)"
           动画制片人="福士裕一郎＆中目貴史"
            */
            return personList.sortedBy { info ->
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
