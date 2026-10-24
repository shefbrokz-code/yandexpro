package kz.ypro.core

/**
 * Достаёт параметры заказа из текстов, собранных с экрана.
 *
 * Расстояния и время берутся в порядке появления на экране: если их два,
 * первое относится к подаче, второе — к поездке. Если расстояние одно,
 * считаем его расстоянием поездки.
 *
 * Если на экране есть признак заказа без конечного адреса, всё найденное
 * относится к подаче, а поездка остаётся неизвестной.
 */
object OrderParser {

    private const val SPACES = " \u00A0\u202F"

    // \s в Java не включает неразрывные пробелы, а приложения ставят их между числом и единицей.
    private const val GAP = "[\\s\u00A0\u202F]*"

    private val priceRegex = Regex(
        "(?<![\\d+])(\\d{1,3}(?:[$SPACES]\\d{3})+|\\d+)(?:[.,]\\d{1,2})?$GAP(?:₸|〒|тг|тенге|kzt)",
        RegexOption.IGNORE_CASE,
    )

    // Расстояние в километрах или метрах ("м" не путаем с "мин").
    private val distanceRegex = Regex(
        "(\\d+(?:[.,]\\d+)?)$GAP(км|м)(?!\\p{L})",
        RegexOption.IGNORE_CASE,
    )

    // "1 ч 15 мин", "25 мин", "1 ч".
    private val durationRegex = Regex(
        "(?:(\\d+)${GAP}ч(?:ас\\p{L}*)?$GAP)?(\\d+)${GAP}мин|(\\d+)${GAP}ч(?:ас\\p{L}*)?(?!\\p{L})",
        RegexOption.IGNORE_CASE,
    )

    /**
     * @param texts тексты элементов экрана в порядке обхода.
     * @param requiredKeywords экран считается заказом, только если на нём есть
     *   хотя бы одно из этих слов (например, кнопка «Принять»). Пустой список — без проверки.
     * @param noDestinationKeywords слова, по которым видно, что конечный адрес не указан.
     * @return заказ или null, если на экране нет предложения заказа.
     */
    fun parse(
        texts: List<String>,
        requiredKeywords: List<String> = emptyList(),
        noDestinationKeywords: List<String> = emptyList(),
    ): OrderOffer? {
        val screen = texts.joinToString("\n")
        if (requiredKeywords.isNotEmpty() && requiredKeywords.none { screen.contains(it, ignoreCase = true) }) {
            return null
        }

        val price = priceRegex.findAll(screen)
            .mapNotNull { it.groupValues[1].filterNot { c -> c in SPACES }.toDoubleOrNull() }
            .maxOrNull()
            ?: return null

        val distances = distanceRegex.findAll(screen).map { match ->
            val value = match.groupValues[1].replace(',', '.').toDouble()
            if (match.groupValues[2].equals("м", ignoreCase = true)) value / 1000 else value
        }.toList()

        val durations = durationRegex.findAll(screen).map { match ->
            val (hours, minutes, onlyHours) = match.destructured
            when {
                onlyHours.isNotEmpty() -> onlyHours.toDouble() * 60
                else -> (hours.toDoubleOrNull() ?: 0.0) * 60 + minutes.toDouble()
            }
        }.toList()

        if (noDestinationKeywords.any { screen.contains(it, ignoreCase = true) }) {
            return OrderOffer(
                priceTenge = price,
                tripKm = null,
                pickupKm = distances.firstOrNull(),
                pickupMinutes = durations.firstOrNull(),
            )
        }
        if (distances.isEmpty()) return null

        return if (distances.size >= 2) {
            OrderOffer(
                priceTenge = price,
                pickupKm = distances[0],
                tripKm = distances[1],
                pickupMinutes = durations.getOrNull(0),
                tripMinutes = durations.getOrNull(1),
            )
        } else {
            OrderOffer(
                priceTenge = price,
                tripKm = distances[0],
                tripMinutes = durations.firstOrNull(),
            )
        }
    }
}
