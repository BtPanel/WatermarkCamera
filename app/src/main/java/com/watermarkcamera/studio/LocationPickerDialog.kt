package com.watermarkcamera.studio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    initialText: String = "",
    confirmLabel: String = "添加"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("privacy", 0) }
    var accepted by remember { mutableStateOf(prefs.getBoolean("amap_accepted", false)) }
    var manualText by remember(initialText) { mutableStateOf(initialText) }
    var keyword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hasApiKey = AmapService.hasApiKey(context)

    fun locate() {
        loading = true
        error = null
        scope.launch {
            AmapService.locateOnce(context).onSuccess {
                manualText = it.watermarkText.ifBlank { "${it.latitude}, ${it.longitude}" }
                city = it.city
            }.onFailure { error = it.message ?: "定位失败" }
            loading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) locate()
        else error = "未授予位置权限，可以手动搜索或输入"
    }

    if (!accepted) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("使用高德位置服务") },
            text = {
                Text("为生成位置水印，应用会在你主动点击定位或搜索时使用高德地图服务。定位信息只用于本次水印编辑；你也可以拒绝并手动填写位置。")
            },
            confirmButton = {
                Button(onClick = {
                    accepted = true
                    prefs.edit().putBoolean("amap_accepted", true).apply()
                    AmapService.setPrivacyConsent(context, true)
                }) { Text("同意并继续") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("暂不使用") } }
        )
        return
    }

    LaunchedEffect(accepted) { AmapService.setPrivacyConsent(context, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置位置水印") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (!hasApiKey) {
                    Text(
                        "尚未配置高德 Key，请在设置中填写；当前仍可手动输入。",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    label = { Text("最终显示的位置文字") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("搜索地点") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                AmapService.searchPlaces(context, keyword, city).onSuccess { results = it }
                                    .onFailure { error = it.message ?: "搜索失败" }
                                loading = false
                            }
                        },
                        enabled = keyword.isNotBlank() && !loading && hasApiKey,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Icon(Icons.Outlined.Search, "搜索") }
                }
                TextButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) locate() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    enabled = !loading && hasApiKey
                ) {
                    Icon(Icons.Outlined.LocationSearching, null)
                    Text("定位到当前位置", Modifier.padding(start = 6.dp))
                }
                if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    results.forEach { place ->
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                manualText = place.watermarkText
                            }.padding(vertical = 9.dp)
                        ) {
                            Text(place.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                place.address.ifBlank { "${place.latitude}, ${place.longitude}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onPick(manualText.trim()) }, enabled = manualText.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
