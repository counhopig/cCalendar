package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.counhopig.ccalendar.data.WidgetSettings
import com.counhopig.ccalendar.data.WidgetSettingsRepository
import com.counhopig.ccalendar.ui.viewmodel.EventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsSheet(
    viewModel: EventViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { WidgetSettingsRepository(context) }
    var settings by remember { mutableStateOf(repository.getSettings()) }
    
    // We update the widget immediately on change or on save?
    // Let's update on save/dismiss to avoid too many writes, 
    // but maybe update a preview?
    // For now, simple fields.

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "小组件设置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Font Color
            ColorSettingRow(
                label = "字体颜色",
                color = Color(settings.fontColor),
                onColorSelected = { color ->
                    settings = settings.copy(fontColor = color.toArgb())
                }
            )

            // Background Color
            ColorSettingRow(
                label = "背景颜色",
                color = Color(settings.backgroundColor),
                onColorSelected = { color ->
                    settings = settings.copy(backgroundColor = color.toArgb())
                }
            )

            // Transparency
            Column {
               Text("背景透明度: ${(settings.backgroundTransparency * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
               Slider(
                   value = settings.backgroundTransparency,
                   onValueChange = { settings = settings.copy(backgroundTransparency = it) },
                   valueRange = 0f..1f
               )
            }

            // Corner Radius
            Column {
                Text("圆角: ${settings.cornerRadius} dp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.cornerRadius.toFloat(),
                    onValueChange = { settings = settings.copy(cornerRadius = it.toInt()) },
                    valueRange = 0f..40f,
                    steps = 40
                )
            }

            Button(
                onClick = {
                    repository.saveSettings(settings)
                    viewModel.updateWidgets(context) // Helper to trigger update
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存并在桌面更新")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ColorSettingRow(
    label: String,
    color: Color,
    onColorSelected: (Color) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        ColorPickerDialog(
            initialColor = color,
            onDismiss = { showDialog = false },
            onColorSelected = { 
                onColorSelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    // Simple preset colors + RGB sliders
    var red by remember { mutableStateOf(initialColor.red) }
    var green by remember { mutableStateOf(initialColor.green) }
    var blue by remember { mutableStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("选择颜色", style = MaterialTheme.typography.titleMedium)

                // Preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )

                // Sliders
                ColorSlider("R", red, Color.Red) { red = it }
                ColorSlider("G", green, Color.Green) { green = it }
                ColorSlider("B", blue, Color.Blue) { blue = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onColorSelected(currentColor) }) { Text("确定") }
                }
            }
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
        Text((value * 255).toInt().toString(), modifier = Modifier.width(30.dp))
    }
}
