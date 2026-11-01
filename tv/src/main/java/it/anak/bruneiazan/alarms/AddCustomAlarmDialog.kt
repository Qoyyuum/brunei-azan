package it.anak.bruneiazan.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.OutlinedButton
import it.anak.bruneiazan.shared.settings.CustomAlarm
import java.util.UUID

/**
 * D-pad friendly "add alarm" dialog. The Material3 TimePicker used on
 * mobile isn't remote-friendly, so hour/minute are wheel columns: focus a
 * wheel, D-pad up/down changes the value with wrap-around, left/right moves
 * between wheels. The label field opens the on-screen keyboard on select.
 */
@Composable
fun AddCustomAlarmDialog(onDismiss: () -> Unit, onAdd: (CustomAlarm) -> Unit) {
    var hour by remember { mutableIntStateOf(12) }
    var minute by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    val hourFocus = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.border)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Add alarm", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Wheel(
                    value = hour,
                    count = 24,
                    onValueChange = { hour = it },
                    modifier = Modifier.focusRequester(hourFocus),
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Wheel(
                    value = minute,
                    count = 60,
                    onValueChange = { minute = it },
                )
            }

            var labelFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .border(
                        1.dp,
                        if (labelFocused) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.border
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { labelFocused = it.isFocused },
                    decorationBox = { inner ->
                        if (label.isEmpty()) {
                            Text(
                                "Label (optional)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onAdd(
                            CustomAlarm(
                                id = UUID.randomUUID().toString(),
                                hour = hour,
                                minute = minute,
                                label = label.trim(),
                                enabled = true,
                            ),
                        )
                    },
                    modifier = Modifier.padding(start = 12.dp),
                ) { Text("Add") }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { hourFocus.requestFocus() }
}

/**
 * One number wheel: shows prev/current/next, D-pad up/down steps the value
 * with wrap-around (consumed as key events so they don't move focus).
 */
@Composable
private fun Wheel(
    value: Int,
    count: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (ev.key) {
                    Key.DirectionUp -> {
                        onValueChange(TimeWheel.wrap(value - 1, count))
                        true
                    }
                    Key.DirectionDown -> {
                        onValueChange(TimeWheel.wrap(value + 1, count))
                        true
                    }
                    else -> false
                }
            }
            .focusTarget()
            .border(
                1.dp,
                if (focused) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.border
                },
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        WheelNumber(TimeWheel.wrap(value - 1, count), dim = true)
        Text(
            "%02d".format(value),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        WheelNumber(TimeWheel.wrap(value + 1, count), dim = true)
    }
}

@Composable
private fun WheelNumber(value: Int, dim: Boolean) {
    Text(
        "%02d".format(value),
        style = MaterialTheme.typography.bodyLarge,
        color = if (dim) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
}
