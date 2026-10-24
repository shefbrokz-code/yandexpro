package kz.ypro.core

enum class Rating { GREEN, YELLOW, RED }

data class Evaluation(
    val perHourTenge: Double,
    val netTenge: Double,
    val deductionsTenge: Double,
    val carCostsTenge: Double,
    val totalMinutes: Double,
    val totalKm: Double,
    val rating: Rating,
    /** true, если хотя бы одно время на экране не нашлось и взято из пробок. */
    val usedTrafficEstimate: Boolean,
)

object EarningsCalculator {

    private const val MIN_TOTAL_MINUTES = 1.0

    /** @param speedKmh текущая средняя скорость по городу с учётом пробок. */
    fun evaluate(offer: OrderOffer, costs: CostSettings, speedKmh: Double): Evaluation {
        val pickupKm = offer.pickupKm ?: costs.defaultPickupKm
        val pickupMinutes = offer.pickupMinutes ?: minutesAt(pickupKm, speedKmh)
        val tripMinutes = offer.tripMinutes ?: minutesAt(offer.tripKm, speedKmh)
        val totalMinutes = (pickupMinutes + tripMinutes + costs.overheadMinutes).coerceAtLeast(MIN_TOTAL_MINUTES)
        val totalKm = pickupKm + offer.tripKm

        val percent = costs.serviceCommissionPct + costs.parkCommissionPct + costs.taxPct
        val deductions = offer.priceTenge * percent / 100 + costs.fixedFeePerOrder
        val fuel = totalKm * costs.fuelLitersPer100Km / 100 * costs.fuelPricePerLiter
        val carCosts = fuel + totalKm * costs.wearPerKm
        val net = offer.priceTenge - deductions - carCosts
        val perHour = net / (totalMinutes / 60)

        return Evaluation(
            perHourTenge = perHour,
            netTenge = net,
            deductionsTenge = deductions,
            carCostsTenge = carCosts,
            totalMinutes = totalMinutes,
            totalKm = totalKm,
            rating = when {
                perHour >= costs.greenFromPerHour -> Rating.GREEN
                perHour >= costs.yellowFromPerHour -> Rating.YELLOW
                else -> Rating.RED
            },
            usedTrafficEstimate = offer.pickupMinutes == null || offer.tripMinutes == null,
        )
    }

    private fun minutesAt(km: Double, speedKmh: Double) = km / speedKmh * 60
}
