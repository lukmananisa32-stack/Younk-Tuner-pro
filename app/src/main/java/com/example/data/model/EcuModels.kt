package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class HardwareInterfaceType(val label: String, val defaultBaudRate: Int) {
    USB_FTDI("USB OTG K-Line (FTDI/CH340)", 10400),
    BLUETOOTH("Bluetooth OBD2 / ELM327", 115200),
    CAN_BUS("USB CAN-Bus (500 kbps)", 500000),
    BENCH("Bench Telemetry (No Cable)", 10400)
}

enum class TuningStrategy(val label: String, val shortDesc: String) {
    STOCK("Stock OEM", "Setelan standar pabrikan AHM"),
    DRAG_RACE("Drag Race", "201m/402m: WOT +13% Fuel, +4° Ign, Limiter +1800"),
    TOURING("Touring", "Jarak jauh: Pendinginan klep, cruising efisien, Limiter +600"),
    HARIAN("Harian Stop & Go", "Responsif TPS bawah 2-25%, irit BBM langsam")
}

data class HondaEcuModel(
    val id: String,
    val name: String,
    val category: String,
    val partNumber: String,
    val mcuArchitecture: String,
    val manufacturer: String,
    val displacementCc: Int,
    val flashSizeKb: Int,
    val protocol: String,
    val stockRevLimiter: Int,
    val fuelTableOffsetHex: String,
    val ignitionTableOffsetHex: String,
    val limiterOffsetHex: String,
    val rpmBreakpoints: List<Int>
)

data class TuningTables(
    val rpmAxis: List<Int>,
    val tpsAxis: List<Int>,
    val fuelVe: List<List<Double>>,       // 16 x 16
    val ignitionAdvance: List<List<Double>>, // 16 x 16
    val revLimiterRpm: Int,
    val launchControlRpm: Int,
    val targetAfr: Double,
    val activeStrategy: TuningStrategy = TuningStrategy.STOCK
)

data class HardwareConnection(
    val type: HardwareInterfaceType = HardwareInterfaceType.BENCH,
    val deviceName: String = "Bench ECU Mode (Ready)",
    val baudRate: Int = 10400,
    val connected: Boolean = true,
    val isPhysicalDevice: Boolean = false,
    val protocol: String = "K-Line (ISO 9141-2 / Honda 10400bps)",
    val voltage: Double = 12.85,
    val latencyMs: Int = 6,
    val lastPacketHex: String = "FE 04 FF FF -> 72 05 00 F0 99"
)

data class LiveTelemetry(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val tpsPercent: Int = 0,
    val mapKpa: Int = 101,
    val ectCelsius: Int = 34,
    val iatCelsius: Int = 30,
    val o2VoltageMv: Int = 450,
    val afr: Double = 14.7,
    val targetAfr: Double = 14.7,
    val batteryVoltage: Double = 12.85,
    val ignitionAdvanceDeg: Int = 10,
    val injectorPulseWidthMs: Double = 0.0,
    val injectorDutyCyclePercent: Double = 0.0,
    val gear: String = "N",
    val sideStandDown: Boolean = false,
    val milActive: Boolean = false,
    val engineRunning: Boolean = false
)

data class DtcRecord(
    val code: String,
    val blinkCode: Int,
    val title: String,
    val description: String,
    val remedy: String,
    val status: String = "ACTIVE",
    val timestamp: String = ""
)

data class SmartkeyData(
    val ecmId: String = "7A89-F4C2-9011",
    val scuId: String = "SCU-33P-08994",
    val registeredFob1: String = "528-941-002",
    val registeredFob2: String = "528-941-003",
    val emergencyPin: String = "942851",
    val frequencyMhz: Double = 433.92
)

data class SensorCalibrationData(
    val tpsClosedVoltage: Double = 0.49,
    val tpsWotVoltage: Double = 4.35,
    val tpsZeroOffsetDeg: Double = 0.0,
    val o2Mode: String = "narrowband",
    val o2MinMv: Int = 100,
    val o2SwitchPointMv: Int = 450,
    val o2MaxMv: Int = 850,
    val lastCalibratedTime: String = "Tersinkronisasi EEPROM"
)

data class EcuSpecialFeatures(
    val offImmobilizer: Boolean = false,
    val offSideStand: Boolean = false,
    val offO2Sensor: Boolean = false,
    val offEctCut: Boolean = false,
    val muteDtcCodes: List<String> = emptyList()
)

data class FlashProgressState(
    val isFlashing: Boolean = false,
    val progressPercent: Int = 0,
    val currentAddressHex: String = "0x000000",
    val stageMessage: String = "Siap menulis ke memori Flash ECU",
    val logs: List<String> = emptyList(),
    val completedSuccess: Boolean = false
)

data class DynoPoint(
    val rpm: Int,
    val stockHp: Double,
    val tunedHp: Double,
    val stockTorqueNm: Double,
    val tunedTorqueNm: Double
)

data class DynoRunSummary(
    val points: List<DynoPoint>,
    val peakStockHp: Double,
    val peakTunedHp: Double,
    val peakHpRpm: Int,
    val peakStockTorque: Double,
    val peakTunedTorque: Double,
    val peakTorqueRpm: Int,
    val zeroToHundredSec: Double,
    val drag201mSec: Double,
    val drag402mSec: Double
)

data class AiTuningRecommendation(
    val fuelDeltaPct: Double = 0.0,
    val ignitionDeltaDeg: Double = 0.0,
    val recommendedLimiterRpm: Int = 10500,
    val targetAfr: Double = 12.8,
    val summary: String = "",
    val warnings: List<String> = emptyList()
)

@IgnoreExtraProperties
data class EcuTuningMap(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mapName: String = "",
    val ecuModelId: String = "",
    val ecuName: String = "",
    val partNumber: String = "",
    val strategyName: String = "",
    val revLimiterRpm: Long = 10000L,
    val launchControlRpm: Long = 4500L,
    val targetAfr: Double = 13.2,
    val fuelVeValues: List<Double> = emptyList(),
    val ignitionValues: List<Double> = emptyList(),
    val checksumHex: String = "",
    val offImmobilizer: Boolean = false,
    val offSideStand: Boolean = false,
    val offO2Sensor: Boolean = false,
    val offEctCut: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DynoRunRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ecuModelId: String = "",
    val ecuName: String = "",
    val mapName: String = "",
    val peakHp: Double = 0.0,
    val peakHpRpm: Long = 0L,
    val peakTorqueNm: Double = 0.0,
    val peakTorqueRpm: Long = 0L,
    val zeroToHundredSec: Double = 0.0,
    val drag201mSec: Double = 0.0,
    val drag402mSec: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)
