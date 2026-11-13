package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiTuningRecommendation
import com.example.data.model.HondaEcuModel
import com.example.data.model.TuningStrategy
import com.example.data.model.TuningTables
import com.example.ui.components.Curve2DGraph
import com.example.ui.components.Surface3DGraph
import com.example.ui.components.heatMapColor
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
import com.example.ui.viewmodel.AiUiState

@Composable
fun RemapScreen(
    activeEcu: HondaEcuModel,
    tables: TuningTables,
    checksumHex: String,
    aiState: AiUiState,
    onApplyStrategy: (TuningStrategy) -> Unit,
    onAdjustCell: (Boolean, Int, Int, Double) -> Unit,
    onSmoothTable: (Boolean) -> Unit,
    onUpdateLimits: (Int, Int, Double) -> Unit,
    onRunAiTuning: (String, String, String) -> Unit,
    onApplyAiRecommendation: (AiTuningRecommendation) -> Unit,
    onSaveMapCloud: (String, String) -> Unit,
    onBuildRomBytes: () -> ByteArray,
    onBuildXdfXml: () -> String,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var isFuelTab by remember { mutableStateOf(true) }
    var viewMode by remember { mutableIntStateOf(0) } // 0=Grid, 1=2D, 2=3D
    var selectedRow by remember { mutableIntStateOf(10) }
    var selectedCol by remember { mutableIntStateOf(8) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var mapTitle by remember(activeEcu) { mutableStateOf("${activeEcu.name.take(20)} Tune") }
    var mapNotes by remember { mutableStateOf("") }

    var engineMods by remember { mutableStateOf("Knalpot racing, filter udara open, per klep racing") }
    var fuelOctane by remember { mutableStateOf("RON 92 / Pertamax") }
    var tuningGoal by remember { mutableStateOf("Tarikan responsif bawah-tengah & aman touring") }

    val binLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onBuildRomBytes())
                }
                onShowToast("File ROM .BIN (CRC32: $checksumHex) berhasil diekspor!")
            } catch (e: Exception) {
                onShowToast("Gagal ekspor .BIN: ${e.localizedMessage}")
            }
        }
    }

    val xdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onBuildXdfXml().toByteArray())
                }
                onShowToast("Definisi TunerPro .XDF berhasil diekspor!")
            } catch (e: Exception) {
                onShowToast("Gagal ekspor .XDF: ${e.localizedMessage}")
            }
        }
    }

    val matrix = if (isFuelTab) tables.fuelVe else tables.ignitionAdvance
    val minRange = if (isFuelTab) 50.0 else 4.0
    val maxRange = if (isFuelTab) 130.0 else 42.0
    val stepDelta = if (isFuelTab) 1.0 else 0.5
    val currentCellVal = matrix.getOrNull(selectedRow)?.getOrNull(selectedCol) ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Strategy Presets & CRC32 Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-Mapping Presets", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Surface(
                color = EmeraldOk.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, EmeraldOk)
            ) {
                Text(
                    text = "CRC32: $checksumHex",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldOk,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TuningStrategy.entries.forEach { strat ->
                val selected = tables.activeStrategy == strat
                FilterChip(
                    selected = selected,
                    onClick = { onApplyStrategy(strat) },
                    label = { Text(strat.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HondaRed,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("strategy_${strat.name.lowercase()}")
                )
            }
        }

        // Table Type + View Switcher
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isFuelTab = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFuelTab) HondaRed else CarbonElevated
                        ),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("tab_fuel_ve")
                    ) {
                        Text("Fuel VE (%)", color = Color.White)
                    }
                    Button(
                        onClick = { isFuelTab = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isFuelTab) TelemetryCyan else CarbonElevated
                        ),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("tab_ignition")
                    ) {
                        Text("Ignition (°)", color = if (!isFuelTab) ObsidianBg else Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // 3-Mode Graph Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "Tabel 16x16", 1 to "Kurva 2D", 2 to "3D Surface").forEach { (idx, title) ->
                        OutlinedButton(
                            onClick = { viewMode = idx },
                            border = BorderStroke(1.dp, if (viewMode == idx) TelemetryCyan else CarbonBorder),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(title, fontSize = 11.sp, color = if (viewMode == idx) TelemetryCyan else Color.White)
                        }
                    }
                }

                when (viewMode) {
                    0 -> {
                        // 16x16 Interactive Heatmap Matrix
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Header RPM Row
                            Row {
                                Box(modifier = Modifier.width(42.dp).height(24.dp))
                                tables.rpmAxis.forEachIndexed { c, rpm ->
                                    Text(
                                        text = "${rpm / 100}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.sp,
                                        color = if (c == selectedCol) TelemetryCyan else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(34.dp)
                                    )
                                }
                            }
                            matrix.forEachIndexed { r, rowVals ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${tables.tpsAxis[r]}%",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.sp,
                                        color = if (r == selectedRow) HondaRed else TextSecondary,
                                        modifier = Modifier.width(42.dp)
                                    )
                                    rowVals.forEachIndexed { c, cell ->
                                        val isSel = r == selectedRow && c == selectedCol
                                        val bg = heatMapColor(cell, minRange, maxRange)
                                        Box(
                                            modifier = Modifier
                                                .width(34.dp)
                                                .height(26.dp)
                                                .padding(1.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(bg.copy(alpha = if (isSel) 1f else 0.72f))
                                                .then(
                                                    if (isSel) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                                                    else Modifier
                                                )
                                                .clickable {
                                                    selectedRow = r
                                                    selectedCol = c
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isFuelTab) cell.toInt().toString() else "%.1f".format(cell),
                                                fontFamily = JetBrainsMonoFamily,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = ObsidianBg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> Curve2DGraph(matrix = matrix, rpmAxis = tables.rpmAxis, isFuel = isFuelTab, selectedRow = selectedRow)
                    else -> Surface3DGraph(matrix = matrix, isFuel = isFuelTab)
                }

                // Cell Fine-Tuning Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sel: TPS ${tables.tpsAxis[selectedRow]}% @ ${tables.rpmAxis[selectedCol]} RPM",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isFuelTab) "$currentCellVal %" else "$currentCellVal °BTDC",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAdjustCell(isFuelTab, selectedRow, selectedCol, -stepDelta) },
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonElevated),
                            modifier = Modifier.height(44.dp).testTag("cell_minus_button")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Kurangi Nilai", tint = Color.White)
                        }
                        Button(
                            onClick = { onAdjustCell(isFuelTab, selectedRow, selectedCol, stepDelta) },
                            colors = ButtonDefaults.buttonColors(containerColor = HondaRed),
                            modifier = Modifier.height(44.dp).testTag("cell_plus_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah Nilai", tint = Color.White)
                        }
                        OutlinedButton(
                            onClick = { onSmoothTable(isFuelTab) },
                            border = BorderStroke(1.dp, TelemetryCyan),
                            modifier = Modifier.height(44.dp).testTag("smooth_3x3_button")
                        ) {
                            Icon(Icons.Filled.BlurOn, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Smooth", color = TelemetryCyan)
                        }
                    }
                }
            }
        }

        // Rev Limiter & Launch Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rev Limiter Cut", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text("${tables.revLimiterRpm} RPM", fontFamily = JetBrainsMonoFamily, color = HondaRed, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = tables.revLimiterRpm.toFloat(),
                    onValueChange = { onUpdateLimits(it.toInt(), tables.launchControlRpm, tables.targetAfr) },
                    valueRange = 8000f..15500f,
                    colors = SliderDefaults.colors(thumbColor = HondaRed, activeTrackColor = HondaRed)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Launch Control RPM", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text("${tables.launchControlRpm} RPM", fontFamily = JetBrainsMonoFamily, color = TelemetryCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = tables.launchControlRpm.toFloat(),
                    onValueChange = { onUpdateLimits(tables.revLimiterRpm, it.toInt(), tables.targetAfr) },
                    valueRange = 3000f..8500f,
                    colors = SliderDefaults.colors(thumbColor = TelemetryCyan, activeTrackColor = TelemetryCyan)
                )
            }
        }

        // AI Auto-Mapping Assistant (Firebase AI Logic)
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TelemetryCyan)
                    Text("AI Auto-Mapping Assistant", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                OutlinedTextField(
                    value = engineMods,
                    onValueChange = { engineMods = it },
                    label = { Text("Spesifikasi Mesin / Modifikasi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fuelOctane,
                        onValueChange = { fuelOctane = it },
                        label = { Text("BBM / Oktan") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tuningGoal,
                        onValueChange = { tuningGoal = it },
                        label = { Text("Target Tuning") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Button(
                    onClick = { onRunAiTuning(engineMods, fuelOctane, tuningGoal) },
                    enabled = aiState !is AiUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = ObsidianBg),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_autotune_button")
                ) {
                    if (aiState is AiUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianBg, strokeWidth = 2.dp)
                    } else {
                        Text("Analisa & Hitung Rekomendasi AI", fontWeight = FontWeight.Bold)
                    }
                }

                when (aiState) {
                    is AiUiState.Success -> {
                        val rec = aiState.recommendation
                        Surface(
                            color = CarbonElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldOk),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Fuel ${if (rec.fuelDeltaPct >= 0) "+" else ""}${rec.fuelDeltaPct}% • Ign ${if (rec.ignitionDeltaDeg >= 0) "+" else ""}${rec.ignitionDeltaDeg}° • Limiter ${rec.recommendedLimiterRpm} RPM • AFR ${rec.targetAfr}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldOk
                                )
                                Text(rec.summary, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                rec.warnings.forEach { w ->
                                    Text("• $w", style = MaterialTheme.typography.labelSmall, color = AmberWarn)
                                }
                                Button(
                                    onClick = { onApplyAiRecommendation(rec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOk, contentColor = ObsidianBg),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("Terapkan Rekomendasi ke Map 16x16", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is AiUiState.Error -> {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HondaRed
                        )
                    }
                    else -> Unit
                }
            }
        }

        // Cloud Save & .BIN / .XDF Export Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HondaRed),
                modifier = Modifier.weight(1f).height(50.dp).testTag("save_cloud_map_button")
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Cloud")
            }
            OutlinedButton(
                onClick = { binLauncher.launch("${activeEcu.id}_TUNED.bin") },
                border = BorderStroke(1.dp, TelemetryCyan),
                modifier = Modifier.weight(1f).height(50.dp).testTag("export_bin_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekspor .BIN", color = TelemetryCyan)
            }
        }

        OutlinedButton(
            onClick = { xdfLauncher.launch("${activeEcu.id}_UNLOCKED.xdf") },
            border = BorderStroke(1.dp, CarbonBorder),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_xdf_button")
        ) {
            Text("Ekspor Definisi TunerPro (.XDF)", color = Color.White)
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = CarbonSurface,
            title = { Text("Simpan Map ke Cloud Firestore", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mapTitle,
                        onValueChange = { mapTitle = it },
                        label = { Text("Nama Profil Map") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mapNotes,
                        onValueChange = { mapNotes = it },
                        label = { Text("Catatan Mekanik (opsional)") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveMapCloud(mapTitle, mapNotes)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HondaRed)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}
