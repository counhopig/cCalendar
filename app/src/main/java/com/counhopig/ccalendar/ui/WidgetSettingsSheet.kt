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

private val NeoBackground = Color(0xFFE9EEF6)
private val NeoSurface = Color(0xFFE9EEF6)
private val NeoText = Color(0xFF243044)
private val NeoTextMuted = Color(0xFF738099)
private val NeoAccent = Color(0xFF7C5CFF)

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
        containerColor = NeoBackground,
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
                color = NeoText
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
               Text(
                   "背景透明度: ${(settings.backgroundTransparency * 100).toInt()}%",
                   style = MaterialTheme.typography.bodyMedium,
                   color = NeoTextMuted
               )
               Slider(
                   value = settings.backgroundTransparency,
                   onValueChange = { settings = settings.copy(backgroundTransparency = it) },
                   valueRange = 0f..1f,
                   colors = SliderDefaults.colors(
                       thumbColor = NeoAccent,
                       activeTrackColor = NeoAccent,
                       inactiveTrackColor = Color(0xFFD0D8E6)
                   )
               )
            }

            // Corner Radius
            Column {
                Text(
                    "圆角: ${settings.cornerRadius} dp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeoTextMuted
                )
                Slider(
                    value = settings.cornerRadius.toFloat(),
                    onValueChange = { settings = settings.copy(cornerRadius = it.toInt()) },
                    valueRange = 0f..40f,
                    steps = 40,
                    colors = SliderDefaults.colors(
                        thumbColor = NeoAccent,
                        activeTrackColor = NeoAccent,
                        inactiveTrackColor = Color(0xFFD0D8E6)
                    )
                )
            }

            Button(
                onClick = {
                    repository.saveSettings(settings)
                    viewModel.updateWidgets(context) // Helper to trigger update
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = NeoAccent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("保存并在桌面更新", color = Color.White)
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
        Text(label, style = MaterialTheme.typography.bodyLarge, color = NeoText)
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape)
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
            colors = CardDefaults.cardColors(containerColor = NeoSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("选择颜色", style = MaterialTheme.typography.titleMedium, color = NeoText)

                // Preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                )

                // Sliders
                ColorSlider("R", red, Color.Red) { red = it }
                ColorSlider("G", green, Color.Green) { green = it }
                ColorSlider("B", blue, Color.Blue) { blue = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消", color = NeoTextMuted) }
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("确定", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), color = NeoText)
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
        Text((value * 255).toInt().toString(), modifier = Modifier.width(30.dp), color = NeoTextMuted)
    }
}
