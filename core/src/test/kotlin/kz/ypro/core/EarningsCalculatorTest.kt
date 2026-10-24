package kz.ypro.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarningsCalculatorTest {

    private val costs = CostSettings(
        serviceCommissionPct = 15.0,
        parkCommissionPct = 5.0,
        taxPct = 0.0,
        fixedFeePerOrder = 0.0,
        fuelPricePerLiter = 200.0,
        fuelLitersPer100Km = 10.0,
        wearPerKm = 0.0,
        overheadMinutes = 0.0,
        greenFromPerHour = 3000.0,
        yellowFromPerHour = 2000.0,
    )

    @Test
    fun `uses times from screen`() {
        val offer = OrderOffer(priceTenge = 2000.0, pickupKm = 2.0, pickupMinutes = 5.0, tripKm = 8.0, tripMinutes = 25.0)
        val result = EarningsCalculator.evaluate(offer, costs, speedKmh = 30.0)

        // 2000 − 20% = 1600; топливо 10 км × 0,1 л × 200 = 200; итого 1400 за 30 минут.
        assertEquals(400.0, result.deductionsTenge, 1e-9)
        assertEquals(200.0, result.carCostsTenge, 1e-9)
        assertEquals(1400.0, result.netTenge, 1e-9)
        assertEquals(2800.0, result.perHourTenge, 1e-9)
        assertEquals(Rating.YELLOW, result.rating)
        assertFalse(result.usedTrafficEstimate)
    }

    @Test
    fun `estimates missing times from traffic speed`() {
        val offer = OrderOffer(priceTenge = 3000.0, tripKm = 10.0)
        val result = EarningsCalculator.evaluate(offer, costs.copy(defaultPickupKm = 2.0), speedKmh = 24.0)

        // 12 км при 24 км/ч = 30 минут.
        assertEquals(30.0, result.totalMinutes, 1e-9)
        assertTrue(result.usedTrafficEstimate)
    }

    @Test
    fun `unknown destination uses assumed trip and tariff`() {
        val settings = costs.copy(assumedTripKm = 6.0, tariffBase = 400.0, tariffPerKm = 100.0, tariffPerMinute = 20.0)
        val offer = OrderOffer(priceTenge = 700.0, tripKm = null, pickupKm = 1.0, pickupMinutes = 3.0)
        val result = EarningsCalculator.evaluate(offer, settings, speedKmh = 24.0)

        // 6 км при 24 км/ч = 15 мин; по тарифу 400 + 600 + 300 = 1300 > 700 с экрана.
        assertEquals(1300.0, result.priceTenge, 1e-9)
        assertEquals(6.0, result.tripKm, 0.0)
        assertEquals(18.0, result.totalMinutes, 1e-9)
        assertFalse(result.destinationKnown)
    }

    @Test
    fun `unknown destination without tariff keeps screen price`() {
        val offer = OrderOffer(priceTenge = 700.0, tripKm = null)
        assertEquals(700.0, EarningsCalculator.evaluate(offer, costs, speedKmh = 24.0).priceTenge, 0.0)
    }

    @Test
    fun `heavy traffic turns order red`() {
        val offer = OrderOffer(priceTenge = 1500.0, pickupKm = 1.0, tripKm = 9.0)
        assertEquals(Rating.GREEN, EarningsCalculator.evaluate(offer, costs, speedKmh = 40.0).rating)
        assertEquals(Rating.RED, EarningsCalculator.evaluate(offer, costs, speedKmh = 15.0).rating)
    }
}
