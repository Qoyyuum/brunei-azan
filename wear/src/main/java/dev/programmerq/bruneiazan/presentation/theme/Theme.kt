package dev.programmerq.bruneiazan.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import dev.programmerq.bruneiazan.shared.settings.AppFont

private val PaperWhite = Color(0xFFFAFAF7)
private val InkBlack = Color(0xFF111111)
private val InkMuted = Color(0xFF6B6B66)
private val PaperDark = Color(0xFF0D0D0D)
private val InkWhite = Color(0xFFEDEDE8)
private val InkMutedDark = Color(0xFF9A9A93)

private val LightPaperScheme = ColorScheme(
    primary = InkBlack,
    onPrimary = PaperWhite,
    background = PaperWhite,
    onBackground = InkBlack,
    surfaceContainer = PaperWhite,
    onSurface = InkBlack,
    onSurfaceVariant = InkMuted,
    outline = InkMuted,
)

private val DarkPaperScheme = ColorScheme(
    primary = InkWhite,
    onPrimary = PaperDark,
    background = PaperDark,
    onBackground = InkWhite,
    surfaceContainer = PaperDark,
    onSurface = InkWhite,
    onSurfaceVariant = InkMutedDark,
    outline = InkMutedDark,
)

fun fontFamilyOf(font: AppFont): FontFamily = when (font) {
    AppFont.SANS -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONO -> FontFamily.Monospace
}

@Composable
fun BruneiAzanTheme(
    dark: Boolean = true,
    font: AppFont = AppFont.SANS,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkPaperScheme else LightPaperScheme,
        typography = Typography(defaultFontFamily = fontFamilyOf(font)),
        content = content,
    )
}
