package com.nv.user.sunderkand.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level shape of `app/src/main/assets/content.json`. The Compose
 * reader consumes this directly via kotlinx.serialization — no network,
 * no Room.
 */
@Serializable
data class ContentBundle(
    val version: String = "4.0",
    val chalisas: List<Chalisa> = emptyList(),
)

@Serializable
data class Chalisa(
    /** Stable machine id (used in nav arguments, prefs keys, etc.). */
    val id: String,
    /** Display title in Devanagari. */
    val title: String,
    /** Optional Devanagari author attribution. */
    val author: String? = null,
    /**
     * Filename of the bundled MP3 in `res/raw/`, without extension.
     * `R.raw.<audio>` for use with Media3. `null` for text-only readers.
     */
    val audio: String? = null,
    val sections: List<Section> = emptyList(),
) {
    /** Total verses across all sections (used by the home cards). */
    val verseCount: Int get() = sections.sumOf { it.verses.size }
}

@Serializable
data class Section(
    /** The section heading (दोहा / चौपाई / श्लोक) as authored. */
    val heading: String,
    val verses: List<Verse> = emptyList(),
)

@Serializable
data class Verse(
    val type: VerseType = VerseType.VERSE,
    /** 1-based verse number within the section, or null. */
    val number: Int? = null,
    /** One element per visible line of the verse. */
    val lines: List<String> = emptyList(),
)

@Serializable
enum class VerseType {
    @SerialName("doha")
    DOHA,

    @SerialName("chaupai")
    CHAUPAI,

    @SerialName("shlok")
    SHLOK,

    @SerialName("mantra")
    MANTRA,

    @SerialName("verse")
    VERSE,
}
