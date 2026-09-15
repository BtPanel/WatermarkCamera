package com.watermarkcamera.studio

import android.content.Context
import android.graphics.Typeface
import android.net.Uri

object FontSupport {
    fun resolve(context: Context, layer: WatermarkLayer): Typeface {
        val base = when (layer.font) {
            WatermarkFont.DEFAULT -> Typeface.DEFAULT
            WatermarkFont.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            WatermarkFont.SERIF -> Typeface.SERIF
            WatermarkFont.MONOSPACE -> Typeface.MONOSPACE
            WatermarkFont.CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
            WatermarkFont.CUSTOM -> loadCustom(context, layer.fontUri) ?: Typeface.DEFAULT
        }
        return Typeface.create(base, if (layer.bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun loadCustom(context: Context, uriValue: String?): Typeface? {
        val uri = uriValue?.let(Uri::parse) ?: return null
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                Typeface.Builder(descriptor.fileDescriptor).build()
            }
        }.getOrNull()
    }
}
