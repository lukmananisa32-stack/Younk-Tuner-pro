package com.example.ui.viewmodel

import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.HondaEcuCatalog
import com.example.data.catalog.TuningPresets
import com.example.data.hardware.HondaEcuHardwareManager
import com.example.data.model.AiTuningRecommendation
import com.example.data.model.DtcRecord
import com.example.data.model.DynoRunRecord
import com.example.data.model.DynoRunSummary
import com.example.data.model.EcuSpecialFeatures
import com.example.data.model.EcuTuningMap
import com.example.data.model.FlashProgressState
import com.example.data.model.HardwareConnection
import com.example.data.model.HondaEcuModel
import com.example.data.model.LiveTelemetry
import com.example.data.model.SensorCalibrationData
import com.example.data.model.SmartkeyData
import com.example.data.model.TuningStrategy
import com.example.data.model.TuningTables
import com.example.data.repository.AiTuningRepository
import com.example.data.repository.TuningCloudRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val recommendation: AiTuningRecommendation) : AiUiState
    data class Error(val message: String) : AiUiState
}

class YounkTunerViewModel(
    private val cloudRepo: TuningCloudRepository,
    private val aiRepo: AiTuningRepository,
    private val hardware: HondaEcuHardwareManager,
    private val currentUserId: String
) : ViewModel() {

    private val _activeEcu = MutableStateFlow(HondaEcuCatalog.MODELS.first())
    val activeEcu: StateFlow<HondaEcuModel> = _activeEcu.asStateFlow()

    private val _tuningTables = MutableStateFlow(TuningPresets.generateTablesForEcu(HondaEcuCatalog.MODELS.first()))
    val tuningTables: StateFlow<TuningTables> = _tuningTables.asStateFlow()

    private val _specialFeatures = MutableStateFlow(EcuSpecialFeatures())
    val specialFeatures: StateFlow<EcuSpecialFeatures> = _specialFeatures.asStateFlow()

    private val _checksumHex = MutableStateFlow(
        TuningPresets.calculateCrc32Hex(
            HondaEcuCatalog.MODELS.first(),
            _tuningTables.value,
            EcuSpecialFeatures()
        )
    )
    val checksumHex: StateFlow<String> = _checksumHex.asStateFlow()

    private val _dynoSummary = MutableStateFlow(
        TuningPresets.buildDynoComparison(HondaEcuCatalog.MODELS.first(), _tuningTables.value)
    )
    val dynoSummary: StateFlow<DynoRunSummary> = _dynoSummary.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _actuatorResult = MutableStateFlow<String?>(null)
    val actuatorResult: StateFlow<String?> = _actuatorResult.asStateFlow()

    val telemetry: StateFlow<LiveTelemetry> = hardware.telemetry
    val connection: StateFlow<HardwareConnection> = hardware.connection
    val dtcs: StateFlow<List<DtcRecord>> = hardware.dtcs
    val calibration: StateFlow<SensorCalibrationData> = hardware.calibration
    val smartkey: StateFlow<SmartkeyData> = hardware.smartkey
    val flashState: StateFlow<FlashProgressState> = hardware.flashState
    val discoveredUsbDevices: StateFlow<List<UsbDevice>> = hardware.discoveredUsbDevices

    val savedMapsState: StateFlow<CloudUiState<List<EcuTuningMap>>> =
        cloudRepo.observeSavedMaps(currentUserId)
            .map<List<EcuTuningMap>, CloudUiState<List<EcuTuningMap>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing saved ECU maps", root)
                emit(CloudUiState.Error(root.message ?: "Gagal memuat map dari cloud"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val dynoRunsState: StateFlow<CloudUiState<List<DynoRunRecord>>> =
        cloudRepo.observeDynoRuns(currentUserId)
            .map<List<DynoRunRecord>, CloudUiState<List<DynoRunRecord>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing dyno runs", root)
                emit(CloudUiState.Error(root.message ?: "Gagal memuat riwayat dyno"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun selectEcu(ecu: HondaEcuModel) {
        _activeEcu.value = ecu
        hardware.setActiveEcu(ecu)
        val freshTables = TuningPresets.generateTablesForEcu(ecu)
        updateTablesAndRecalc(freshTables)
        _bannerMessage.value = "Profil ECU ${ecu.name} (${ecu.partNumber}) dimuat."
    }

    fun applyStrategy(strategy: TuningStrategy) {
        val updated = TuningPresets.applyStrategy(_activeEcu.value, strategy)
        updateTablesAndRecalc(updated)
        _bannerMessage.value = "Preset ${strategy.label} diterapkan."
    }

    fun adjustTableCell(isFuel: Boolean, row: Int, col: Int, delta: Double) {
        val cur = _tuningTables.value
        val updated = if (isFuel) {
            val matrix = cur.fuelVe.mapIndexed { r, rowList ->
                if (r != row) rowList else rowList.mapIndexed { c, v ->
                    if (c != col) v else ((max(45.0, min(145.0, v + delta))) * 10.0).roundToInt() / 10.0
                }
            }
            cur.copy(fuelVe = matrix)
        } else {
            val matrix = cur.ignitionAdvance.mapIndexed { r, rowList ->
                if (r != row) rowList else rowList.mapIndexed { c, v ->
                    if (c != col) v else ((max(4.0, min(45.0, v + delta))) * 10.0).roundToInt() / 10.0
                }
            }
            cur.copy(ignitionAdvance = matrix)
        }
        updateTablesAndRecalc(updated)
    }

    fun smoothActiveTable(isFuel: Boolean) {
        val cur = _tuningTables.value
        val updated = if (isFuel) {
            cur.copy(fuelVe = TuningPresets.smoothMatrix3x3(cur.fuelVe))
        } else {
            cur.copy(ignitionAdvance = TuningPresets.smoothMatrix3x3(cur.ignitionAdvance))
        }
        updateTablesAndRecalc(updated)
        _bannerMessage.value = "Smoothing Gaussian 3x3 selesai."
    }

    fun updateLimiterSettings(revLimiterRpm: Int, launchRpm: Int, targetAfr: Double) {
        val cur = _tuningTables.value
        updateTablesAndRecalc(
            cur.copy(
                revLimiterRpm = revLimiterRpm.coerceIn(7500, 16000),
                launchControlRpm = launchRpm.coerceIn(2500, 9500),
                targetAfr = ((targetAfr.coerceIn(11.0, 16.0)) * 10.0).roundToInt() / 10.0
            )
        )
    }

    private fun updateTablesAndRecalc(tables: TuningTables) {
        _tuningTables.value = tables
        _checksumHex.value = TuningPresets.calculateCrc32Hex(_activeEcu.value, tables, _specialFeatures.value)
        _dynoSummary.value = TuningPresets.buildDynoComparison(_activeEcu.value, tables)
    }

    // --- AI Tuning Assistant ---
    fun runAiTuning(engineMods: String, fuelOctane: String, goal: String) {
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            val res = aiRepo.generateTuningRecommendation(
                ecu = _activeEcu.value,
                engineMods = engineMods.ifBlank { "Mesin standar harian + knalpot racing" },
                fuelOctane = fuelOctane.ifBlank { "RON 92 / Pertamax" },
                goal = goal.ifBlank { "Akselerasi bawah-tengah responsif & aman harian" }
            )
            _aiState.value = res.fold(
                onSuccess = { AiUiState.Success(it) },
                onFailure = { AiUiState.Error(it.localizedMessage ?: "Analisis AI gagal.") }
            )
        }
    }

    fun applyAiRecommendation(rec: AiTuningRecommendation) {
        val updated = TuningPresets.applyGlobalOffset(
            tables = _tuningTables.value,
            fuelDeltaPct = rec.fuelDeltaPct,
            ignitionDeltaDeg = rec.ignitionDeltaDeg,
            newLimiterRpm = rec.recommendedLimiterRpm,
            newTargetAfr = rec.targetAfr
        )
        updateTablesAndRecalc(updated)
        _bannerMessage.value = "Rekomendasi AI diterapkan ke tabel 16x16 & Checksum CRC32 diperbarui!"
    }

    // --- Cloud Persistence ---
    fun saveCurrentMapToCloud(mapName: String, notes: String) {
        viewModelScope.launch {
            val ecu = _activeEcu.value
            val tables = _tuningTables.value
            val feat = _specialFeatures.value
            val mapEntity = EcuTuningMap(
                userId = currentUserId,
                mapName = mapName.ifBlank { "${ecu.name} Custom" },
                ecuModelId = ecu.id,
                ecuName = ecu.name,
                partNumber = ecu.partNumber,
                strategyName = tables.activeStrategy.label,
                revLimiterRpm = tables.revLimiterRpm.toLong(),
                launchControlRpm = tables.launchControlRpm.toLong(),
                targetAfr = tables.targetAfr,
                fuelVeValues = tables.fuelVe.flatten(),
                ignitionValues = tables.ignitionAdvance.flatten(),
                checksumHex = _checksumHex.value,
                offImmobilizer = feat.offImmobilizer,
                offSideStand = feat.offSideStand,
                offO2Sensor = feat.offO2Sensor,
                offEctCut = feat.offEctCut,
                notes = notes
            )
            val result = cloudRepo.saveNewEcuMap(mapEntity)
            _bannerMessage.value = result.fold(
                onSuccess = { "Map '${mapEntity.mapName}' tersimpan ke Cloud Firestore." },
                onFailure = { "Gagal menyimpan map: ${it.localizedMessage}" }
            )
        }
    }

    fun loadCloudMap(saved: EcuTuningMap) {
        val model = HondaEcuCatalog.MODELS.find { it.id == saved.ecuModelId } ?: _activeEcu.value
        _activeEcu.value = model
        hardware.setActiveEcu(model)

        val fuelGrid = if (saved.fuelVeValues.size == 256) saved.fuelVeValues.chunked(16) else _tuningTables.value.fuelVe
        val ignGrid = if (saved.ignitionValues.size == 256) saved.ignitionValues.chunked(16) else _tuningTables.value.ignitionAdvance
        val strategy = TuningStrategy.entries.find { it.label == saved.strategyName } ?: TuningStrategy.HARIAN

        val loadedTables = TuningTables(
            rpmAxis = model.rpmBreakpoints,
            tpsAxis = TuningPresets.TPS_AXIS,
            fuelVe = fuelGrid,
            ignitionAdvance = ignGrid,
            revLimiterRpm = saved.revLimiterRpm.toInt(),
            launchControlRpm = saved.launchControlRpm.toInt(),
            targetAfr = saved.targetAfr,
            activeStrategy = strategy
        )
        _specialFeatures.value = EcuSpecialFeatures(
            offImmobilizer = saved.offImmobilizer,
            offSideStand = saved.offSideStand,
            offO2Sensor = saved.offO2Sensor,
            offEctCut = saved.offEctCut
        )
        updateTablesAndRecalc(loadedTables)
        _bannerMessage.value = "Map Cloud '${saved.mapName}' dimuat ke editor."
    }

    fun deleteCloudMap(mapId: String) {
        viewModelScope.launch {
            val res = cloudRepo.deleteEcuMap(mapId)
            _bannerMessage.value = res.fold(
                onSuccess = { "Map dihapus dari Cloud." },
                onFailure = { "Gagal menghapus map: ${it.localizedMessage}" }
            )
        }
    }

    fun runAndSaveDynoPull() {
        val summary = TuningPresets.buildDynoComparison(_activeEcu.value, _tuningTables.value)
        _dynoSummary.value = summary
        viewModelScope.launch {
            val ecu = _activeEcu.value
            val record = DynoRunRecord(
                userId = currentUserId,
                ecuModelId = ecu.id,
                ecuName = ecu.name,
                mapName = "${_tuningTables.value.activeStrategy.label} (Lim ${ _tuningTables.value.revLimiterRpm })",
                peakHp = summary.peakTunedHp,
                peakHpRpm = summary.peakHpRpm.toLong(),
                peakTorqueNm = summary.peakTunedTorque,
                peakTorqueRpm = summary.peakTorqueRpm.toLong(),
                zeroToHundredSec = summary.zeroToHundredSec,
                drag201mSec = summary.drag201mSec,
                drag402mSec = summary.drag402mSec
            )
            val res = cloudRepo.saveDynoRun(record)
            _bannerMessage.value = res.fold(
                onSuccess = { "Dyno Pull ${summary.peakTunedHp} HP tercatat ke Cloud." },
                onFailure = { "Dyno selesai (${summary.peakTunedHp} HP). Info sync: ${it.localizedMessage}" }
            )
        }
    }

    fun deleteDynoRun(runId: String) {
        viewModelScope.launch {
            cloudRepo.deleteDynoRun(runId)
        }
    }

    // --- ROM & XDF Export Builders ---
    fun buildCurrentRomBytes(): ByteArray =
        TuningPresets.buildRomBytes(_activeEcu.value, _tuningTables.value, _specialFeatures.value)

    fun buildCurrentXdfXml(): String =
        TuningPresets.buildXdfDefinitionXml(_activeEcu.value)

    // --- Hardware & Diagnostics Delegation ---
    fun toggleEngine() = hardware.toggleEngine()
    fun setBenchThrottle(tps: Int) = hardware.setBenchThrottle(tps)
    fun scanUsbDevices() = hardware.scanUsbDevices()
    fun connectUsb(device: UsbDevice, baud: Int) {
        hardware.connectUsbDevice(device, baud) { msg -> _bannerMessage.value = msg }
    }
    fun getPairedBluetoothDevices(): List<BluetoothDevice> = hardware.getPairedBluetoothDevices()
    fun connectBluetooth(device: BluetoothDevice) {
        hardware.connectBluetoothDevice(device) { msg -> _bannerMessage.value = msg }
    }
    fun switchToBenchMode() {
        hardware.switchToBenchMode()
        _bannerMessage.value = "Mode Bench Telemetry aktif."
    }

    fun flashEcu(fullRom: Boolean) = hardware.startFlashEcu(_activeEcu.value, fullRom)
    fun recoveryUnbrick() = hardware.startRecoveryUnbrick(_activeEcu.value)
    fun scanDtcs(simulateFault: Boolean) = hardware.scanDtcsFromEcu(simulateFault)

    fun clearDtcs() {
        viewModelScope.launch {
            hardware.clearDtcs()
            _bannerMessage.value = "Memori DTC & Lampu MIL berhasil di-reset."
        }
    }

    fun testActuator(name: String) {
        viewModelScope.launch {
            _actuatorResult.value = "Mengirim pulsa uji ke $name..."
            _actuatorResult.value = hardware.testActuator(name)
        }
    }

    fun resetTpsZero() {
        viewModelScope.launch {
            _bannerMessage.value = hardware.resetTpsZero(0.49)
        }
    }

    fun calibrateO2(mode: String, switchMv: Int) {
        viewModelScope.launch {
            _bannerMessage.value = hardware.calibrateO2Sensor(mode, switchMv)
        }
    }

    fun updateSpecialFeatures(features: EcuSpecialFeatures) {
        _specialFeatures.value = features
        _checksumHex.value = TuningPresets.calculateCrc32Hex(_activeEcu.value, _tuningTables.value, features)
        _bannerMessage.value = hardware.applyBypassFeatures(features)
    }

    fun readSmartkey() {
        viewModelScope.launch {
            hardware.readSmartkeyScu()
            _bannerMessage.value = "Data SCU & Smartkey berhasil dibaca dari bus."
        }
    }

    fun registerSmartkeyFob(fobCode: String) {
        viewModelScope.launch {
            hardware.registerSmartkeyFob(fobCode.ifBlank { "528-941-889" })
            _bannerMessage.value = "Remote Smartkey baru berhasil didaftarkan ke SCU."
        }
    }

    private companion object {
        const val TAG = "YounkTunerVM"
    }
}
