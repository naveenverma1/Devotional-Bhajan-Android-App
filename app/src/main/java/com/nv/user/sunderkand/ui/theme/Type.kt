package com.nv.user.sunderkand.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nv.user.sunderkand.R

// Bundled Devanagari typography for the v4.0 reader.
//
//  - NotoSerifDevanagari is a variable font (wght axis 100..900). We expose
//    Regular (400) and SemiBold (600) instances; Compose's font matcher
//    will pick the right axis when a Text uses FontWeight.SemiBold.
//  - TiroDevanagariHindi is a single-weight calligraphic display face,
//    used for hero titles and section headings.
//
// The default English-language fallback when a glyph isn't in the
// Devanagari font (digits, Latin chars, etc.) is the system serif.

private val NotoSerifDevanagari = FontFamily(
    Font(
        resId = R.font.noto_serif_devanagari,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.noto_serif_devanagari,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.noto_serif_devanagari,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

private val TiroDevanagariHindi = FontFamily(
    Font(resId = R.font.tiro_devanagari_hindi_regular, weight = FontWeight.Normal),
    Font(
        resId = R.font.tiro_devanagari_hindi_regular,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
    ),
)

/** Body font — used for verse content and all running text. */
internal val BodyFont = NotoSerifDevanagari

/** Display font — used for titles, hero text, section headings. */
internal val DisplayFont = TiroDevanagariHindi

internal val SunderkandTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Normal,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Normal,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Normal,
    ),

    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Normal,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
    ),

    titleLarge = TextStyle(
        fontFamily = BodyFont,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFont,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFont,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),

    // Body — devotee-friendly default of 18sp. The reader screen will
    // multiply this by the user's font-size choice in DataStore.
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontSize = 18.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFont,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),

    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)
