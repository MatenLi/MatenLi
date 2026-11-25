package dev.rffrr.mateli.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import dev.rffrr.mateli.settings.PrefKeys
import dev.rffrr.mateli.settings.rememberPrefs

/**
 * Material 3 theme. Night Mode: 1 = dark, 2 = light, 0 and 3 follow the system.
 * App Theme: 0 = dynamic colour, 1-5 = preset accents, 6 = custom hex colour.
 */
@Composable
fun MatenLiTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = rememberPrefs()
    val dark = when (prefs.string(PrefKeys.NIGHT_THEME, "0")) {
        "1" -> true
        "2" -> false
        else -> isSystemInDarkTheme()
    }
    val accent = accentColor(
        prefs.string(PrefKeys.APP_THEME, "0"),
        prefs.string(PrefKeys.CUSTOM_ACCENT, "#6750A4")
    )
    val scheme: ColorScheme = when {
        accent != null -> presetScheme(accent, dark)
        dark -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Accent presets for the App Theme setting. */
val presetAccents: Map<String, Color> = mapOf(
    "1" to Color(0xFF6750A4),
    "2" to Color(0xFF386A9C),
    "3" to Color(0xFF2E7D32),
    "4" to Color(0xFFB3261E),
    "5" to Color(0xFFE65100)
)

/** Accent for a setting value, or null for dynamic colour. */
fun accentColor(value: String, custom: String): Color? = when (value) {
    "0" -> null
    "6" -> parseHexColor(custom)
    else -> presetAccents[value]
}

private fun parseHexColor(text: String): Color? = try {
    Color(android.graphics.Color.parseColor(text.trim()))
} catch (e: IllegalArgumentException) {
    null
}

/** Builds a scheme around one accent. Roles not set here keep Material defaults. */
private fun presetScheme(seed: Color, dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = lerp(seed, Color.White, 0.4f),
            onPrimary = lerp(seed, Color.Black, 0.8f),
            primaryContainer = lerp(seed, Color.Black, 0.6f),
            onPrimaryContainer = lerp(seed, Color.White, 0.8f),
            secondary = Color(0xFF81C784),
            onSecondary = Color(0xFF003910),
            tertiary = Color(0xFFFFCC80),
            onTertiary = Color(0xFF4A2800)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = lerp(seed, Color.White, 0.85f),
            onPrimaryContainer = lerp(seed, Color.Black, 0.5f),
            secondary = Color(0xFF2E7D32),
            onSecondary = Color.White,
            tertiary = Color(0xFFB26A00),
            onTertiary = Color.White
        )
    }
