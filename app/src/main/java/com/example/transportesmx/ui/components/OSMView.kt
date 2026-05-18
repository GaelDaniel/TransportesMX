package com.example.transportesmx.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.transportesmx.models.UnitData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun OSMView(
    modifier: Modifier,
    startPoint: GeoPoint? = null,
    endPoint: GeoPoint? = null,
    units: List<UnitData> = emptyList()
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    LaunchedEffect(startPoint, endPoint) {
        if (startPoint != null && endPoint != null) {
            routePoints = withContext(Dispatchers.IO) { fetchOsrmRoute(startPoint, endPoint) }
        }
    }

    AndroidView(factory = {
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true) // Habilita botones de zoom
            isTilesScaledToDpi = true
            controller.setZoom(12.0)
            controller.setCenter(startPoint ?: GeoPoint(25.6866, -100.3161))
        }
    }, update = { map ->
        map.overlays.removeAll { it is Marker || it is Polyline }
        
        // Dibujar Unidades
        units.forEach { u ->
            if (u.lastLat != 0.0) {
                val m = Marker(map)
                m.position = GeoPoint(u.lastLat, u.lastLng)
                m.title = "Eco: ${u.numeroEconomico}"
                m.snippet = "Estado: ${u.status}"
                map.overlays.add(m)
            }
        }

        // Dibujar Ruta
        if (startPoint != null && endPoint != null) {
            val pts = routePoints.ifEmpty { listOf(startPoint, endPoint) }
            val line = Polyline()
            line.setPoints(pts)
            line.color = android.graphics.Color.RED
            line.width = 10f
            map.overlays.add(line)
            
            map.overlays.add(Marker(map).apply { position = startPoint; title = "Origen" })
            map.overlays.add(Marker(map).apply { position = endPoint; title = "Destino" })
            
            if (routePoints.isNotEmpty()) {
                map.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(pts), true, 100)
            }
        }
        map.invalidate()
    }, modifier = modifier)
}

suspend fun fetchOsrmRoute(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
    return try {
        val url = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
        val conn = URL(url).openConnection() as HttpURLConnection
        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        val coords = json.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            GeoPoint(pt.getDouble(1), pt.getDouble(0))
        }
    } catch (e: Exception) {
        listOf(start, end)
    }
}
