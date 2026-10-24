package kz.ypro.app.traffic

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kz.ypro.core.Corridor
import kz.ypro.core.GeoPoint
import kz.ypro.core.RouteSample
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Время проезда по маршруту с учётом текущих пробок через 2GIS Routing API. */
object TwoGisClient {

    private const val ENDPOINT = "https://routing.api.2gis.com/routing/7.0.0/global"
    private const val TIMEOUT_MS = 5_000

    /** @return замер маршрута или null, если 2GIS не ответил. */
    fun measure(apiKey: String, corridor: Corridor): RouteSample? {
        val body = JSONObject()
            .put("points", JSONArray().put(point(corridor.from)).put(point(corridor.to)))
            .put("transport", "driving")
            .put("route_mode", "fastest")
            .put("traffic_mode", "jam")
            .put("output", "summary")

        val connection = URL("$ENDPOINT?key=${URLEncoder.encode(apiKey, "UTF-8")}").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val route = response.optJSONArray("result")?.optJSONObject(0) ?: return null
            val distance = route.optDouble("total_distance", Double.NaN)
            val duration = route.optDouble("total_duration", Double.NaN)
            if (distance.isNaN() || duration.isNaN()) null else RouteSample(distance, duration)
        } catch (e: IOException) {
            null
        } catch (e: JSONException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun point(p: GeoPoint) = JSONObject().put("type", "stop").put("lat", p.lat).put("lon", p.lon)
}
