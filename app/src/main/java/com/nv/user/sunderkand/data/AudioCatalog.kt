package com.nv.user.sunderkand.data

import com.nv.user.sunderkand.R

/**
 * Explicit table from a chalisa's `audio` name (content.json) to the
 * bundled `R.raw` resource.
 *
 * This is deliberately NOT `resources.getIdentifier()`: a name-based
 * lookup leaves no static reference to the MP3s, so R8's resource
 * shrinker removed them from the v4.0 release bundle and audio silently
 * disappeared for every user. Add a row here whenever a new audio file
 * lands in `res/raw/`; `ContentIntegrityTest` fails the build if
 * content.json references a name that is missing from this table.
 */
object AudioCatalog {
    val byName: Map<String, Int> = mapOf(
        "sunder" to R.raw.sunder,
        "hanumanchalisa" to R.raw.hanumanchalisa,
    )

    /** Resource id for [name], or 0 when there is no bundled audio. */
    fun rawIdFor(name: String?): Int = if (name.isNullOrBlank()) 0 else byName[name] ?: 0
}
