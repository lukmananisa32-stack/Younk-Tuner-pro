package com.example.ui.navigation

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.hardware.HondaEcuHardwareManager
import com.example.data.repository.AiTuningRepository
import com.example.data.repository.TuningCloudRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.HardwareConnectionSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosaScreen
import com.example.ui.screens.DynoDatabaseScreen
import com.example.ui.screens.FlashSpecialScreen
import com.example.ui.screens.RemapScreen
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.HondaRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.YounkTunerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class TunerTab(val label: String, val icon: ImageVector) {
    LIVE("Live", Icons.Filled.Speed),
    REMAP("Remap", Icons.Filled.Tune),
    FLASH("Flash", Icons.Filled.FlashOn),
    DIAGNOSA("DTC", Icons.Filled.Warning),
    DYNO_DB("Dyno/DB", Icons.Filled.Timeline)
}

@Composable
fun AppNavigation() {
    val authState by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = authState
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            YounkTunerMainHost(currentUserId = user.uid)
        }
    }
}

@Composable
private fun YounkTunerMainHost(currentUserId: String) {
    val viewModel: YounkTunerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                YounkTunerViewModel(
                    cloudRepo = TuningCloudRepository(firestore),
                    aiRepo = AiTuningRepository(),
                    hardware = HondaEcuHardwareManager(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val activeEcu by viewModel.activeEcu.collectAsStateWithLifecycle()
    val tuningTables by viewModel.tuningTables.collectAsStateWithLifecycle()
    val specialFeatures by viewModel.specialFeatures.collectAsStateWithLifecycle()
    val checksumHex by viewModel.checksumHex.collectAsStateWithLifecycle()
    val dynoSummary by viewModel.dynoSummary.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val actuatorResult by viewModel.actuatorResult.collectAsStateWithLifecycle()

    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val dtcs by viewModel.dtcs.collectAsStateWithLifecycle()
    val calibration by viewModel.calibration.collectAsStateWithLifecycle()
    val smartkey by viewModel.smartkey.collectAsStateWithLifecycle()
    val flashState by viewModel.flashState.collectAsStateWithLifecycle()
    val discoveredUsb by viewModel.discoveredUsbDevices.collectAsStateWithLifecycle()
    val savedMapsState by viewModel.savedMapsState.collectAsStateWithLifecycle()
    val dynoRunsState by viewModel.dynoRunsState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(TunerTab.LIVE) }
    var dynoSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showConnectionSheet by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    BackHandler(enabled = currentTab != TunerTab.LIVE) {
        currentTab = TunerTab.LIVE
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isCompact = maxWidth < 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (!isCompact) {
                NavigationRail(
                    containerColor = CarbonSurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    TunerTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = HondaRed,
                                selectedTextColor = Color.White,
                                indicatorColor = HondaRed.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            Scaffold(
                containerColor = ObsidianBg,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    YounkTunerTopBar(
                        ecuName = activeEcu.name,
                        checksumHex = checksumHex,
                        isPhysical = connection.isPhysicalDevice,
                        onOpenConnection = {
                            viewModel.scanUsbDevices()
                            showConnectionSheet = true
                        },
                        onSignOut = {
                            signOut(credentialManager = credentialManager, scope = scope)
                        }
                    )
                },
                bottomBar = {
                    if (isCompact) {
                        NavigationBar(
                            containerColor = CarbonSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            TunerTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = HondaRed,
                                        selectedTextColor = Color.White,
                                        indicatorColor = HondaRed.copy(alpha = 0.2f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        TunerTab.LIVE -> DashboardScreen(
                            activeEcu = activeEcu,
                            revLimiterRpm = tuningTables.revLimiterRpm,
                            telemetry = telemetry,
                            connection = connection,
                            onToggleEngine = { viewModel.toggleEngine() },
                            onThrottleChange = { viewModel.setBenchThrottle(it) },
                            onOpenConnectionSheet = {
                                viewModel.scanUsbDevices()
                                showConnectionSheet = true
                            },
                            onOpenDatabase = {
                                dynoSubTab = 1
                                currentTab = TunerTab.DYNO_DB
                            }
                        )

                        TunerTab.REMAP -> RemapScreen(
                            activeEcu = activeEcu,
                            tables = tuningTables,
                            checksumHex = checksumHex,
                            aiState = aiState,
                            onApplyStrategy = { viewModel.applyStrategy(it) },
                            onAdjustCell = { isFuel, r, c, delta -> viewModel.adjustTableCell(isFuel, r, c, delta) },
                            onSmoothTable = { viewModel.smoothActiveTable(it) },
                            onUpdateLimits = { lim, launch, afr -> viewModel.updateLimiterSettings(lim, launch, afr) },
                            onRunAiTuning = { mods, ron, goal -> viewModel.runAiTuning(mods, ron, goal) },
                            onApplyAiRecommendation = { viewModel.applyAiRecommendation(it) },
                            onSaveMapCloud = { name, notes -> viewModel.saveCurrentMapToCloud(name, notes) },
                            onBuildRomBytes = { viewModel.buildCurrentRomBytes() },
                            onBuildXdfXml = { viewModel.buildCurrentXdfXml() },
                            onShowToast = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )

                        TunerTab.FLASH -> FlashSpecialScreen(
                            activeEcu = activeEcu,
                            connection = connection,
                            checksumHex = checksumHex,
                            flashState = flashState,
                            calibration = calibration,
                            specialFeatures = specialFeatures,
                            smartkey = smartkey,
                            onFlashEcu = { fullRom -> viewModel.flashEcu(fullRom) },
                            onRecoveryUnbrick = { viewModel.recoveryUnbrick() },
                            onResetTpsZero = { viewModel.resetTpsZero() },
                            onCalibrateO2 = { mode, mv -> viewModel.calibrateO2(mode, mv) },
                            onUpdateFeatures = { viewModel.updateSpecialFeatures(it) },
                            onReadSmartkey = { viewModel.readSmartkey() },
                            onRegisterFob = { viewModel.registerSmartkeyFob(it) }
                        )

                        TunerTab.DIAGNOSA -> DiagnosaScreen(
                            activeEcu = activeEcu,
                            dtcs = dtcs,
                            actuatorResult = actuatorResult,
                            onScanDtcs = { simulate -> viewModel.scanDtcs(simulate) },
                            onClearDtcs = { viewModel.clearDtcs() },
                            onTestActuator = { name -> viewModel.testActuator(name) }
                        )

                        TunerTab.DYNO_DB -> DynoDatabaseScreen(
                            selectedSubTab = dynoSubTab,
                            onSubTabChange = { dynoSubTab = it },
                            activeEcu = activeEcu,
                            dynoSummary = dynoSummary,
                            dynoRunsState = dynoRunsState,
                            savedMapsState = savedMapsState,
                            onRunDynoPull = { viewModel.runAndSaveDynoPull() },
                            onDeleteDynoRun = { viewModel.deleteDynoRun(it) },
                            onSelectEcu = { ecu ->
                                viewModel.selectEcu(ecu)
                                currentTab = TunerTab.LIVE
                            },
                            onLoadCloudMap = { map ->
                                viewModel.loadCloudMap(map)
                                currentTab = TunerTab.REMAP
                            },
                            onDeleteCloudMap = { viewModel.deleteCloudMap(it) }
                        )
                    }
                }
            }
        }
    }

    if (showConnectionSheet) {
        HardwareConnectionSheet(
            connection = connection,
            usbDevices = discoveredUsb,
            onScanUsb = { viewModel.scanUsbDevices() },
            onConnectUsb = { dev, baud -> viewModel.connectUsb(dev, baud) },
            onGetPairedBluetooth = { viewModel.getPairedBluetoothDevices() },
            onConnectBluetooth = { dev -> viewModel.connectBluetooth(dev) },
            onSwitchBench = { viewModel.switchToBenchMode() },
            onDismiss = { showConnectionSheet = false }
        )
    }
}

@Composable
private fun YounkTunerTopBar(
    ecuName: String,
    checksumHex: String,
    isPhysical: Boolean,
    onOpenConnection: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = CarbonSurface,
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = HondaRed,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "YOUNK TUNER PRO",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "$ecuName • $checksumHex",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetryCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CarbonElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isPhysical) EmeraldOk else TelemetryCyan),
                    modifier = Modifier
                        .clickable(onClick = onOpenConnection)
                        .testTag("top_connection_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Usb,
                            contentDescription = stringResource(R.string.cd_hardware_status),
                            tint = if (isPhysical) EmeraldOk else TelemetryCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPhysical) "ECU" else "USB/BT",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(R.string.cd_sign_out),
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}
