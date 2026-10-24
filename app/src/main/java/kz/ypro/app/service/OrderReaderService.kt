package kz.ypro.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kz.ypro.app.BuildConfig
import kz.ypro.app.data.SettingsStore
import kz.ypro.app.overlay.OverlayBanner
import kz.ypro.app.traffic.TrafficRepository
import kz.ypro.core.EarningsCalculator
import kz.ypro.core.OrderOffer
import kz.ypro.core.OrderParser

/**
 * Следит за экраном водительского приложения и, когда появляется
 * предложение заказа, показывает сверху плашку с заработком в час.
 */
class OrderReaderService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { scanScreen() }

    private lateinit var settings: SettingsStore
    private lateinit var banner: OverlayBanner
    private var shownOffer: OrderOffer? = null
    private var lastDump: String? = null

    override fun onServiceConnected() {
        settings = SettingsStore(this)
        banner = OverlayBanner(this)
        serviceInfo = serviceInfo.apply { packageNames = settings.targetPackages().toTypedArray() }
        scope.launch { TrafficRepository.keepFresh(BuildConfig.TWOGIS_API_KEY) }
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Пока карточка заказа рисуется, событий приходит пачка — сканируем один раз после неё.
        mainHandler.removeCallbacks(scanRunnable)
        mainHandler.postDelayed(scanRunnable, SCAN_DEBOUNCE_MS)
    }

    private fun scanScreen() {
        val root = rootInActiveWindow ?: return
        val texts = ScreenText.collect(root)
        if (settings.debugCapture()) saveDump(texts)

        val offer = OrderParser.parse(texts, settings.offerKeywords(), settings.noDestinationKeywords())
        if (offer == null) {
            if (shownOffer != null) {
                banner.hide()
                shownOffer = null
            }
            return
        }
        if (offer == shownOffer) return
        shownOffer = offer

        val evaluation = EarningsCalculator.evaluate(offer, settings.costSettings(), TrafficRepository.currentSpeedKmh())
        banner.show(evaluation)
    }

    private fun saveDump(texts: List<String>) {
        val dump = texts.joinToString("\n")
        if (dump == lastDump) return
        lastDump = dump
        settings.saveScreenDump(dump)
    }

    override fun onInterrupt() {
        banner.hide()
    }

    override fun onDestroy() {
        isRunning = false
        mainHandler.removeCallbacks(scanRunnable)
        if (::banner.isInitialized) banner.hide()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val SCAN_DEBOUNCE_MS = 60L

        @Volatile
        var isRunning = false
            private set
    }
}
