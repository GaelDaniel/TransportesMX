package com.example.transportesmx.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GeocodingUtils {
    suspend fun geocodeAddress(address: String): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(address, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "TransportesMX/1.1")
            val response = conn.inputStream.bufferedReader().readText()
            val jsonArray = JSONArray(response)
            if (jsonArray.length() > 0) {
                val obj = jsonArray.getJSONObject(0)
                GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
            } else null
        } catch (e: Exception) { null }
    }
}
