package it.anak.bruneiazan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import it.anak.bruneiazan.TvAppState
import it.anak.bruneiazan.components.SectionHeader
import it.anak.bruneiazan.components.TvRadioRow
import it.anak.bruneiazan.shared.settings.AppFont
import it.anak.bruneiazan.shared.settings.DarkMode
import it.anak.bruneiazan.shared.settings.District
import it.anak.bruneiazan.shared.settings.Settings
import it.anak.bruneiazan.ui.theme.fontFamilyOf
import kotlinx.coroutines.launch

@Composable
fun TvSettingsScreen(app: TvAppState, settings: Settings) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
    ) {
        item { SectionHeader("District") }
        items(District.entries) { d ->
            TvRadioRow(
                label = d.label,
                selected = settings.district == d,
                onClick = { app.scope.launch { app.settingsStore.setDistrict(d) } },
            )
        }

        item { SectionHeader("Appearance") }
        items(DarkMode.entries) { mode ->
            TvRadioRow(
                label = mode.label,
                selected = settings.darkMode == mode,
                onClick = { app.scope.launch { app.settingsStore.setDarkMode(mode) } },
            )
        }

        item { SectionHeader("Font") }
        items(AppFont.entries) { f ->
            TvRadioRow(
                label = f.label,
                selected = settings.font == f,
                onClick = { app.scope.launch { app.settingsStore.setFont(f) } },
                trailing = {
                    Text(
                        "Aa",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = fontFamilyOf(f),
                    )
                },
            )
        }

        item {
            Text(
                "Settings stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        item {
            val credit = buildAnnotatedString {
                append("Made with ❤️ from Brunei 🇧🇳 under ")
                withLink(LinkAnnotation.Url("https://anak.it")) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append("Anak IT Brunei")
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Prayer times from the waktu.co.bn API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    credit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
