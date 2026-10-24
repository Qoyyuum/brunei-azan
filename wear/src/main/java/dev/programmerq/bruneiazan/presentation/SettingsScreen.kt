package dev.programmerq.bruneiazan.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.programmerq.bruneiazan.presentation.theme.fontFamilyOf
import dev.programmerq.bruneiazan.shared.settings.AppFont
import dev.programmerq.bruneiazan.shared.settings.DarkMode
import dev.programmerq.bruneiazan.shared.settings.District
import dev.programmerq.bruneiazan.shared.settings.Settings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: WearAppState, settings: Settings, listState: TransformingLazyColumnState) {
    TransformingLazyColumn(state = listState, modifier = Modifier.padding(horizontal = 10.dp)) {
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) { Text("District") }
        }
        District.entries.forEach { d ->
            item {
                Button(
                    onClick = { app.scope.launch { app.settingsStore.setDistrict(d) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Text("${if (settings.district == d) "●" else "○"} ${d.label}")
                }
            }
        }
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) { Text("Theme") }
        }
        DarkMode.entries.forEach { mode ->
            item {
                Button(
                    onClick = { app.scope.launch { app.settingsStore.setDarkMode(mode) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Text(
                        "${if (settings.darkMode == mode) "●" else "○"} ${mode.label}",
                    )
                }
            }
        }
        item {
            ListHeader(modifier = Modifier.fillMaxWidth()) { Text("Font") }
        }
        AppFont.entries.forEach { f ->
            item {
                Button(
                    onClick = { app.scope.launch { app.settingsStore.setFont(f) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Text(
                        "${if (settings.font == f) "●" else "○"} ${f.label}  Aa",
                        fontFamily = fontFamilyOf(f),
                    )
                }
            }
        }
    }
}
