package com.nv.user.sunderkand.data

import com.nv.user.sunderkand.data.model.ContentBundle
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plain-JVM sanity checks over the bundled `assets/content.json`.
 *
 * These are the things that, if wrong, ship a broken app without any
 * compiler complaint: unparseable JSON, duplicate ids (nav + prefs keys
 * collide), empty verses, or an `audio` name with no matching entry in
 * [AudioCatalog] (the v4.0 "audio vanished" bug).
 */
class ContentIntegrityTest {

    private val bundle: ContentBundle by lazy {
        val file = listOf(
            File("src/main/assets/content.json"),
            File("app/src/main/assets/content.json"),
        ).first { it.exists() }
        Json { ignoreUnknownKeys = true; coerceInputValues = true }
            .decodeFromString(ContentBundle.serializer(), file.readText())
    }

    @Test
    fun `content parses and is non-empty`() {
        assertTrue(bundle.chalisas.isNotEmpty())
    }

    @Test
    fun `chalisa ids are unique and url-safe`() {
        val ids = bundle.chalisas.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue("id '$id' must be [a-z0-9_]", id.matches(Regex("[a-z0-9_]+")))
        }
    }

    @Test
    fun `every chalisa has a title and at least one verse with text`() {
        bundle.chalisas.forEach { c ->
            assertTrue("${c.id} has blank title", c.title.isNotBlank())
            assertTrue("${c.id} has no verses", c.verseCount > 0)
            c.sections.forEach { s ->
                s.verses.forEach { v ->
                    assertTrue("${c.id} has a verse with no lines", v.lines.isNotEmpty())
                    assertTrue("${c.id} has a blank line", v.lines.all { it.isNotBlank() })
                }
            }
        }
    }

    @Test
    fun `every audio reference is wired into AudioCatalog`() {
        bundle.chalisas.filter { it.audio != null }.forEach { c ->
            assertTrue(
                "${c.id} references audio '${c.audio}' which is not in AudioCatalog.byName",
                AudioCatalog.byName.containsKey(c.audio),
            )
        }
    }

    @Test
    fun `verse timings are only on chalisas with audio and strictly increase`() {
        bundle.chalisas.forEach { c ->
            val starts = c.sections.flatMap { it.verses }.mapNotNull { it.startMs }
            if (starts.isEmpty()) return@forEach
            assertTrue("${c.id} has timings but no audio", c.audio != null)
            assertTrue("${c.id}: first timing must be >= 0", starts.first() >= 0L)
            starts.zipWithNext().forEachIndexed { i, (a, b) ->
                assertTrue("${c.id}: startMs not increasing at verse $i ($a -> $b)", b > a)
            }
        }
    }

    @Test
    fun `every catalog entry has a matching mp3 in res raw`() {
        val rawDir = listOf(File("src/main/res/raw"), File("app/src/main/res/raw")).first { it.exists() }
        AudioCatalog.byName.keys.forEach { name ->
            assertTrue("res/raw/$name.mp3 missing", File(rawDir, "$name.mp3").exists())
        }
    }
}
