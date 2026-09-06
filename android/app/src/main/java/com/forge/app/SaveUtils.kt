package com.forge.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray

private const val PREFS = "kilnwork_gallery"
private const val KEY_URIS = "uris"

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): String? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kilnwork")
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    addToGalleryOrder(context, uri.toString())
    return uri.toString()
}

fun addToGalleryOrder(context: Context, uriString: String) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val current = getGalleryOrder(context).toMutableList()
    current.add(0, uriString)
    val arr = JSONArray()
    current.forEach { arr.put(it) }
    prefs.edit().putString(KEY_URIS, arr.toString()).apply()
}

fun getGalleryOrder(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_URIS, null) ?: return emptyList()
    val arr = JSONArray(raw)
    return (0 until arr.length()).map { arr.getString(it) }
}

fun saveGalleryOrder(context: Context, uris: List<String>) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val arr = JSONArray()
    uris.forEach { arr.put(it) }
    prefs.edit().putString(KEY_URIS, arr.toString()).apply()
}
