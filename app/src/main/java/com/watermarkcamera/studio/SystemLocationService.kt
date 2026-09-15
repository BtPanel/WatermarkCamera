package com.watermarkcamera.studio

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class CurrentPlace(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?
)

object SystemLocationService {
    @SuppressLint("MissingPermission")
    suspend fun locateOnce(context: Context): Result<CurrentPlace> = runCatching {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = manager.getBestProvider(Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
        }, true) ?: manager.getProviders(true).firstOrNull()
            ?: error("系统定位服务未开启")

        val recent = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, provider)
            .distinct()
            .mapNotNull { name -> runCatching { manager.getLastKnownLocation(name) }.getOrNull() }
            .maxByOrNull(Location::getTime)

        val location = withTimeoutOrNull(10_000L) {
            requestCurrentLocation(context, manager, provider)
        } ?: recent ?: error("暂时无法获取当前位置")

        val address = reverseGeocode(context, location.latitude, location.longitude)
        CurrentPlace(
            address = address,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude.takeIf { location.hasAltitude() }
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(
        context: Context,
        manager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            manager.getCurrentLocation(
                provider,
                signal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            continuation.invokeOnCancellation { signal.cancel() }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }

    private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
        if (!Geocoder.isPresent()) return ""
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
            }
        }
        return address?.displayText().orEmpty()
    }

    private fun Address.displayText(): String {
        val compact = listOf(locality, subLocality, thoroughfare, featureName)
            .filterNotNull()
            .filter(String::isNotBlank)
            .distinct()
            .joinToString("")
        return compact.ifBlank { getAddressLine(0).orEmpty() }
    }
}

fun List<WatermarkLayer>.withCurrentPlace(place: CurrentPlace): List<WatermarkLayer> = map { layer ->
    when {
        layer.kind == LayerKind.LOCATION && place.address.isNotBlank() -> layer.copy(text = when {
            layer.text.startsWith("地点：") -> "地点：${place.address}"
            layer.text.contains("打卡") -> "打卡地点：${place.address}"
            layer.text.contains("到访") || layer.text.contains("拜访") -> "到访地址：${place.address}"
            layer.text.contains("检查") || layer.text.contains("巡检") -> "检查地点：${place.address}"
            layer.text.contains("点击设置") -> "地点：${place.address}"
            else -> place.address
        })
        layer.kind == LayerKind.TEXT && layer.text.contains("经纬度") -> layer.copy(
            text = "经纬度：%.6f, %.6f".format(Locale.US, place.longitude, place.latitude)
        )
        layer.kind == LayerKind.TEXT && layer.text.contains("海拔") && place.altitude != null -> layer.copy(
            text = "海拔：%.1f 米".format(Locale.US, place.altitude)
        )
        else -> layer
    }
}
