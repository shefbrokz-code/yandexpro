package kz.ypro.app.data

import android.content.Context
import kz.ypro.core.CostSettings

/** Числовые настройки расходов: ключ в SharedPreferences и подпись в интерфейсе. */
enum class CostField(val key: String, val label: String) {
    SERVICE_COMMISSION("service_commission_pct", "Комиссия сервиса, %"),
    PARK_COMMISSION("park_commission_pct", "Комиссия парка, %"),
    TAX("tax_pct", "Налог, %"),
    FIXED_FEE("fixed_fee", "Фикс. списание за заказ, ₸"),
    FUEL_PRICE("fuel_price", "Цена топлива, ₸/л"),
    FUEL_CONSUMPTION("fuel_consumption", "Расход, л/100 км"),
    WEAR("wear_per_km", "Износ авто, ₸/км"),
    OVERHEAD("overhead_minutes", "Ожидание и посадка, мин"),
    DEFAULT_PICKUP("default_pickup_km", "Подача, если не видна, км"),
    GREEN_FROM("green_from", "Зелёный от, ₸/ч"),
    YELLOW_FROM("yellow_from", "Жёлтый от, ₸/ч"),
}

fun CostSettings.valueOf(field: CostField): Double = when (field) {
    CostField.SERVICE_COMMISSION -> serviceCommissionPct
    CostField.PARK_COMMISSION -> parkCommissionPct
    CostField.TAX -> taxPct
    CostField.FIXED_FEE -> fixedFeePerOrder
    CostField.FUEL_PRICE -> fuelPricePerLiter
    CostField.FUEL_CONSUMPTION -> fuelLitersPer100Km
    CostField.WEAR -> wearPerKm
    CostField.OVERHEAD -> overheadMinutes
    CostField.DEFAULT_PICKUP -> defaultPickupKm
    CostField.GREEN_FROM -> greenFromPerHour
    CostField.YELLOW_FROM -> yellowFromPerHour
}

fun CostSettings.with(field: CostField, value: Double): CostSettings = when (field) {
    CostField.SERVICE_COMMISSION -> copy(serviceCommissionPct = value)
    CostField.PARK_COMMISSION -> copy(parkCommissionPct = value)
    CostField.TAX -> copy(taxPct = value)
    CostField.FIXED_FEE -> copy(fixedFeePerOrder = value)
    CostField.FUEL_PRICE -> copy(fuelPricePerLiter = value)
    CostField.FUEL_CONSUMPTION -> copy(fuelLitersPer100Km = value)
    CostField.WEAR -> copy(wearPerKm = value)
    CostField.OVERHEAD -> copy(overheadMinutes = value)
    CostField.DEFAULT_PICKUP -> copy(defaultPickupKm = value)
    CostField.GREEN_FROM -> copy(greenFromPerHour = value)
    CostField.YELLOW_FROM -> copy(yellowFromPerHour = value)
}

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("ypro_settings", Context.MODE_PRIVATE)

    fun costSettings(): CostSettings = CostField.entries.fold(CostSettings()) { settings, field ->
        val stored = prefs.getString(field.key, null)?.toDoubleOrNull()
        if (stored == null) settings else settings.with(field, stored)
    }

    fun saveCostSettings(settings: CostSettings) {
        prefs.edit().apply {
            CostField.entries.forEach { putString(it.key, settings.valueOf(it).toString()) }
        }.apply()
    }

    /** Пакеты водительских приложений, экран которых читаем. */
    fun targetPackages(): List<String> = splitList(prefs.getString(KEY_PACKAGES, null) ?: DEFAULT_PACKAGES)

    /** Слова, по которым экран считается предложением заказа. */
    fun offerKeywords(): List<String> = splitList(prefs.getString(KEY_KEYWORDS, null) ?: DEFAULT_KEYWORDS)

    fun saveScreenRules(packages: String, keywords: String) {
        prefs.edit().putString(KEY_PACKAGES, packages).putString(KEY_KEYWORDS, keywords).apply()
    }

    fun debugCapture(): Boolean = prefs.getBoolean(KEY_DEBUG_CAPTURE, false)

    fun setDebugCapture(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBUG_CAPTURE, enabled).apply()
    }

    /** Последний снятый текст экрана — по нему настраивается распознавание заказа. */
    fun lastScreenDump(): String = prefs.getString(KEY_LAST_DUMP, "").orEmpty()

    fun saveScreenDump(dump: String) {
        prefs.edit().putString(KEY_LAST_DUMP, dump).apply()
    }

    private fun splitList(value: String) = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        const val DEFAULT_PACKAGES = "ru.yandex.taximeter"
        const val DEFAULT_KEYWORDS = "Принять"

        private const val KEY_PACKAGES = "target_packages"
        private const val KEY_KEYWORDS = "offer_keywords"
        private const val KEY_DEBUG_CAPTURE = "debug_capture"
        private const val KEY_LAST_DUMP = "last_screen_dump"
    }
}
