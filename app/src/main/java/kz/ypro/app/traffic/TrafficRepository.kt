package kz.ypro.app.traffic

import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kz.ypro.core.AlmatyCorridors
import kz.ypro.core.TrafficModel
import kz.ypro.core.TrafficSnapshot

/**
 * Держит в памяти свежую скорость по городу, чтобы оценка заказа
 * не ждала сеть: замеры 2GIS обновляются в фоне заранее.
 */
object TrafficRepository {

    private const val REFRESH_INTERVAL_MS = 2 * 60 * 1000L

    private val snapshot = AtomicReference<TrafficSnapshot?>(null)

    fun snapshot(): TrafficSnapshot? = snapshot.get()

    fun currentSpeedKmh(): Double {
        val now = Calendar.getInstance()
        val day = now.get(Calendar.DAY_OF_WEEK)
        return TrafficModel.effectiveSpeedKmh(
            snapshot = snapshot.get(),
            nowMillis = now.timeInMillis,
            hourOfDay = now.get(Calendar.HOUR_OF_DAY),
            isWeekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY,
        )
    }

    /** Обновляет пробки, пока жив вызывающий scope. Без ключа 2GIS ничего не делает. */
    suspend fun keepFresh(apiKey: String) {
        if (apiKey.isBlank()) return
        while (true) {
            refresh(apiKey)
            delay(REFRESH_INTERVAL_MS)
        }
    }

    private suspend fun refresh(apiKey: String): Unit = coroutineScope {
        val samples = AlmatyCorridors.all
            .map { corridor -> async(Dispatchers.IO) { TwoGisClient.measure(apiKey, corridor) } }
            .awaitAll()
            .filterNotNull()
        // При сбое сети оставляем прошлый снимок: он сам устареет через TrafficModel.
        TrafficModel.snapshotOf(samples, System.currentTimeMillis())?.let { snapshot.set(it) }
    }
}
