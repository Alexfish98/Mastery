package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiagnosticsScreen() {
    val context = LocalContext.current
    val config = LocalConfiguration.current

    // Battery information
    val batteryStatus: Intent? = remember {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(null, ifilter)
    }
    val batteryLevel: Int = remember(batteryStatus) {
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }
    val isCharging: Boolean = remember(batteryStatus) {
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    // Storage information
    val statFs = remember { StatFs(Environment.getDataDirectory().path) }
    val totalBytes = remember(statFs) { statFs.totalBytes }
    val freeBytes = remember(statFs) { statFs.availableBytes }
    val totalGb = String.format(Locale.US, "%.2f", totalBytes / (1024.0 * 1024.0 * 1024.0))
    val freeGb = String.format(Locale.US, "%.2f", freeBytes / (1024.0 * 1024.0 * 1024.0))

    // Network status
    val cm = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    val activeNet = cm.activeNetwork
    val caps = cm.getNetworkCapabilities(activeNet)
    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PermDeviceInformation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Device & System Diagnostics",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Hardware, OS, and telemetry monitoring",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Hardware & OS Card
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🤖 Hardware & Android OS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        HorizontalDivider()
                        DiagRow("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                        DiagRow("Device Model", Build.MODEL)
                        DiagRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        DiagRow("Hardware Board", Build.BOARD)
                        DiagRow("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
                    }
                }
            }

            // Display & Screen Card
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🖥️ Display Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        HorizontalDivider()
                        DiagRow("Screen Width (dp)", "${config.screenWidthDp} dp")
                        DiagRow("Screen Height (dp)", "${config.screenHeightDp} dp")
                        DiagRow("Display Density (DPI)", "${config.densityDpi} dpi")
                        DiagRow("Orientation", if (config.screenWidthDp < config.screenHeightDp) "Portrait" else "Landscape")
                    }
                }
            }

            // Storage & Battery Card
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔋 Battery & 💾 Storage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        HorizontalDivider()
                        DiagRow("Battery Percentage", if (batteryLevel >= 0) "$batteryLevel%" else "Unknown")
                        DiagRow("Charging Status", if (isCharging) "Charging ⚡" else "Discharging")
                        DiagRow("Total Internal Storage", "$totalGb GB")
                        DiagRow("Available Free Storage", "$freeGb GB")
                    }
                }
            }

            // Network Connectivity Card
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📶 Network Connectivity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        HorizontalDivider()
                        DiagRow("Internet Connected", if (isConnected) "Yes (Online)" else "No (Offline)")
                        DiagRow("Connection Type", when {
                            isWifi -> "Wi-Fi Network"
                            isCellular -> "Cellular Mobile Data"
                            isConnected -> "Ethernet / Other"
                            else -> "Offline (Full EUEE offline mode active)"
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}
