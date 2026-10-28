package com.tricosoft.booknest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** CSS hsl() -> Compose Color (web er --variable gulor moto). */
fun hsl(h: Float, s: Float, l: Float): Color {
    val sat = s / 100f
    val lig = l / 100f
    val c = (1 - abs(2 * lig - 1)) * sat
    val hp = h / 60f
    val x = c * (1 - abs(hp % 2 - 1))
    val r: Float
    val g: Float
    val b: Float
    when {
        hp < 1 -> { r = c; g = x; b = 0f }
        hp < 2 -> { r = x; g = c; b = 0f }
        hp < 3 -> { r = 0f; g = c; b = x }
        hp < 4 -> { r = 0f; g = x; b = c }
        hp < 5 -> { r = x; g = 0f; b = c }
        else -> { r = c; g = 0f; b = x }
    }
    val m = lig - c / 2
    return Color(r + m, g + m, b + m, 1f)
}

@Immutable
class BnColors(
    val dark: Boolean,
    val background: Color,
    val foreground: Color,
    val card: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val border: Color,
    val input: Color,
)

// src/index.css :root
val LightBn = BnColors(
    dark = false,
    background = hsl(36f, 33f, 97f),
    foreground = hsl(24f, 10f, 10f),
    card = hsl(36f, 30f, 95f),
    primary = hsl(28f, 72f, 45f),
    primaryForeground = hsl(36f, 33f, 97f),
    secondary = hsl(36f, 20f, 90f),
    secondaryForeground = hsl(24f, 10f, 20f),
    muted = hsl(36f, 15f, 92f),
    mutedForeground = hsl(24f, 8f, 50f),
    accent = hsl(172f, 40f, 35f),
    destructive = hsl(0f, 72f, 51f),
    destructiveForeground = Color.White,
    border = hsl(30f, 15f, 88f),
    input = hsl(30f, 15f, 88f),
)

// src/index.css .dark
val DarkBn = BnColors(
    dark = true,
    background = hsl(220f, 15f, 8f),
    foreground = hsl(36f, 20f, 92f),
    card = hsl(220f, 15f, 12f),
    primary = hsl(32f, 75f, 50f),
    primaryForeground = hsl(220f, 15f, 8f),
    secondary = hsl(220f, 12f, 18f),
    secondaryForeground = hsl(36f, 20f, 85f),
    muted = hsl(220f, 12f, 16f),
    mutedForeground = hsl(36f, 10f, 55f),
    accent = hsl(172f, 40f, 40f),
    destructive = hsl(0f, 62f, 45f),
    destructiveForeground = Color.White,
    border = hsl(220f, 12f, 20f),
    input = hsl(220f, 12f, 20f),
)

val LocalBn = staticCompositionLocalOf { LightBn }

@Composable
@ReadOnlyComposable
fun bn(): BnColors = LocalBn.current

@Composable
fun BookNestTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkBn else LightBn
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.primaryForeground,
            background = c.background, onBackground = c.foreground,
            surface = c.card, onSurface = c.foreground,
            secondary = c.secondary, onSecondary = c.secondaryForeground,
            error = c.destructive, onError = c.destructiveForeground,
            outline = c.border,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.primaryForeground,
            background = c.background, onBackground = c.foreground,
            surface = c.card, onSurface = c.foreground,
            secondary = c.secondary, onSecondary = c.secondaryForeground,
            error = c.destructive, onError = c.destructiveForeground,
            outline = c.border,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalBn provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
