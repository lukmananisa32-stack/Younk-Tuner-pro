package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EcuSpecialFeatures
import com.example.data.model.FlashProgressState
import com.example.data.model.HardwareConnection
import com.example.data.model.HondaEcuModel
import com.example.data.model.SensorCalibrationData
import com.example.data.model.SmartkeyData
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
fun FlashSpecialScreen(
    activeEcu: HondaEcuModel,
    connection: HardwareConnection,
    checksumHex: String,
    flashState: FlashProgressState,
    calibration: SensorCalibrationData,
    specialFeatures: EcuSpecialFeatures,
    smartkey: SmartkeyData,
    onFlashEcu: (Boolean) -> Unit,
    onRecoveryUnbrick: () -> Unit,
    onResetTpsZero: () -> Unit,
    onCalibrateO2: (String, Int) -> Unit,
    onUpdateFeatures: (EcuSpecialFeatures) -> Unit,
    onReadSmartkey: () -> Unit,
    onRegisterFob: (String) -> Unit
) {
    var newFobInput by remember { mutableStateOf("528-941-777") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Flash & Recovery Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FlashOn, contentDescription = null, tint = HondaRed)
                        Text("Flash & Recovery ECU", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Text(
                        text = "${activeEcu.flashSizeKb}KB",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )
                }

                Text(
                    text = "Target: ${activeEcu.name} (${activeEcu.partNumber}) • CRC32: $checksumHex • Mode: ${if (connection.isPhysicalDevice) connection.deviceName else "Bench Bus"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                LinearProgressIndicator(
                    progress = { flashState.progressPercent / 100f },
                    color = if (flashState.completedSuccess) EmeraldOk else HondaRed,
                    trackColor = CarbonElevated,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = flashState.stageMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (flashState.completedSuccess) EmeraldOk else Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${flashState.progressPercent}% (${flashState.currentAddressHex})",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )
                }

                if (flashState.logs.isNotEmpty()) {
                    Surface(
                        color = ObsidianBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            flashState.logs.forEach { line ->
                                Text(line, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = EmeraldOk)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onFlashEcu(false) },
                        enabled = !flashState.isFlashing,
                        colors = ButtonDefaults.buttonColors(containerColor = HondaRed),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("flash_cal_button")
                    ) {
                        Text("Flash Cal (16 Blk)")
                    }
                    Button(
                        onClick = { onFlashEcu(true) },
                        enabled = !flashState.isFlashing,
                        colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("flash_full_button")
                    ) {
                        Text("Full ROM", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onRecoveryUnbrick,
                    enabled = !flashState.isFlashing,
                    border = BorderStroke(1.dp, AmberWarn),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("recovery_unbrick_button")
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = AmberWarn, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mode Recovery / Unbrick Bootloader (70ms Break)", color = AmberWarn)
                }
            }
        }

        // Sensor Utility Card (Reset TPS & Kalibrasi O2)
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = TelemetryCyan)
                    Text("Utilitas Sensor & Kalibrasi EEPROM", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }

                Text(
                    text = "TPS Tutup: ${calibration.tpsClosedVoltage}V • WOT: ${calibration.tpsWotVoltage}V • O2: ${calibration.o2Mode.uppercase()} (${calibration.o2SwitchPointMv}mV) • Sync: ${calibration.lastCalibratedTime}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetryCyan
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onResetTpsZero,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldOk, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("reset_tps_button")
                    ) {
                        Text("Reset TPS 0°", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val nextMode = if (calibration.o2Mode == "narrowband") "wideband" else "narrowband"
                            val nextMv = if (nextMode == "wideband") 2450 else 450
                            onCalibrateO2(nextMode, nextMv)
                        },
                        border = BorderStroke(1.dp, TelemetryCyan),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Toggle O2 Mode", color = TelemetryCyan)
                    }
                }
            }
        }

        // Bypass Flags Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = AmberWarn)
                    Text("Patch & Bypass Proteksi ECU", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }

                BypassSwitchRow(
                    title = "Off Immobilizer (Bypass HISS)",
                    checked = specialFeatures.offImmobilizer,
                    onCheckedChange = { onUpdateFeatures(specialFeatures.copy(offImmobilizer = it)) }
                )
                BypassSwitchRow(
                    title = "Off Switch Standar Samping",
                    checked = specialFeatures.offSideStand,
                    onCheckedChange = { onUpdateFeatures(specialFeatures.copy(offSideStand = it)) }
                )
                BypassSwitchRow(
                    title = "Off Closed-Loop Sensor O2 (Open Loop)",
                    checked = specialFeatures.offO2Sensor,
                    onCheckedChange = { onUpdateFeatures(specialFeatures.copy(offO2Sensor = it)) }
                )
                BypassSwitchRow(
                    title = "Off Limiter Suhu Mesin (ECT Cut)",
                    checked = specialFeatures.offEctCut,
                    onCheckedChange = { onUpdateFeatures(specialFeatures.copy(offEctCut = it)) }
                )
            }
        }

        // Smartkey / SCU Keyless Utility
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = HondaRed)
                    Text("Smartkey / SCU Honda Keyless", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Text(
                    text = "ECM ID: ${smartkey.ecmId} • SCU: ${smartkey.scuId}\nPIN Darurat: ${smartkey.emergencyPin} • Fob 1: ${smartkey.registeredFob1} • Fob 2: ${smartkey.registeredFob2}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = newFobInput,
                    onValueChange = { newFobInput = it },
                    label = { Text("Kode ID Remote Fob Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onReadSmartkey,
                        border = BorderStroke(1.dp, TelemetryCyan),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Baca SCU ID", color = TelemetryCyan)
                    }
                    Button(
                        onClick = { onRegisterFob(newFobInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = HondaRed),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Daftar Fob")
                    }
                }
            }
        }
    }
}

@Composable
private fun BypassSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HondaRed)
        )
    }
}
