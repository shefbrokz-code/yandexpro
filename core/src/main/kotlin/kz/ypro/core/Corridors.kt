package kz.ypro.core

data class GeoPoint(val lat: Double, val lon: Double)

data class Corridor(val name: String, val from: GeoPoint, val to: GeoPoint)

/** Основные магистрали Алматы: по ним замеряем среднюю скорость с пробками. */
object AlmatyCorridors {
    val all = listOf(
        Corridor("Аль-Фараби", GeoPoint(43.2085, 76.8650), GeoPoint(43.2245, 76.9560)),
        Corridor("Абая", GeoPoint(43.2405, 76.8700), GeoPoint(43.2420, 76.9500)),
        Corridor("Райымбека", GeoPoint(43.2705, 76.8500), GeoPoint(43.2690, 76.9500)),
        Corridor("Саина", GeoPoint(43.2100, 76.8430), GeoPoint(43.2880, 76.8420)),
        Corridor("Сейфуллина", GeoPoint(43.2950, 76.9320), GeoPoint(43.2370, 76.9330)),
        Corridor("Достык", GeoPoint(43.2600, 76.9560), GeoPoint(43.2200, 76.9590)),
        Corridor("Аэропорт — центр", GeoPoint(43.3460, 77.0100), GeoPoint(43.2560, 76.9300)),
    )
}
