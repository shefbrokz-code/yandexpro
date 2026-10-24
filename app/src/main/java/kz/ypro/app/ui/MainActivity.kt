package kz.ypro.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kz.ypro.app.BuildConfig
import kz.ypro.app.data.CostField
import kz.ypro.app.data.SettingsStore
import kz.ypro.app.data.valueOf
import kz.ypro.app.data.with
import kz.ypro.app.service.OrderReaderService
import kz.ypro.app.traffic.TrafficRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    MainScreen(store)
                }
            }
        }
    }
}

private const val STATUS_REFRESH_MS = 3_000L

@Composable
private fun MainScreen(store: SettingsStore) {
    val context = LocalContext.current
    var serviceRunning by remember { mutableStateOf(OrderReaderService.isRunning) }
    var trafficLine by remember { mutableStateOf(trafficStatus()) }
    LaunchedEffect(Unit) {
        while (true) {
            serviceRunning = OrderReaderService.isRunning
            trafficLine = trafficStatus()
            delay(STATUS_REFRESH_MS)
        }
    }

    val costs = remember { store.costSettings() }
    val fields = remember { CostField.entries.map { it to formatNumber(costs.valueOf(it)) }.toMutableStateMap() }
    var packages by remember { mutableStateOf(store.targetPackages().joinToString(", ")) }
    var keywords by remember { mutableStateOf(store.offerKeywords().joinToString(", ")) }
    var noDestination by remember { mutableStateOf(store.noDestinationKeywords().joinToString(", ")) }
    var debugCapture by remember { mutableStateOf(store.debugCapture()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Y-Pro", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (serviceRunning) "Чтение экрана включено" else "Чтение экрана выключено")
                Text(trafficLine, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                    Text("Открыть спец. возможности")
                }
            }
        }

        Text("Расходы и пороги", style = MaterialTheme.typography.titleMedium)
        CostField.entries.forEach { field ->
            OutlinedTextField(
                value = fields[field].orEmpty(),
                onValueChange = { fields[field] = it },
                label = { Text(field.label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text("Распознавание заказа", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = packages,
            onValueChange = { packages = it },
            label = { Text("Приложения (пакеты через запятую)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = keywords,
            onValueChange = { keywords = it },
            label = { Text("Слова на экране заказа") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = noDestination,
            onValueChange = { noDestination = it },
            label = { Text("Признаки заказа без адреса") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                val updated = CostField.entries.fold(store.costSettings()) { settings, field ->
                    val value = fields[field]?.replace(',', '.')?.toDoubleOrNull()
                    if (value == null) settings else settings.with(field, value)
                }
                store.saveCostSettings(updated)
                store.saveScreenRules(packages, keywords, noDestination)
                Toast.makeText(context, "Сохранено. Список приложений применится после перезапуска сервиса.", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }

        Text("Отладка", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сохранять текст экрана", Modifier.weight(1f))
            Switch(
                checked = debugCapture,
                onCheckedChange = {
                    debugCapture = it
                    store.setDebugCapture(it)
                },
            )
        }
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Y-Pro screen", store.lastScreenDump()))
                Toast.makeText(context, "Текст экрана скопирован", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Скопировать последний экран")
        }
    }
}

private fun trafficStatus(): String {
    val speed = TrafficRepository.currentSpeedKmh().roundToInt()
    val snapshot = TrafficRepository.snapshot()
    return when {
        BuildConfig.TWOGIS_API_KEY.isBlank() -> "Ключ 2GIS не задан — скорость по времени суток: $speed км/ч"
        snapshot == null -> "Пробки ещё не загружены — по времени суток: $speed км/ч"
        else -> {
            val ageMinutes = (System.currentTimeMillis() - snapshot.fetchedAtMillis) / 60_000
            "Пробки 2GIS: $speed км/ч (замер $ageMinutes мин назад)"
        }
    }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
