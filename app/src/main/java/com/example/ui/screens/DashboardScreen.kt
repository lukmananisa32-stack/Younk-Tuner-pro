package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HardwareConnection
import com.example.data.model.HondaEcuModel
import com.example.data.model.LiveTelemetry
import com.example.ui.components.TachometerGauge
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.HondaRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    activeEcu: HondaEcuModel,
    revLimiterRpm: Int,
    telemetry: LiveTelemetry,
    connection: HardwareConnection,
    onToggleEngine: () -> Unit,
    onThrottleChange: (Int) -> Unit,
    onOpenConnectionSheet: () -> Unit,
    onOpenDatabase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active ECU Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenDatabase)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_ecu_banner),
                contentDescription = stringResource(R.string.cd_ecu_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ObsidianBg.copy(alpha = 0.92f), ObsidianBg.copy(alpha = 0.55f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = activeEcu.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeEcu.partNumber} • ${activeEcu.mcuArchitecture}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )
                    Text(
                        text = "${activeEcu.manufacturer} • ${activeEcu.flashSizeKb}KB Flash • Tap ganti motor",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    color = if (connection.isPhysicalDevice) EmeraldOk.copy(alpha = 0.2f) else TelemetryCyan.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (connection.isPhysicalDevice) EmeraldOk else TelemetryCyan),
                    modifier = Modifier.clickable(onClick = onOpenConnectionSheet)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Usb,
                            contentDescription = "Status Koneksi",
                            tint = if (connection.isPhysicalDevice) EmeraldOk else TelemetryCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (connection.isPhysicalDevice) "LIVE ECU" else "BENCH",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // MIL Alert Banner if DTC active
        if (telemetry.milActive) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HondaRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = HondaRed)
                    Text(
                        text = "Lampu MIL / Check Engine AKTIF! Buka tab Diagnosa untuk cek kode DTC.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Tachometer Gauge Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TachometerGauge(
                    rpm = telemetry.rpm,
                    maxRpm = maxOf(12000, revLimiterRpm + 1000),
                    redlineRpm = revLimiterRpm,
                    speedKmh = telemetry.speedKmh,
                    gear = telemetry.gear,
                    afr = telemetry.afr
                )

                // Interactive Bench Controls when no physical cable attached
                if (!connection.isPhysicalDevice) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleEngine,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (telemetry.engineRunning) HondaRed else EmeraldOk,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("engine_toggle_button")
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(if (telemetry.engineRunning) "MATIKAN" else "START")
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bukaan Gas (TPS)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    "${telemetry.tpsPercent}%",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TelemetryCyan
                                )
                            }
                            Slider(
                                value = telemetry.tpsPercent.toFloat(),
                                onValueChange = { onThrottleChange(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = HondaRed,
                                    activeTrackColor = HondaRed
                                ),
                                modifier = Modifier.testTag("tps_throttle_slider")
                            )
                        }
                    }
                }
            }
        }

        // 6-Sensor Telemetry Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryTile(
                icon = Icons.Filled.Speed,
                label = "TPS",
                value = "${telemetry.tpsPercent}%",
                accent = TelemetryCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Filled.Sensors,
                label = "MAP",
                value = "${telemetry.mapKpa} kPa",
                accent = EmeraldOk,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Filled.Bolt,
                label = "IGNITION",
                value = "${telemetry.ignitionAdvanceDeg}° BTDC",
                accent = AmberWarn,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryTile(
                icon = Icons.Filled.Thermostat,
                label = "ECT / IAT",
                value = "${telemetry.ectCelsius}° / ${telemetry.iatCelsius}°C",
                accent = if (telemetry.ectCelsius > 105) HondaRed else EmeraldOk,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Filled.Sensors,
                label = "O2 / INJ",
                value = "${telemetry.o2VoltageMv}mV • ${telemetry.injectorPulseWidthMs}ms",
                accent = TelemetryCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                icon = Icons.Filled.BatteryChargingFull,
                label = "AKI / VOLT",
                value = "${telemetry.batteryVoltage} V",
                accent = if (telemetry.batteryVoltage < 11.8) HondaRed else EmeraldOk,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonElevated),
        border = BorderStroke(1.dp, CarbonBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
