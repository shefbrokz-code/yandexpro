package kz.ypro.core

/** Расходы водителя и пороги цвета плашки. Все суммы в тенге. */
data class CostSettings(
    val serviceCommissionPct: Double = 17.0,
    val parkCommissionPct: Double = 3.0,
    val taxPct: Double = 0.0,
    val fixedFeePerOrder: Double = 0.0,
    val fuelPricePerLiter: Double = 210.0,
    val fuelLitersPer100Km: Double = 8.0,
    val wearPerKm: Double = 15.0,
    /** Ожидание клиента, посадка, поиск парковки — время сверх движения. */
    val overheadMinutes: Double = 3.0,
    /** Подача, если на экране её не видно. */
    val defaultPickupKm: Double = 1.5,
    val greenFromPerHour: Double = 3500.0,
    val yellowFromPerHour: Double = 2500.0,
)
