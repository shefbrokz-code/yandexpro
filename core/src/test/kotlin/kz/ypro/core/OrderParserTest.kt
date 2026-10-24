package kz.ypro.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderParserTest {

    @Test
    fun `parses pickup and trip in screen order`() {
        val offer = OrderParser.parse(
            listOf("1 450 ₸", "4 мин · 1,2 км", "ул. Абая, 10", "25 мин · 12,5 км", "Принять"),
            listOf("Принять"),
        )!!
        assertEquals(1450.0, offer.priceTenge, 0.0)
        assertEquals(1.2, offer.pickupKm!!, 1e-9)
        assertEquals(4.0, offer.pickupMinutes!!, 0.0)
        assertEquals(12.5, offer.tripKm, 1e-9)
        assertEquals(25.0, offer.tripMinutes!!, 0.0)
    }

    @Test
    fun `single distance is the trip`() {
        val offer = OrderParser.parse(listOf("2 100 тг", "18 км"))!!
        assertEquals(2100.0, offer.priceTenge, 0.0)
        assertEquals(18.0, offer.tripKm, 0.0)
        assertNull(offer.pickupKm)
        assertNull(offer.tripMinutes)
    }

    @Test
    fun `meters and hours are converted`() {
        val offer = OrderParser.parse(listOf("6 800 ₸", "2 мин 800 м", "1 ч 10 мин 45 км"))!!
        assertEquals(0.8, offer.pickupKm!!, 1e-9)
        assertEquals(45.0, offer.tripKm, 0.0)
        assertEquals(70.0, offer.tripMinutes!!, 0.0)
    }

    @Test
    fun `bonus with plus sign is not taken as price`() {
        val offer = OrderParser.parse(listOf("+3 000 ₸ за серию", "1 200 ₸", "10 км"))!!
        assertEquals(1200.0, offer.priceTenge, 0.0)
    }

    @Test
    fun `non-breaking spaces in price`() {
        val offer = OrderParser.parse(listOf("1\u00A0350\u00A0₸", "2\u202Fмин 7\u00A0км"))!!
        assertEquals(1350.0, offer.priceTenge, 0.0)
        assertEquals(7.0, offer.tripKm, 0.0)
        assertEquals(2.0, offer.tripMinutes!!, 0.0)
    }

    @Test
    fun `screen without required keyword is ignored`() {
        assertNull(OrderParser.parse(listOf("Баланс 12 500 ₸", "Пробег 140 км"), listOf("Принять")))
    }

    @Test
    fun `screen without price or distance is ignored`() {
        assertNull(OrderParser.parse(listOf("12 км", "Принять")))
        assertNull(OrderParser.parse(listOf("1 200 ₸", "Принять")))
    }
}
