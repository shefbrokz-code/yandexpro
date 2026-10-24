package kz.ypro.core

/** Замер одного маршрута с учётом пробок. */
data class RouteSample(val distanceMeters: Double, val durationSeconds: Double)

data class TrafficSnapshot(val averageSpeedKmh: Double, val sampleCount: Int, val fetchedAtMillis: Long)

object TrafficModel {

    /** Снимок пробок старше этого считается устаревшим. */
    const val MAX_SNAPSHOT_AGE_MILLIS = 10 * 60 * 1000L

    private const val MIN_SPEED_KMH = 5.0
    private const val MAX_SPEED_KMH = 80.0

    fun snapshotOf(samples: List<RouteSample>, nowMillis: Long): TrafficSnapshot? {
        val valid = samples.filter { it.distanceMeters > 0 && it.durationSeconds > 0 }
        if (valid.isEmpty()) return null
        val km = valid.sumOf { it.distanceMeters } / 1000
        val hours = valid.sumOf { it.durationSeconds } / 3600
        val speed = (km / hours).coerceIn(MIN_SPEED_KMH, MAX_SPEED_KMH)
        return TrafficSnapshot(speed, valid.size, nowMillis)
    }

    /** Свежий снимок 2GIS, а без него — типичная скорость Алматы на этот час. */
    fun effectiveSpeedKmh(snapshot: TrafficSnapshot?, nowMillis: Long, hourOfDay: Int, isWeekend: Boolean): Double {
        if (snapshot != null && nowMillis - snapshot.fetchedAtMillis <= MAX_SNAPSHOT_AGE_MILLIS) {
            return snapshot.averageSpeedKmh
        }
        return typicalSpeedKmh(hourOfDay, isWeekend)
    }

    fun typicalSpeedKmh(hourOfDay: Int, isWeekend: Boolean): Double = if (isWeekend) {
        when (hourOfDay) {
            in 0..7 -> 40.0
            in 8..11 -> 32.0
            in 12..20 -> 26.0
            else -> 32.0
        }
    } else {
        when (hourOfDay) {
            in 0..6 -> 40.0
            7 -> 28.0
            in 8..9 -> 18.0
            in 10..16 -> 25.0
            in 17..19 -> 17.0
            in 20..21 -> 26.0
            else -> 35.0
        }
    }
}
