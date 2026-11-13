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
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DtcRecord
import com.example.data.model.HondaEcuModel
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
fun DiagnosaScreen(
    activeEcu: HondaEcuModel,
    dtcs: List<DtcRecord>,
    actuatorResult: String?,
    onScanDtcs: (Boolean) -> Unit,
    onClearDtcs: () -> Unit,
    onTestActuator: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MIL Status Header
        Surface(
            color = if (dtcs.isEmpty()) EmeraldOk.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (dtcs.isEmpty()) EmeraldOk else HondaRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (dtcs.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (dtcs.isEmpty()) EmeraldOk else HondaRed,
                    modifier = Modifier.size(30.dp)
                )
                Column {
                    Text(
                        text = if (dtcs.isEmpty()) "Sistem PGM-FI Normal (MIL OFF)" else "${dtcs.size} Kode Kerusakan Aktif (MIL ON)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ECU: ${activeEcu.name} (${activeEcu.partNumber})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Scan & Clear DTC Actions
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onScanDtcs(false) },
                colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianBg),
                modifier = Modifier.weight(1f).height(48.dp).testTag("scan_dtc_button")
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan DTC", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClearDtcs,
                colors = ButtonDefaults.buttonColors(containerColor = HondaRed),
                modifier = Modifier.weight(1f).height(48.dp).testTag("clear_dtc_button")
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset MIL")
            }
        }

        OutlinedButton(
            onClick = { onScanDtcs(true) },
            border = BorderStroke(1.dp, AmberWarn),
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("simulate_fault_button")
        ) {
            Text("Uji Deteksi Sensor Fault (Simulasi Bengkel)", color = AmberWarn)
        }

        // DTC List
        if (dtcs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldOk, modifier = Modifier.size(36.dp))
                    Text("Tidak ada kode error DTC tersimpan di ECM.", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Tekan 'Scan DTC' saat kontak motor ON untuk membaca memori ECM.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            dtcs.forEach { dtc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    border = BorderStroke(1.dp, HondaRed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${dtc.code} • Kedipan MIL ${dtc.blinkCode}x",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = HondaRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(dtc.timestamp, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text(dtc.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(dtc.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Surface(
                            color = CarbonElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Solusi Mekanik: ${dtc.remedy}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldOk,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Actuator Test Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.BuildCircle, contentDescription = null, tint = TelemetryCyan)
                    Text("Uji Aktuator Aktif (Output ECM)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                if (actuatorResult != null) {
                    Surface(
                        color = CarbonElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldOk),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(actuatorResult, style = MaterialTheme.typography.bodyMedium, color = EmeraldOk, modifier = Modifier.padding(10.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onTestActuator("Fuel Pump Relay") },
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Fuel Pump", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onTestActuator("Koil Pengapian") },
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Koil Ign", color = Color.White)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onTestActuator("Injektor BBM") },
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Injektor", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { onTestActuator("Motor IACV") },
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("IACV Valve", color = Color.White)
                    }
                }
            }
        }
    }
}
