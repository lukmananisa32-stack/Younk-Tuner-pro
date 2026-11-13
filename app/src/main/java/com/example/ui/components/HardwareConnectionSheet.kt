package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.HardwareConnection
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.HondaRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareConnectionSheet(
    connection: HardwareConnection,
    usbDevices: List<UsbDevice>,
    onScanUsb: () -> Unit,
    onConnectUsb: (UsbDevice, Int) -> Unit,
    onGetPairedBluetooth: () -> List<BluetoothDevice>,
    onConnectBluetooth: (BluetoothDevice) -> Unit,
    onSwitchBench: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedBaud by remember { mutableIntStateOf(10400) }
    var pairedBt by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var btStatusMsg by remember { mutableStateOf<String?>(null) }

    val btPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            pairedBt = onGetPairedBluetooth()
            btStatusMsg = if (pairedBt.isEmpty()) "Tidak ada perangkat OBD2 terpasang (pair di Pengaturan HP)." else null
        } else {
            btStatusMsg = "Izin Bluetooth diperlukan untuk mencari dongle OBD2/ELM327."
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Koneksi Hardware ECU Honda",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Surface(
                color = CarbonElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CarbonBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (connection.isPhysicalDevice) "HARDWARE FISIK TERHUBUNG" else "MODE BENCH TELEMETRY",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (connection.isPhysicalDevice) EmeraldOk else TelemetryCyan
                    )
                    Text(text = connection.deviceName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(text = connection.protocol, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        text = "Frame Terakhir: ${connection.lastPacketHex}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetryCyan
                    )
                }
            }

            // Baud Rate Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(10400 to "K-Line 10.4K", 500000 to "CAN-Bus 500K").forEach { (baud, label) ->
                    OutlinedButton(
                        onClick = { selectedBaud = baud },
                        border = BorderStroke(1.dp, if (selectedBaud == baud) HondaRed else CarbonBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, color = if (selectedBaud == baud) HondaRed else Color.White)
                    }
                }
            }

            // USB OTG Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Usb, contentDescription = null, tint = TelemetryCyan)
                    Text("USB OTG Adapter (FTDI/CH340/CP2102)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                IconButton(onClick = onScanUsb, modifier = Modifier.testTag("scan_usb_button")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Scan USB", tint = TelemetryCyan)
                }
            }

            if (usbDevices.isEmpty()) {
                Text(
                    text = "Belum ada kabel USB OTG terdeteksi. Hubungkan konverter USB-OTG + kabel DLC 4-pin Honda lalu tekan Refresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                usbDevices.forEach { dev ->
                    Surface(
                        color = CarbonElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TelemetryCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onConnectUsb(dev, selectedBaud)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dev.productName ?: "USB Serial Adapter", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "VID: 0x${dev.vendorId.toString(16).uppercase()} PID: 0x${dev.productId.toString(16).uppercase()}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text("SAMBUNG", color = TelemetryCyan, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Bluetooth OBD2 Section
            Button(
                onClick = {
                    val hasAll = btPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasAll) {
                        pairedBt = onGetPairedBluetooth()
                        btStatusMsg = if (pairedBt.isEmpty()) "Tidak ada perangkat OBD2 Bluetooth yang sudah dipasangkan." else null
                    } else {
                        btPermLauncher.launch(btPermissions)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CarbonElevated, contentColor = Color.White),
                border = BorderStroke(1.dp, CarbonBorder),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scan_bt_button")
            ) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = TelemetryCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Cari Dongle Bluetooth OBD2 / ESP32")
            }

            btStatusMsg?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            pairedBt.forEach { btDev ->
                @SuppressLint("MissingPermission")
                val btName = btDev.name ?: "Bluetooth OBD2"
                Surface(
                    color = CarbonElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CarbonBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onConnectBluetooth(btDev)
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(btName, color = Color.White, style = MaterialTheme.typography.labelLarge)
                            Text(btDev.address, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text("CONNECT SPP", color = EmeraldOk, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onSwitchBench()
                    onDismiss()
                },
                border = BorderStroke(1.dp, HondaRed),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("bench_mode_button")
            ) {
                Icon(Icons.Filled.DeveloperBoard, contentDescription = null, tint = HondaRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Gunakan Mode Bench Telemetry (Tanpa Kabel)", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
