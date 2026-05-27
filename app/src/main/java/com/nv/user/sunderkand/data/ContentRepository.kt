package com.nv.user.sunderkand.data

import android.content.Context
import com.nv.user.sunderkand.data.model.Chalisa
import com.nv.user.sunderkand.data.model.ContentBundle
import kotlinx.serialization.json.Json

/**
 * Reads the bundled `assets/content.json` exactly once per process and
 * exposes the parsed [ContentBundle] from then on. There is no network
 * call, no Room, no Firebase — the content is fully embedded in the APK.
 *
 * The repository is intentionally stateless beyond the cache: the
 * Application class holds a single instance, screens read from it on
 * each composition (cheap — it's a simple field access after first
 * load), and the cached bundle never changes for the life of the
 * process.
 */
class ContentRepository(private val appContext: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile
    private var cached: ContentBundle? = null

    /** Parses and caches `content.json` on first call. Thread-safe. */
    fun load(): ContentBundle {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val text = appContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val bundle = json.decodeFromString<ContentBundle>(text)
            cached = bundle
            return bundle
        }
    }

    /** Convenience: find a chalisa by id. */
    fun chalisaById(id: String): Chalisa? = load().chalisas.firstOrNull { it.id == id }

    /** All chalisas, in source order (sunderkand, hanuman_chalisa, ...). */
    fun allChalisas(): List<Chalisa> = load().chalisas

    companion object {
        private const val ASSET_NAME = "content.json"
    }
}
