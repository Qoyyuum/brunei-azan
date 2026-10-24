package dev.programmerq.bruneiazan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.programmerq.bruneiazan.AppState
import dev.programmerq.bruneiazan.components.RadioRow
import dev.programmerq.bruneiazan.components.SectionHeader
import dev.programmerq.bruneiazan.shared.settings.AppFont
import dev.programmerq.bruneiazan.shared.settings.DarkMode
import dev.programmerq.bruneiazan.shared.settings.District
import dev.programmerq.bruneiazan.shared.settings.Settings
import dev.programmerq.bruneiazan.ui.theme.fontFamilyOf
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: AppState, settings: Settings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        SectionHeader("District")
        District.entries.forEach { d ->
            RadioRow(
                label = d.label,
                selected = settings.district == d,
                onClick = { app.scope.launch { app.settingsStore.setDistrict(d) } },
            )
        }

        SectionHeader("Appearance")
        DarkMode.entries.forEach { mode ->
            RadioRow(
                label = mode.label,
                selected = settings.darkMode == mode,
                onClick = { app.scope.launch { app.settingsStore.setDarkMode(mode) } },
            )
        }

        SectionHeader("Font")
        AppFont.entries.forEach { f ->
            RadioRow(
                label = f.label,
                selected = settings.font == f,
                onClick = { app.scope.launch { app.settingsStore.setFont(f) } },
                trailing = {
                    Text("Aa", style = MaterialTheme.typography.titleLarge, fontFamily = fontFamilyOf(f))
                },
            )
        }

        Text(
            "Changes sync to your watch when connected.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )

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
