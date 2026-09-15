package com.watermarkcamera.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var showAmapKey by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "返回") }
                    Text("设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SettingTitle(Icons.Outlined.Map, "高德地图")
                OutlinedTextField(
                    value = settings.amapApiKey,
                    onValueChange = { value ->
                        onChange(settings.copy(amapApiKey = value.filterNot(Char::isWhitespace)))
                    },
                    label = { Text("高德地图 Android Key") },
                    supportingText = { Text("留空时使用工程内置 Key") },
                    singleLine = true,
                    visualTransformation = if (showAmapKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showAmapKey = !showAmapKey }) {
                            Icon(
                                if (showAmapKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (showAmapKey) "隐藏 Key" else "显示 Key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                SettingDivider()
                SettingTitle(Icons.Outlined.Tune, "新水印默认值")
                LabeledSlider(
                    title = "默认透明度",
                    valueText = "${(settings.defaultOpacity * 100).toInt()}%",
                    value = settings.defaultOpacity,
                    onValueChange = { onChange(settings.copy(defaultOpacity = it)) },
                    valueRange = .1f..1f
                )
                LabeledSlider(
                    title = "默认文字大小",
                    valueText = settings.defaultFontSize.toInt().toString(),
                    value = settings.defaultFontSize,
                    onValueChange = { onChange(settings.copy(defaultFontSize = it)) },
                    valueRange = 8f..48f
                )
                OutlinedTextField(
                    value = settings.defaultTimeFormat,
                    onValueChange = { onChange(settings.copy(defaultTimeFormat = it)) },
                    label = { Text("默认时间格式") },
                    supportingText = { Text("例如 yyyy-MM-dd HH:mm:ss") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("yyyy-MM-dd  HH:mm", "yyyy/MM/dd", "HH:mm:ss").forEach { format ->
                        FilterChip(
                            selected = settings.defaultTimeFormat == format,
                            onClick = { onChange(settings.copy(defaultTimeFormat = format)) },
                            label = { Text(format) }
                        )
                    }
                }
                SettingDivider()
                SettingTitle(Icons.Outlined.Edit, "编辑器")
                SettingSwitch(
                    title = "中心吸附",
                    checked = settings.snapToCenter,
                    onChecked = { onChange(settings.copy(snapToCenter = it)) }
                )
                SettingSwitch(
                    title = "显示中心参考线",
                    checked = settings.showGuides,
                    onChecked = { onChange(settings.copy(showGuides = it)) }
                )
                SettingSwitch(
                    title = "深色模式",
                    checked = settings.darkMode,
                    onChecked = { onChange(settings.copy(darkMode = it)) }
                )
                SettingDivider()
                SettingTitle(Icons.Outlined.FileDownload, "导出")
                LabeledSlider(
                    title = "默认 JPEG 质量",
                    valueText = "${settings.exportQuality}%",
                    value = settings.exportQuality.toFloat(),
                    onValueChange = { onChange(settings.copy(exportQuality = it.toInt())) },
                    valueRange = 60f..100f
                )
                OutlinedButton(
                    onClick = { onChange(AppSettings()) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Icon(Icons.Outlined.RestartAlt, null, Modifier.size(18.dp))
                    Text("恢复默认设置", Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingTitle(icon: ImageVector, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            value,
            Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        Modifier.padding(top = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun LabeledSlider(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
