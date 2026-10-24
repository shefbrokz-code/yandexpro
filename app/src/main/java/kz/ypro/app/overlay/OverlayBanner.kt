package kz.ypro.app.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kz.ypro.core.Evaluation
import kz.ypro.core.Rating

/**
 * Цветная плашка поверх экрана. Окно типа accessibility overlay
 * не требует отдельного разрешения и не перехватывает нажатия,
 * так что кнопка «Принять» под ней остаётся доступной.
 */
class OverlayBanner(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hide() }
    private val numbers = NumberFormat.getIntegerInstance(RU_KZ)
    private var view: TextView? = null

    fun show(evaluation: Evaluation) {
        val banner = view ?: createView().also {
            windowManager.addView(it, layoutParams())
            view = it
        }
        val (background, textColor) = colorsOf(evaluation.rating)
        (banner.background as GradientDrawable).setColor(background)
        banner.setTextColor(textColor)
        banner.text = format(evaluation)

        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, AUTO_HIDE_MS)
    }

    fun hide() {
        handler.removeCallbacks(hideRunnable)
        view?.let(windowManager::removeView)
        view = null
    }

    private fun format(e: Evaluation): String {
        val estimate = if (e.usedTrafficEstimate) " (пробки)" else ""
        return "${numbers.format(e.perHourTenge.roundToInt())} ₸/ч\n" +
            "чистыми ${numbers.format(e.netTenge.roundToInt())} ₸ · " +
            "${e.totalMinutes.roundToInt()} мин$estimate · " +
            String.format(RU_KZ, "%.1f км", e.totalKm)
    }

    private fun createView() = TextView(service).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        gravity = Gravity.CENTER
        val padding = dp(12)
        setPadding(padding * 2, padding, padding * 2, padding)
        background = GradientDrawable().apply { cornerRadius = dp(16).toFloat() }
        elevation = dp(6).toFloat()
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = dp(40)
    }

    private fun colorsOf(rating: Rating): Pair<Int, Int> = when (rating) {
        Rating.GREEN -> Color.rgb(0x2E, 0x7D, 0x32) to Color.WHITE
        Rating.YELLOW -> Color.rgb(0xF9, 0xA8, 0x25) to Color.BLACK
        Rating.RED -> Color.rgb(0xC6, 0x28, 0x28) to Color.WHITE
    }

    private fun dp(value: Int) = (value * service.resources.displayMetrics.density).roundToInt()

    private companion object {
        const val AUTO_HIDE_MS = 15_000L
        val RU_KZ: Locale = Locale.forLanguageTag("ru-KZ")
    }
}
