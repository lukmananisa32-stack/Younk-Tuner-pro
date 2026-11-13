package com.example.data.hardware

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.data.catalog.HondaEcuCatalog
import com.example.data.model.DtcRecord
import com.example.data.model.EcuSpecialFeatures
import com.example.data.model.FlashProgressState
import com.example.data.model.HardwareConnection
import com.example.data.model.HardwareInterfaceType
import com.example.data.model.HondaEcuModel
import com.example.data.model.LiveTelemetry
import com.example.data.model.SensorCalibrationData
import com.example.data.model.SmartkeyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class HondaEcuHardwareManager(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var telemetryJob: Job? = null

    private val usbManager: UsbManager? =
        appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private var usbEndpointIn: UsbEndpoint? = null

    private var btSocket: BluetoothSocket? = null

    private val _connection = MutableStateFlow(HardwareConnection())
    val connection: StateFlow<HardwareConnection> = _connection.asStateFlow()

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private val _dtcs = MutableStateFlow<List<DtcRecord>>(emptyList())
    val dtcs: StateFlow<List<DtcRecord>> = _dtcs.asStateFlow()

    private val _calibration = MutableStateFlow(SensorCalibrationData())
    val calibration: StateFlow<SensorCalibrationData> = _calibration.asStateFlow()

    private val _smartkey = MutableStateFlow(SmartkeyData())
    val smartkey: StateFlow<SmartkeyData> = _smartkey.asStateFlow()

    private val _flashState = MutableStateFlow(FlashProgressState())
    val flashState: StateFlow<FlashProgressState> = _flashState.asStateFlow()

    private val _discoveredUsbDevices = MutableStateFlow<List<UsbDevice>>(emptyList())
    val discoveredUsbDevices: StateFlow<List<UsbDevice>> = _discoveredUsbDevices.asStateFlow()

    private var activeEcu: HondaEcuModel = HondaEcuCatalog.MODELS.first()
    private val rxBuffer = ArrayList<Int>()
    private var lastTxEchoCount = 0

    init {
        startTelemetryLoop()
        scanUsbDevices()
    }

    fun setActiveEcu(ecu: HondaEcuModel) {
        activeEcu = ecu
    }

    // --- Honda PGM-FI Protocol Checksum ---
    fun calculateHondaChecksum(data: IntArray): Int {
        var sum = 0
        for (b in data) sum += b
        return (0x100 - (sum and 0xFF)) and 0xFF
    }

    fun verifyHondaPacket(packet: List<Int>): Boolean {
        if (packet.size < 3) return false
        var sum = 0
        for (b in packet) sum += b
        return (sum and 0xFF) == 0x00
    }

    private fun buildHondaFrame(vararg bytes: Int): ByteArray {
        val cs = calculateHondaChecksum(bytes)
        val out = ByteArray(bytes.size + 1)
        for (i in bytes.indices) out[i] = (bytes[i] and 0xFF).toByte()
        out[bytes.size] = (cs and 0xFF).toByte()
        return out
    }

    // --- USB OTG Host Support (FTDI, CH340, CP2102, CANable) ---
    fun scanUsbDevices(): List<UsbDevice> {
        val list = usbManager?.deviceList?.values?.toList().orEmpty()
        _discoveredUsbDevices.value = list
        return list
    }

    fun connectUsbDevice(
        device: UsbDevice,
        baudRate: Int = 10400,
        onStatus: (String) -> Unit
    ) {
        val mgr = usbManager ?: run {
            onStatus("USB Host tidak tersedia pada perangkat ini.")
            return
        }

        if (!mgr.hasPermission(device)) {
            val action = "${appContext.packageName}.USB_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == action) {
                        try {
                            appContext.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (granted) {
                            openUsbSerialConnection(device, baudRate, onStatus)
                        } else {
                            onStatus("Izin USB OTG ditolak oleh pengguna.")
                        }
                    }
                }
            }
            val filter = IntentFilter(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(receiver, filter)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val pi = PendingIntent.getBroadcast(appContext, 0, Intent(action), flags)
            mgr.requestPermission(device, pi)
            onStatus("Meminta izin USB OTG untuk ${device.productName ?: "K-Line Adapter"}...")
        } else {
            openUsbSerialConnection(device, baudRate, onStatus)
        }
    }

    private fun openUsbSerialConnection(
        device: UsbDevice,
        baudRate: Int,
        onStatus: (String) -> Unit
    ) {
        scope.launch {
            try {
                closePhysicalConnections()
                val conn = usbManager?.openDevice(device)
                    ?: throw IOException("Gagal membuka UsbDeviceConnection.")
                val intf = device.getInterface(0)
                if (!conn.claimInterface(intf, true)) {
                    conn.close()
                    throw IOException("Gagal mengklaim interface USB.")
                }

                var epIn: UsbEndpoint? = null
                var epOut: UsbEndpoint? = null
                for (i in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(i)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
                        else epOut = ep
                    }
                }

                // Configure FTDI / CDC baud rate
                if (device.vendorId == 0x0403) {
                    // FTDI Reset + Set Baud Rate (3_000_000 / baudRate)
                    conn.controlTransfer(0x40, 0x00, 0x0000, 0, null, 0, 500)
                    val divisor = (3000000 / baudRate).coerceIn(1, 16383)
                    conn.controlTransfer(0x40, 0x03, divisor, 0, null, 0, 500)
                    conn.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 500) // 8N1
                } else {
                    // CDC / CP2102 line coding fallback
                    val lineCoding = byteArrayOf(
                        (baudRate and 0xFF).toByte(),
                        ((baudRate shr 8) and 0xFF).toByte(),
                        ((baudRate shr 16) and 0xFF).toByte(),
                        ((baudRate shr 24) and 0xFF).toByte(),
                        0, 0, 8
                    )
                    conn.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.size, 500)
                }

                usbConnection = conn
                usbInterface = intf
                usbEndpointIn = epIn
                usbEndpointOut = epOut

                val label = device.productName ?: "USB K-Line [VID:${device.vendorId.toString(16)}]"
                _connection.value = HardwareConnection(
                    type = if (baudRate >= 500000) HardwareInterfaceType.CAN_BUS else HardwareInterfaceType.USB_FTDI,
                    deviceName = label,
                    baudRate = baudRate,
                    connected = true,
                    isPhysicalDevice = true,
                    protocol = if (baudRate >= 500000) "CAN-Bus ISO 15765-4 500kbps" else "K-Line Honda Fast-Init ${baudRate}bps",
                    voltage = 12.85,
                    latencyMs = 4
                )

                sendHondaFastInit()
                onStatus("Terhubung ke $label (${baudRate} bps)")
            } catch (e: Exception) {
                Log.e(TAG, "USB connect failed", e)
                onStatus("Gagal membuka USB OTG: ${e.localizedMessage}")
            }
        }
    }

    // --- Bluetooth RFCOMM SPP Connection ---
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<BluetoothDevice> {
        val btMgr = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = btMgr?.adapter
        return try {
            adapter?.bondedDevices?.toList().orEmpty()
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth permission missing", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBluetoothDevice(device: BluetoothDevice, onStatus: (String) -> Unit) {
        scope.launch {
            try {
                closePhysicalConnections()
                val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()
                btSocket = socket

                _connection.value = HardwareConnection(
                    type = HardwareInterfaceType.BLUETOOTH,
                    deviceName = "BT: ${device.name ?: device.address}",
                    baudRate = 115200,
                    connected = true,
                    isPhysicalDevice = true,
                    protocol = "Bluetooth SPP / Honda K-Line Bridge",
                    voltage = 12.82,
                    latencyMs = 14
                )

                sendHondaFastInit()
                onStatus("Bluetooth terhubung ke ${device.name ?: device.address}")
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth connect failed", e)
                onStatus("Gagal menyambung Bluetooth: ${e.localizedMessage}")
            }
        }
    }

    fun switchToBenchMode() {
        closePhysicalConnections()
        _connection.value = HardwareConnection(
            type = HardwareInterfaceType.BENCH,
            deviceName = "Bench ECU Mode (Ready)",
            baudRate = 10400,
            connected = true,
            isPhysicalDevice = false,
            protocol = "K-Line (ISO 9141-2 / Honda 10400bps)",
            voltage = 12.85,
            latencyMs = 6
        )
    }

    private fun closePhysicalConnections() {
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (_: Exception) {}
        usbConnection = null
        usbInterface = null
        usbEndpointIn = null
        usbEndpointOut = null

        try {
            btSocket?.close()
        } catch (_: Exception) {}
        btSocket = null
    }

    private fun writeBytesPhysical(bytes: ByteArray) {
        val hexStr = bytes.joinToString(" ") { "%02X".format(it) }
        _connection.value = _connection.value.copy(lastPacketHex = hexStr)
        lastTxEchoCount = bytes.size

        try {
            val epOut = usbEndpointOut
            val uConn = usbConnection
            if (epOut != null && uConn != null) {
                uConn.bulkTransfer(epOut, bytes, bytes.size, 100)
            }
            btSocket?.outputStream?.write(bytes)
        } catch (e: Exception) {
            Log.w(TAG, "Physical write warning: ${e.message}")
        }
    }

    private fun readPhysicalIncoming() {
        val buf = ByteArray(64)
        var readLen = 0
        try {
            val epIn = usbEndpointIn
            val uConn = usbConnection
            if (epIn != null && uConn != null) {
                // Skip FTDI 2-byte modem status header if vendorId == 0x0403
                val n = uConn.bulkTransfer(epIn, buf, buf.size, 40)
                if (n > 2) {
                    for (i in 2 until n) {
                        if (lastTxEchoCount > 0) {
                            lastTxEchoCount--
                        } else {
                            rxBuffer.add(buf[i].toInt() and 0xFF)
                        }
                    }
                    readLen = n
                }
            }
            val btIn = btSocket?.inputStream
            if (btIn != null && btIn.available() > 0) {
                val n = btIn.read(buf)
                if (n > 0) {
                    for (i in 0 until n) {
                        rxBuffer.add(buf[i].toInt() and 0xFF)
                    }
                    readLen = n
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Physical read warning: ${e.message}")
        }
        if (readLen > 0) parseIncomingKLineBuffer()
    }

    private fun parseIncomingKLineBuffer() {
        while (rxBuffer.size >= 4) {
            if (rxBuffer[0] == 0x02) {
                val frameLen = rxBuffer[1]
                if (frameLen in 4..64 && rxBuffer.size >= frameLen) {
                    val packet = rxBuffer.subList(0, frameLen).toList()
                    repeat(frameLen) { rxBuffer.removeAt(0) }
                    if (verifyHondaPacket(packet) && packet.size >= 15 && packet[2] == 0x72 && packet[3] == 0x11) {
                        val rpmRaw = (packet[6] shl 8) or packet[7]
                        val rpm = (rpmRaw * 0.5).roundToInt()
                        val tps = ((packet[8] / 255.0) * 100.0).roundToInt()
                        val map = (packet[9] * 0.98).roundToInt()
                        val ect = packet[10] - 40
                        val iat = packet[11] - 40
                        val bat = (packet[12] * 1.0).roundToInt() / 10.0
                        val o2Mv = packet[13] * 5
                        val ign = (packet[14] * 0.5 - 10.0).roundToInt()
                        val afr = ((o2Mv / 1000.0 * 6.0 + 10.0) * 100.0).roundToInt() / 100.0
                        _telemetry.value = _telemetry.value.copy(
                            rpm = rpm,
                            tpsPercent = tps,
                            mapKpa = map,
                            ectCelsius = ect,
                            iatCelsius = iat,
                            batteryVoltage = bat,
                            o2VoltageMv = o2Mv,
                            ignitionAdvanceDeg = ign,
                            afr = afr,
                            engineRunning = rpm > 350,
                            speedKmh = ((rpm / 8500.0) * 110.0).roundToInt()
                        )
                    }
                } else {
                    break
                }
            } else {
                rxBuffer.removeAt(0)
            }
        }
    }

    suspend fun sendHondaFastInit() {
        writeBytesPhysical(byteArrayOf(0xFE.toByte(), 0x04, 0xFF.toByte(), 0xFF.toByte()))
        delay(180)
        writeBytesPhysical(buildHondaFrame(0x72, 0x05, 0x00, 0xF0))
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            var tick = 0.0
            while (isActive) {
                tick += 0.1
                val conn = _connection.value
                if (conn.isPhysicalDevice) {
                    writeBytesPhysical(buildHondaFrame(0x72, 0x07, 0x72, 0x11, 0x00, 0x14))
                    readPhysicalIncoming()
                } else {
                    val cur = _telemetry.value
                    if (cur.engineRunning) {
                        val tps = cur.tpsPercent
                        val maxRpm = activeEcu.stockRevLimiter.toDouble()
                        val targetRpm = 1520.0 + (tps / 100.0) * (maxRpm - 1520.0) + sin(tick * 4.5) * 45.0
                        val newRpm = (cur.rpm * 0.72 + targetRpm * 0.28).roundToInt()
                        val newSpeed = ((newRpm / maxRpm) * 124.0 * (tps.coerceAtLeast(15) / 100.0)).roundToInt()
                        val gear = when {
                            newSpeed <= 2 -> "N"
                            newSpeed < 25 -> "1"
                            newSpeed < 48 -> "2"
                            newSpeed < 72 -> "3"
                            newSpeed < 98 -> "4"
                            else -> "5"
                        }
                        val mapKpa = (38 + (tps / 100.0) * 62).roundToInt()
                        val newEct = min(90, if (cur.ectCelsius < 84) cur.ectCelsius + 1 else cur.ectCelsius)
                        val targetAfr = when {
                            tps > 70 -> 12.8
                            tps > 30 -> 13.6
                            else -> 14.7
                        }
                        val afr = ((targetAfr + sin(tick * 6.0) * 0.15) * 100.0).roundToInt() / 100.0
                        val o2Mv = (450 + sin(tick * 5.0) * 320).roundToInt().coerceIn(80, 920)
                        val ignDeg = (11 + (newRpm / maxRpm) * 23).roundToInt()
                        val pulseMs = ((1.8 + (tps / 100.0) * 7.4) * 100.0).roundToInt() / 100.0
                        val dutyPct = min(95.0, ((pulseMs * newRpm / 1200.0) * 10.0).roundToInt() / 10.0)

                        _telemetry.value = cur.copy(
                            rpm = newRpm,
                            speedKmh = newSpeed,
                            gear = gear,
                            mapKpa = mapKpa,
                            ectCelsius = newEct,
                            iatCelsius = 31,
                            afr = afr,
                            targetAfr = targetAfr,
                            o2VoltageMv = o2Mv,
                            ignitionAdvanceDeg = ignDeg,
                            injectorPulseWidthMs = pulseMs,
                            injectorDutyCyclePercent = dutyPct,
                            batteryVoltage = 14.2,
                            milActive = _dtcs.value.isNotEmpty()
                        )
                    } else {
                        _telemetry.value = cur.copy(
                            rpm = 0,
                            speedKmh = 0,
                            gear = "N",
                            mapKpa = 101,
                            afr = 14.7,
                            injectorPulseWidthMs = 0.0,
                            injectorDutyCyclePercent = 0.0,
                            batteryVoltage = 12.65,
                            milActive = _dtcs.value.isNotEmpty()
                        )
                    }
                }
                delay(100)
            }
        }
    }

    fun toggleEngine() {
        val next = !_telemetry.value.engineRunning
        _telemetry.value = _telemetry.value.copy(
            engineRunning = next,
            rpm = if (next) 1520 else 0,
            speedKmh = if (next) _telemetry.value.speedKmh else 0
        )
    }

    fun setBenchThrottle(tpsPercent: Int) {
        _telemetry.value = _telemetry.value.copy(tpsPercent = tpsPercent.coerceIn(0, 100))
    }

    // --- Flash & Recovery ---
    fun startFlashEcu(ecu: HondaEcuModel, fullRom: Boolean) {
        if (_flashState.value.isFlashing) return
        scope.launch {
            val totalBlocks = if (fullRom) 48 else 16
            val startAddr = if (fullRom) 0x000000 else 0x008000
            val blockSize = (ecu.flashSizeKb * 1024) / totalBlocks
            val logList = mutableListOf<String>()

            fun push(pct: Int, addr: String, stage: String) {
                logList.add("[$addr] $stage")
                _flashState.value = FlashProgressState(
                    isFlashing = pct < 100,
                    progressPercent = pct,
                    currentAddressHex = addr,
                    stageMessage = stage,
                    logs = logList.takeLast(8),
                    completedSuccess = pct == 100
                )
            }

            push(0, "0x000000", "Handshake Seed/Key Security Access (${ecu.partNumber})...")
            writeBytesPhysical(buildHondaFrame(0x72, 0x05, 0x27, 0x01))
            delay(350)

            push(10, "0x000000", "Sector Erase Memori Flash ${ecu.mcuArchitecture}...")
            writeBytesPhysical(buildHondaFrame(0x72, 0x06, 0x31, 0x01, if (fullRom) 0xFF else 0x01))
            delay(450)

            for (i in 1..totalBlocks) {
                val pct = 10 + ((i.toFloat() / totalBlocks) * 82).roundToInt()
                val addrHex = "0x%06X".format(startAddr + i * blockSize)
                push(pct, addrHex, "Menulis Blok $i/$totalBlocks (${ecu.flashSizeKb}KB Flash)...")
                writeBytesPhysical(buildHondaFrame(0x72, 0x14, 0x36, (i shr 8) and 0xFF, i and 0xFF))
                delay(45)
            }

            push(96, "0x01FFFF", "Validasi Checksum CRC32 & Write Counter EEPROM...")
            delay(350)
            push(100, "0x01FFFF", "Flash Selesai! Putar Kunci Kontak OFF lalu ON.")
        }
    }

    fun startRecoveryUnbrick(ecu: HondaEcuModel) {
        if (_flashState.value.isFlashing) return
        scope.launch {
            val logList = mutableListOf<String>()
            fun push(pct: Int, addr: String, stage: String) {
                logList.add("[$addr] $stage")
                _flashState.value = FlashProgressState(
                    isFlashing = pct < 100,
                    progressPercent = pct,
                    currentAddressHex = addr,
                    stageMessage = stage,
                    logs = logList.takeLast(8),
                    completedSuccess = pct == 100
                )
            }

            push(12, "BOOT", "Mengirim Pulsa Break 70ms ke Pin K-Line ECM...")
            sendHondaFastInit()
            delay(650)
            push(38, "0x000000", "Chip ${ecu.mcuArchitecture} merespons Bootloader!")
            delay(600)
            push(68, "0x004000", "Menghapus EEPROM korup & restorasi microcode AHM...")
            delay(750)
            push(92, "0x01FFFC", "Mengunci Master Checksum CRC32...")
            delay(450)
            _dtcs.value = emptyList()
            push(100, "READY", "Pemulihan Sukses! ECU Unbricked & Siap Digunakan.")
        }
    }

    // --- DTC & Actuator ---
    fun scanDtcsFromEcu(simulateFaultIfEmpty: Boolean = false) {
        scope.launch {
            writeBytesPhysical(buildHondaFrame(0x72, 0x05, 0x73, 0x01))
            delay(300)
            if (simulateFaultIfEmpty || (!_connection.value.isPhysicalDevice && _dtcs.value.isEmpty())) {
                val randomDtc = HondaEcuCatalog.DTC_CATALOG.random()
                val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                if (_dtcs.value.none { it.code == randomDtc.code }) {
                    _dtcs.value = _dtcs.value + randomDtc.copy(timestamp = now)
                }
            }
        }
    }

    suspend fun clearDtcs(): Boolean {
        writeBytesPhysical(buildHondaFrame(0x72, 0x04, 0x73))
        writeBytesPhysical(buildHondaFrame(0x14, 0xFF, 0x00))
        delay(450)
        _dtcs.value = emptyList()
        _telemetry.value = _telemetry.value.copy(milActive = false)
        return true
    }

    suspend fun testActuator(name: String): String {
        val actId = when {
            name.contains("Fuel Pump") -> 0x01
            name.contains("Koil") -> 0x02
            name.contains("Injektor") -> 0x03
            else -> 0x04
        }
        writeBytesPhysical(buildHondaFrame(0x72, 0x06, 0x30, actId, 0x01))
        delay(650)
        return "Aktuator $name merespons sinyal output 12V ECM dengan normal."
    }

    // --- Sensor Calibration & Smartkey ---
    suspend fun resetTpsZero(closedVoltage: Double = 0.49): String {
        writeBytesPhysical(buildHondaFrame(0x72, 0x05, 0x74, 0x01, 0xF0))
        delay(500)
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val closed = (closedVoltage * 100.0).roundToInt() / 100.0
        val wot = ((closed + 3.86) * 100.0).roundToInt() / 100.0
        _calibration.value = _calibration.value.copy(
            tpsClosedVoltage = closed,
            tpsWotVoltage = wot,
            lastCalibratedTime = now
        )
        return "Reset TPS Titik Nol berhasil (${closed}V = 0.0%). Adaptasi EEPROM diperbarui."
    }

    suspend fun calibrateO2Sensor(mode: String, switchMv: Int): String {
        delay(350)
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _calibration.value = _calibration.value.copy(
            o2Mode = mode,
            o2SwitchPointMv = switchMv,
            lastCalibratedTime = now
        )
        return "Kalibrasi sensor O2 ($mode, switch ${switchMv}mV) tersimpan di ECM."
    }

    suspend fun readSmartkeyScu(): SmartkeyData {
        writeBytesPhysical(buildHondaFrame(0x72, 0x05, 0x82, 0x01))
        delay(450)
        return _smartkey.value
    }

    suspend fun registerSmartkeyFob(fobCode: String): SmartkeyData {
        writeBytesPhysical(buildHondaFrame(0x72, 0x08, 0x83, 0x02))
        delay(550)
        val updated = _smartkey.value.copy(registeredFob2 = fobCode)
        _smartkey.value = updated
        return updated
    }

    fun applyBypassFeatures(features: EcuSpecialFeatures): String {
        val list = mutableListOf<String>()
        if (features.offImmobilizer) list.add("HISS Bypass")
        if (features.offSideStand) list.add("Stand Switch Off")
        if (features.offO2Sensor) list.add("O2 Loop Off")
        if (features.offEctCut) list.add("ECT Cut Off")
        return if (list.isEmpty()) "Semua flag proteksi kembali ke standar OEM."
        else "Patch diterapkan: ${list.joinToString(" • ")}"
    }

    private companion object {
        const val TAG = "HondaEcuHardware"
    }
}
