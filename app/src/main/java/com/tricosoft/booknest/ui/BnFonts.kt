package com.tricosoft.booknest.ui

import android.content.Context
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * Web er font: Playfair Display (heading) + Source Sans 3 (body).
 * Font file res/font/playfair_display.ttf ar res/font/source_sans_3.ttf thakle seta use hoy
 * (scripts/get-fonts.ps1 download kore). Na thakle system serif / sans-serif.
 */
object BnFonts {
    var display: FontFamily = FontFamily.Serif
        private set
    var body: FontFamily = FontFamily.SansSerif
        private set

    fun init(context: Context) {
        display = load(context, "playfair_display", FontFamily.Serif)
        body = load(context, "source_sans_3", FontFamily.SansSerif)
    }

    @OptIn(ExperimentalTextApi::class)
    private fun load(context: Context, name: String, fallback: FontFamily): FontFamily {
        val id = context.resources.getIdentifier(name, "font", context.packageName)
        if (id == 0) return fallback
        return try {
            FontFamily(
                listOf(400, 500, 600, 700).map { w ->
                    Font(
                        resId = id,
                        weight = FontWeight(w),
                        variationSettings = FontVariation.Settings(FontVariation.weight(w)),
                    )
                },
            )
        } catch (e: Exception) {
            fallback
        }
    }
}
