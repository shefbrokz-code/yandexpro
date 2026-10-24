package kz.ypro.core

/**
 * Предложение заказа, распознанное с экрана водителя.
 * Поля, которых не было на экране, равны null и оцениваются по пробкам.
 */
data class OrderOffer(
    val priceTenge: Double,
    val tripKm: Double,
    val pickupKm: Double? = null,
    val pickupMinutes: Double? = null,
    val tripMinutes: Double? = null,
)
