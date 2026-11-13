package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.catalog.HondaEcuCatalog
import com.example.data.model.DynoRunRecord
import com.example.data.model.DynoRunSummary
import com.example.data.model.EcuTuningMap
import com.example.data.model.HondaEcuModel
import com.example.ui.components.DynoCrossoverGraph
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.HondaRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CloudUiState

@Composable
fun DynoDatabaseScreen(
    selectedSubTab: Int,
    onSubTabChange: (Int) -> Unit,
    activeEcu: HondaEcuModel,
    dynoSummary: DynoRunSummary,
    dynoRunsState: CloudUiState<List<DynoRunRecord>>,
    savedMapsState: CloudUiState<List<EcuTuningMap>>,
    onRunDynoPull: () -> Unit,
    onDeleteDynoRun: (String) -> Unit,
    onSelectEcu: (HondaEcuModel) -> Unit,
    onLoadCloudMap: (EcuTuningMap) -> Unit,
    onDeleteCloudMap: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sub-tab selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to "Dyno Test",
                1 to "Database ECU",
                2 to "Cloud Maps"
            ).forEach { (idx, label) ->
                FilterChip(
                    selected = selectedSubTab == idx,
                    onClick = { onSubTabChange(idx) },
                    label = { Text(label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HondaRed,
                        selectedLabelColor = Color.White,
                        containerColor = CarbonSurface,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dyno_subtab_$idx")
                )
            }
        }

        when (selectedSubTab) {
            0 -> DynoSection(
                activeEcu = activeEcu,
                dynoSummary = dynoSummary,
                dynoRunsState = dynoRunsState,
                onRunDynoPull = onRunDynoPull,
                onDeleteDynoRun = onDeleteDynoRun
            )
            1 -> EcuCatalogSection(
                activeEcu = activeEcu,
                onSelectEcu = onSelectEcu
            )
            else -> CloudMapsSection(
                savedMapsState = savedMapsState,
                onLoadCloudMap = onLoadCloudMap,
                onDeleteCloudMap = onDeleteCloudMap
            )
        }
    }
}

@Composable
private fun DynoSection(
    activeEcu: HondaEcuModel,
    dynoSummary: DynoRunSummary,
    dynoRunsState: CloudUiState<List<DynoRunRecord>>,
    onRunDynoPull: () -> Unit,
    onDeleteDynoRun: (String) -> Unit
) {
    val hpGain = ((dynoSummary.peakTunedHp - dynoSummary.peakStockHp) * 100.0).toInt() / 100.0
    val tqGain = ((dynoSummary.peakTunedTorque - dynoSummary.peakStockTorque) * 100.0).toInt() / 100.0

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Virtual Dyno Crossover",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeEcu.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )
                }
                Icon(Icons.Filled.Timeline, contentDescription = null, tint = HondaRed)
            }

            DynoCrossoverGraph(points = dynoSummary.points)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DynoMetricCard(
                    title = "PEAK POWER",
                    value = "${dynoSummary.peakTunedHp} HP",
                    subtitle = "+$hpGain HP vs Stock @ ${dynoSummary.peakHpRpm} RPM",
                    accent = HondaRed,
                    modifier = Modifier.weight(1f)
                )
                DynoMetricCard(
                    title = "PEAK TORQUE",
                    value = "${dynoSummary.peakTunedTorque} Nm",
                    subtitle = "+$tqGain Nm vs Stock @ ${dynoSummary.peakTorqueRpm} RPM",
                    accent = EmeraldOk,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DragMetricPill("0–100 km/h", "${dynoSummary.zeroToHundredSec} s", Modifier.weight(1f))
                DragMetricPill("Drag 201m", "${dynoSummary.drag201mSec} s", Modifier.weight(1f))
                DragMetricPill("Drag 402m", "${dynoSummary.drag402mSec} s", Modifier.weight(1f))
            }

            Button(
                onClick = onRunDynoPull,
                colors = ButtonDefaults.buttonColors(containerColor = HondaRed, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("run_dyno_button")
            ) {
                Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Dyno Pull & Simpan ke Cloud", fontWeight = FontWeight.Bold)
            }
        }
    }

    Text(
        text = "Riwayat Dyno Pull (Cloud)",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )

    when (dynoRunsState) {
        is CloudUiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HondaRed)
            }
        }
        is CloudUiState.Error -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HondaRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dynoRunsState.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        is CloudUiState.Success -> {
            if (dynoRunsState.data.isEmpty()) {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CarbonBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada riwayat Dyno Pull. Tekan tombol Run Dyno Pull di atas untuk mencatat performa map aktif ke Cloud.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                dynoRunsState.data.forEach { run ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        border = BorderStroke(1.dp, CarbonBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(run.ecuName, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(run.mapName, color = TelemetryCyan, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "${run.peakHp} HP @ ${run.peakHpRpm} RPM • ${run.peakTorqueNm} Nm • 201m: ${run.drag201mSec}s",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldOk
                                )
                            }
                            IconButton(
                                onClick = { onDeleteDynoRun(run.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus riwayat dyno", tint = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EcuCatalogSection(
    activeEcu: HondaEcuModel,
    onSelectEcu: (HondaEcuModel) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    val categories = remember {
        listOf(
            "Semua",
            "Skutik 160cc",
            "Skutik 125-150cc",
            "Skutik 110cc",
            "Sport 150cc",
            "Bebek / Underbone",
            "Sport 250cc+"
        )
    }

    val filteredModels = remember(searchQuery, selectedCategory) {
        HondaEcuCatalog.MODELS.filter { model ->
            val catMatch = selectedCategory == "Semua" || model.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val queryMatch = q.isEmpty() ||
                model.name.lowercase().contains(q) ||
                model.partNumber.lowercase().contains(q) ||
                model.mcuArchitecture.lowercase().contains(q)
            catMatch && queryMatch
        }
    }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Cari motor, kode part (K2S/K45/K64), atau MCU") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ecu_search_input")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { selectedCategory = category },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TelemetryCyan.copy(alpha = 0.25f),
                    selectedLabelColor = TelemetryCyan,
                    containerColor = CarbonSurface,
                    labelColor = TextSecondary
                ),
                modifier = Modifier.height(48.dp)
            )
        }
    }

    Text(
        text = "Menampilkan ${filteredModels.size} dari ${HondaEcuCatalog.MODELS.size} Profil ECU Honda PGM-FI",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary
    )

    filteredModels.forEach { model ->
        val isActive = model.id == activeEcu.id
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            border = BorderStroke(1.dp, if (isActive) HondaRed else CarbonBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = if (isActive) HondaRed else TelemetryCyan
                        )
                        Column {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${model.partNumber} • ${model.manufacturer} ${model.mcuArchitecture}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = TelemetryCyan
                            )
                        }
                    }
                    if (isActive) {
                        Surface(
                            color = HondaRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, HondaRed)
                        ) {
                            Text(
                                text = "AKTIF",
                                style = MaterialTheme.typography.labelSmall,
                                color = HondaRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Flash ${model.flashSizeKb}KB • Limiter OEM ${model.stockRevLimiter} RPM • ${model.protocol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Offsets -> Fuel: ${model.fuelTableOffsetHex} | Ign: ${model.ignitionTableOffsetHex} | Lim: ${model.limiterOffsetHex}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarn
                )

                OutlinedButton(
                    onClick = { onSelectEcu(model) },
                    border = BorderStroke(1.dp, if (isActive) EmeraldOk else TelemetryCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("select_ecu_${model.id}")
                ) {
                    Text(
                        text = if (isActive) "ECU Sedang Aktif (Muat Ulang OEM)" else "Muat Profil ECU ke Editor",
                        color = if (isActive) EmeraldOk else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudMapsSection(
    savedMapsState: CloudUiState<List<EcuTuningMap>>,
    onLoadCloudMap: (EcuTuningMap) -> Unit,
    onDeleteCloudMap: (String) -> Unit
) {
    when (savedMapsState) {
        is CloudUiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HondaRed)
            }
        }
        is CloudUiState.Error -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HondaRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = savedMapsState.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        is CloudUiState.Success -> {
            if (savedMapsState.data.isEmpty()) {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CarbonBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Belum Ada Map Tersimpan di Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Buka tab Remap lalu tekan 'Simpan Cloud' untuk mencadangkan tabel 16x16 Fuel/Ignition dan CRC32 Anda ke Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                savedMapsState.data.forEach { mapItem ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        border = BorderStroke(1.dp, CarbonBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mapItem.mapName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${mapItem.ecuName} (${mapItem.partNumber})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TelemetryCyan
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCloudMap(mapItem.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus map cloud", tint = TextSecondary)
                                }
                            }

                            Text(
                                text = "Strategi: ${mapItem.strategyName} • Limiter ${mapItem.revLimiterRpm} RPM • Target AFR ${mapItem.targetAfr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "CRC32: ${mapItem.checksumHex}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOk
                            )
                            if (mapItem.notes.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${mapItem.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Button(
                                onClick = { onLoadCloudMap(mapItem) },
                                colors = ButtonDefaults.buttonColors(containerColor = TelemetryCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("load_cloud_map_${mapItem.id}")
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Muat Map ke Editor 16x16", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynoMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun DragMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelLarge,
                color = TelemetryCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
