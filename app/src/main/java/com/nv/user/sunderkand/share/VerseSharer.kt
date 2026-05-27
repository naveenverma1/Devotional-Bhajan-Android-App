package com.nv.user.sunderkand.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.nv.user.sunderkand.data.model.Chalisa
import com.nv.user.sunderkand.data.model.Verse
import com.nv.user.sunderkand.data.model.VerseType

/**
 * Builds shareable text for a single verse and dispatches the system
 * share / clipboard intents.
 *
 * Share text format:
 *
 *   {chalisa title}                            (saffron tagline)
 *   {section heading, if any}
 *
 *   ॥{verse.number}॥                          (chaupais only)
 *   {line 1}
 *   {line 2}
 *
 *   — Sunderkand Path & Chalisa
 *   https://play.google.com/store/apps/details?id=com.nv.user.sunderkand
 *
 * We keep it text-only on purpose — Devanagari renders identically in
 * WhatsApp / Instagram / Telegram message bodies, so a graphic isn't
 * needed for v4.0 to look devotionally appropriate.
 */
object VerseSharer {

    private const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.nv.user.sunderkand"

    fun buildShareText(
        chalisa: Chalisa,
        sectionHeading: String?,
        verse: Verse,
    ): String = buildString {
        appendLine(chalisa.title)
        if (!sectionHeading.isNullOrBlank()) {
            appendLine(sectionHeading)
        }
        appendLine()
        if (verse.type == VerseType.CHAUPAI && verse.number != null) {
            appendLine("॥${verse.number}॥")
        }
        verse.lines.forEach { appendLine(it) }
        appendLine()
        appendLine("— Sunderkand Path & Chalisa")
        append(PLAY_URL)
    }

    fun shareViaSystem(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, "Share verse").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Verse", text)
        clipboard.setPrimaryClip(clip)
    }
}
