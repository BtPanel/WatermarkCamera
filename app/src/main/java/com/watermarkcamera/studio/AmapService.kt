package com.watermarkcamera.studio

import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.core.ServiceSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PlaceResult(
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
) {
    val watermarkText: String
        get() = listOf(name, address).filter { it.isNotBlank() }.distinct().joinToString(" · ")
}

object AmapService {
    private fun resolveApiKey(context: Context): String {
        val savedKey = context.getSharedPreferences("watermark_projects", Context.MODE_PRIVATE)
            .getString("amapApiKey", "")
            .orEmpty()
            .trim()
        return savedKey.ifBlank { BuildConfig.AMAP_API_KEY.trim() }
    }

    fun hasApiKey(context: Context): Boolean = resolveApiKey(context).isNotBlank()

    fun applyApiKey(context: Context): Boolean {
        val key = resolveApiKey(context)
        if (key.isBlank()) return false
        AMapLocationClient.setApiKey(key)
        ServiceSettings.getInstance().setApiKey(key)
        MapsInitializer.setApiKey(key)
        return true
    }

    fun setPrivacyConsent(context: Context, agreed: Boolean) {
        AMapLocationClient.updatePrivacyShow(context.applicationContext, true, agreed)
        AMapLocationClient.updatePrivacyAgree(context.applicationContext, agreed)
        ServiceSettings.updatePrivacyShow(context.applicationContext, true, agreed)
        ServiceSettings.updatePrivacyAgree(context.applicationContext, agreed)
    }

    suspend fun locateOnce(context: Context): Result<PlaceResult> = suspendCancellableCoroutine { continuation ->
        if (!applyApiKey(context)) {
            continuation.resume(Result.failure(IllegalStateException("请先在设置中填写高德地图 Key")))
            return@suspendCancellableCoroutine
        }
        val client = runCatching { AMapLocationClient(context.applicationContext) }.getOrElse {
            continuation.resume(Result.failure(it))
            return@suspendCancellableCoroutine
        }
        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isOnceLocationLatest = true
            isNeedAddress = true
            httpTimeOut = 12_000L
        }
        client.setLocationOption(option)
        client.setLocationListener { location ->
            if (!continuation.isActive) return@setLocationListener
            val result = if (location != null && location.errorCode == 0) {
                Result.success(
                    PlaceResult(
                        name = location.aoiName.orEmpty().ifBlank { location.poiName.orEmpty() },
                        address = location.address.orEmpty(),
                        city = location.city.orEmpty(),
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            } else {
                Result.failure(IllegalStateException(location?.errorInfo ?: "定位失败"))
            }
            client.stopLocation()
            client.onDestroy()
            continuation.resume(result)
        }
        continuation.invokeOnCancellation {
            client.stopLocation()
            client.onDestroy()
        }
        client.startLocation()
    }

    suspend fun searchPlaces(context: Context, keyword: String, city: String = ""): Result<List<PlaceResult>> =
        suspendCancellableCoroutine { continuation ->
            if (!applyApiKey(context)) {
                continuation.resume(Result.failure(IllegalStateException("请先在设置中填写高德地图 Key")))
                return@suspendCancellableCoroutine
            }
            if (keyword.isBlank()) {
                continuation.resume(Result.success(emptyList()))
                return@suspendCancellableCoroutine
            }
            val query = PoiSearch.Query(keyword.trim(), "", city).apply {
                pageSize = 20
                pageNum = 1
            }
            val search = runCatching { PoiSearch(context.applicationContext, query) }.getOrElse {
                continuation.resume(Result.failure(it))
                return@suspendCancellableCoroutine
            }
            search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiSearched(result: PoiResult?, code: Int) {
                    if (!continuation.isActive) return
                    if (code == AMapException.CODE_AMAP_SUCCESS) {
                        continuation.resume(Result.success(result?.pois.orEmpty().map(::toPlace)))
                    } else {
                        continuation.resume(Result.failure(IllegalStateException("高德搜索失败：$code")))
                    }
                }

                override fun onPoiItemSearched(item: PoiItem?, code: Int) = Unit
            })
            search.searchPOIAsyn()
        }

    private fun toPlace(item: PoiItem): PlaceResult = PlaceResult(
        name = item.title.orEmpty(),
        address = item.snippet.orEmpty(),
        city = item.cityName.orEmpty(),
        latitude = item.latLonPoint?.latitude ?: 0.0,
        longitude = item.latLonPoint?.longitude ?: 0.0
    )
}
