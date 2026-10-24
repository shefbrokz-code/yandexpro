package kz.ypro.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrafficModelTest {

    @Test
    fun `average speed weights by distance`() {
        val snapshot = TrafficModel.snapshotOf(
            listOf(RouteSample(10_000.0, 1_800.0), RouteSample(5_000.0, 900.0), RouteSample(0.0, 0.0)),
            nowMillis = 0,
        )!!
        assertEquals(20.0, snapshot.averageSpeedKmh, 1e-9)
        assertEquals(2, snapshot.sampleCount)
    }

    @Test
    fun `no valid samples gives no snapshot`() {
        assertNull(TrafficModel.snapshotOf(emptyList(), nowMillis = 0))
    }

    @Test
    fun `stale snapshot falls back to typical speed`() {
        val snapshot = TrafficSnapshot(averageSpeedKmh = 50.0, sampleCount = 3, fetchedAtMillis = 0)
        assertEquals(50.0, TrafficModel.effectiveSpeedKmh(snapshot, 60_000, hourOfDay = 18, isWeekend = false), 0.0)
        assertEquals(
            TrafficModel.typicalSpeedKmh(18, isWeekend = false),
            TrafficModel.effectiveSpeedKmh(snapshot, TrafficModel.MAX_SNAPSHOT_AGE_MILLIS + 1, 18, false),
            0.0,
        )
    }
}
