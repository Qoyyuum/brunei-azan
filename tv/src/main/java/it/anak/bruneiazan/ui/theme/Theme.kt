package it.anak.bruneiazan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import it.anak.bruneiazan.shared.settings.AppFont

private val LightPaperScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = PaperWhite,
    secondary = InkMuted,
    onSecondary = PaperWhite,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = PaperWhite,
    onSurfaceVariant = InkMuted,
    border = RuleLight,
)

private val DarkPaperScheme = darkColorScheme(
    primary = InkWhite,
    onPrimary = PaperDark,
    secondary = InkMutedDark,
    onSecondary = PaperDark,
    background = PaperDark,
    onBackground = InkWhite,
    surface = PaperDark,
    onSurface = InkWhite,
    surfaceVariant = PaperDark,
    onSurfaceVariant = InkMutedDark,
    border = RuleDark,
)

fun fontFamilyOf(font: AppFont): FontFamily = when (font) {
    AppFont.SANS -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONO -> FontFamily.Monospace
}

@Composable
fun BruneiAzanTvTheme(
    dark: Boolean = false,
    font: AppFont = AppFont.SANS,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkPaperScheme else LightPaperScheme,
        typography = paperTypography(fontFamilyOf(font)),
        content = content,
    )
}
