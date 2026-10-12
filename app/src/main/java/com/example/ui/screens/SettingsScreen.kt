package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRepository
import com.example.sync.SyncManager
import com.example.sync.SyncRole
import com.example.sync.SyncUiState
import com.example.ui.theme.PksHadirGreen
import com.example.ui.theme.PksOrangeDark
import com.example.ui.theme.PksOrangeLight
import com.example.ui.theme.PksOrangePrimary
import com.example.util.QrCodeUtil
import kotlinx.coroutines.launch

data class DayItem(val id: Int, val name: String)

@Composable
fun SettingsScreen(
    workDays: List<Int>,
    internshipSchedule: com.example.data.model.InternshipSchedule,
    syncState: SyncUiState,
    syncManager: SyncManager,
    repository: AttendanceRepository,
    onToggleWorkDay: (Int) -> Unit,
    onSaveSchedule: (startDate: String, endDate: String, workDays: List<Int>, timeIn: String, timeOut: String) -> Unit,
    onStartHost: (port: Int) -> Unit,
    onStopHost: () -> Unit,
    onConnectHost: (String) -> Unit,
    onReloadData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSyncTab by remember { mutableIntStateOf(if (syncState.isHostRunning) 0 else if (syncState.role == SyncRole.CLIENT) 1 else 0) }
    var hostAddressInput by remember { mutableStateOf(syncState.clientTargetHost.ifBlank { "" }) }
    var githubTargetInput by remember(syncState.githubRawUrl, syncState.githubGistId) {
        mutableStateOf(if (syncState.githubRawUrl.isNotBlank()) syncState.githubRawUrl else syncState.githubGistId)
    }
    var githubTokenInput by remember(syncState.githubToken) {
        mutableStateOf(syncState.githubToken)
    }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var startDateInput by remember(internshipSchedule.startDate) { mutableStateOf(internshipSchedule.startDate) }
    var endDateInput by remember(internshipSchedule.endDate) { mutableStateOf(internshipSchedule.endDate) }
    var timeInInput by remember(internshipSchedule.timeIn) { mutableStateOf(internshipSchedule.timeIn) }
    var timeOutInput by remember(internshipSchedule.timeOut) { mutableStateOf(internshipSchedule.timeOut) }
    val selectedWorkDays = remember(workDays) { mutableStateListOf<Int>().apply { addAll(workDays) } }

    val daysList = remember {
        listOf(
            DayItem(1, "Senin"),
            DayItem(2, "Selasa"),
            DayItem(3, "Rabu"),
            DayItem(4, "Kamis"),
            DayItem(5, "Jumat"),
            DayItem(6, "Sabtu"),
            DayItem(7, "Minggu")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pengaturan Aplikasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Sinkronisasi Multi-HP & Hari Kerja",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // Section 1: Multi-Device Sync
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("section_multi_device_sync")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PksOrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = PksOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sinkronisasi Antar HP (Multi-Device)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Bisa via Hotspot lokal atau GitHub Cloud di semua HP",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedSyncTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = PksOrangePrimary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedSyncTab == 0,
                        onClick = { selectedSyncTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Host Wi-Fi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedSyncTab == 1,
                        onClick = { selectedSyncTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Klien Wi-Fi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedSyncTab == 2,
                        onClick = { selectedSyncTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Public, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GitHub Cloud", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedSyncTab == 0) {
                    // Host Mode Tab
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Jadikan HP ini sebagai Server Utama. HP lain dapat langsung melakukan scan QR untuk terhubung.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )

                        val deviceIp = remember(syncState.hostIp) {
                            if (syncState.hostIp.isNotBlank()) syncState.hostIp else syncManager.getDeviceIpAddress()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "IP HP Ini (Local Wi-Fi / Hotspot):",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "$deviceIp:${syncState.hostPort}",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Switch(
                                checked = syncState.isHostRunning,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) onStartHost(8888) else onStopHost()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PksOrangePrimary
                                ),
                                modifier = Modifier.testTag("switch_host_server")
                            )
                        }

                        if (syncState.isHostRunning) {
                            // Display QR Code for other phones to scan
                            val syncQrText = syncManager.getSyncQrCodeString()
                            val qrBitmap = remember(syncQrText) {
                                QrCodeUtil.generateQrBitmap(syncQrText, 320, 320)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFF7ED))
                                    .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "QR Code Sinkronisasi HP Lain",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = "Arahkan kamera HP lain ke QR ini untuk menghubungkan langsung:",
                                    fontSize = 10.sp,
                                    color = Color(0xFFC2410C),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Sync",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = syncQrText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF7C2D12),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: ${syncState.connectedClientsCount} Klien tersinkron",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                } else {
                    // Client Mode Tab
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Hubungkan HP ini ke HP Server Host untuk menarik data peserta dan mengirim absensi secara otomatis.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )

                        OutlinedTextField(
                            value = hostAddressInput,
                            onValueChange = { hostAddressInput = it },
                            placeholder = { Text("Contoh: 192.168.1.50:8888", fontSize = 12.sp) },
                            label = { Text("Alamat IP HP Host") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_host_address")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onConnectHost(hostAddressInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = hostAddressInput.isNotBlank() && !syncState.isSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_sync_now")
                            ) {
                                if (syncState.isSyncing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Sinkron Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (syncState.isConnectedToHost) PksHadirGreen else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncState.lastSyncMessage,
                                fontSize = 11.sp,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }

                if (selectedSyncTab == 2) {
                    // GitHub Cloud Sync Tab
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Sinkronisasi absensi di semua HP melalui GitHub Gist / Raw JSON. Tetap terhubung di mana saja tanpa perlu satu Wi-Fi!",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )

                        Text(
                            text = "Link GitHub Raw / Gist ID:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        OutlinedTextField(
                            value = githubTargetInput,
                            onValueChange = {
                                githubTargetInput = it
                                syncManager.saveGithubConfig(it, githubTokenInput, if (it.startsWith("http")) it else "")
                            },
                            placeholder = { Text("Contoh: https://raw.githubusercontent.com/... atau ID Gist", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_github_target")
                        )

                        Text(
                            text = "GitHub Token / PAT (Opsional, untuk Admin mengunggah):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        OutlinedTextField(
                            value = githubTokenInput,
                            onValueChange = {
                                githubTokenInput = it
                                syncManager.saveGithubConfig(githubTargetInput, it, if (githubTargetInput.startsWith("http")) githubTargetInput else "")
                            },
                            placeholder = { Text("Contoh: ghp_xxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_github_token")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    syncManager.pullFromGitHub(githubTargetInput) { success, msg ->
                                        if (success) {
                                            onReloadData()
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !syncState.isGithubSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_pull_github")
                            ) {
                                if (syncState.isGithubSyncing) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tarik dari GitHub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    syncManager.pushToGitHub(githubTargetInput, githubTokenInput) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !syncState.isGithubSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_push_github")
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unggah ke GitHub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = repository.exportFullJson()
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("pks_absensi_github", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Format JSON berhasil disalin! Tempel di GitHub Gist/Repo.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("btn_copy_github_json")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin JSON Format GitHub", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Status message pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (syncState.lastGithubMessage.contains("Berhasil")) PksHadirGreen else Color(0xFF3B82F6))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncState.lastGithubMessage,
                                fontSize = 11.sp,
                                color = Color(0xFF334155)
                            )
                        }

                        // Tutorial Card for GitHub
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Panduan Absen Semua HP via GitHub", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF92400E))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. Admin/Ketua buat Gist di gist.github.com bernama 'pks_magang_absensi.json'.\n" +
                                            "2. Klik 'Salin JSON Format GitHub' di atas dan tempelkan ke Gist tersebut.\n" +
                                            "3. Bagikan link Raw Gist kepada seluruh anggota/peserta magang.\n" +
                                            "4. Di HP masing-masing, buka menu ini, tempel link, dan klik 'Tarik dari GitHub'.\n" +
                                            "5. Seluruh HP langsung tersinkronisasi data peserta, NIM, dan kehadiran!",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Backup JSON Share & Import
                Text(
                    text = "TRANSFER FILE CADANGAN (OFFLINE)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val json = repository.exportFullJson()
                                QrCodeUtil.shareCsv(context, json, "backup_pks_magang")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor Backup", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Impor Backup", fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 2: Internship Schedule & Working Days (Directly editable on phone!)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_internship_schedule_settings")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Jadwal & Periode Magang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Bisa diedit langsung di HP & tersinkron ke semua perangkat",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tanggal Mulai & Akhir Magang
                Text(
                    text = "PERIODE TANGGAL MAGANG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mulai Magang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = startDateInput,
                            onValueChange = { startDateInput = it },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_start_date")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Akhir Magang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = endDateInput,
                            onValueChange = { endDateInput = it },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_end_date")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Jam Masuk & Jam Keluar
                Text(
                    text = "JAM KERJA OPERASIONAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jam Masuk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = timeInInput,
                            onValueChange = { timeInInput = it },
                            placeholder = { Text("08:00", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_time_in")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jam Keluar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = timeOutInput,
                            onValueChange = { timeOutInput = it },
                            placeholder = { Text("17:00", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_time_out")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hari Kerja Sepekan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HARI KERJA DALAM SEPEKAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEDD5))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${selectedWorkDays.size} Hari Aktif / Pekan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9A3412)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedWorkDays.clear()
                            selectedWorkDays.addAll(listOf(1, 2, 3, 4, 5))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Text("5 Hari (Sen-Jum)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedWorkDays.clear()
                            selectedWorkDays.addAll(listOf(1, 2, 3, 4, 5, 6))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Text("6 Hari (Sen-Sab)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    daysList.forEach { day ->
                        val isChecked = selectedWorkDays.contains(day.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                .border(
                                    1.dp,
                                    if (isChecked) Color(0xFFFFEDD5) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isChecked) selectedWorkDays.remove(day.id) else selectedWorkDays.add(day.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (isChecked) Color(0xFF9A3412) else Color(0xFF475569)
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { check ->
                                    if (check) {
                                        if (!selectedWorkDays.contains(day.id)) selectedWorkDays.add(day.id)
                                    } else {
                                        selectedWorkDays.remove(day.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PksOrangePrimary,
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save button
                Button(
                    onClick = {
                        onSaveSchedule(
                            startDateInput,
                            endDateInput,
                            selectedWorkDays.toList(),
                            timeInInput,
                            timeOutInput
                        )
                        Toast.makeText(context, "Pengaturan jadwal magang berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_save_internship_schedule")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Pengaturan Jadwal Magang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Impor Data Cadangan JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Tempel teks JSON cadangan di bawah ini untuk memulihkan data peserta & absensi:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("{\"participants\": [...]}", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = repository.importFullJson(importText)
                            if (success) {
                                onReloadData()
                            }
                            Toast.makeText(
                                context,
                                if (success) "Data berhasil dipulihkan!" else "Format JSON tidak valid!",
                                Toast.LENGTH_SHORT
                            ).show()
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary)
                ) {
                    Text("Impor Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
