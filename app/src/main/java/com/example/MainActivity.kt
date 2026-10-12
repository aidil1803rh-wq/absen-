package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Participant
import com.example.data.model.ParticipantWithAttendance
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.dialogs.AddParticipantDialog
import com.example.ui.dialogs.AlertModalDialog
import com.example.ui.dialogs.DailyChecklistDialog
import com.example.ui.dialogs.EditParticipantDialog
import com.example.ui.dialogs.KokardeDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PesertaScreen
import com.example.ui.screens.RekapScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PksOrangePrimary
import com.example.util.QrCodeUtil
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentTab by viewModel.currentTab.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val todayParticipants by viewModel.todayParticipants.collectAsState()
    val allParticipants by viewModel.allParticipants.collectAsState()
    val rekapParticipants by viewModel.rekapParticipants.collectAsState()
    val selectedRekapDate by viewModel.selectedRekapDate.collectAsState()
    val workDays by viewModel.workDays.collectAsState()
    val internshipSchedule by viewModel.internshipSchedule.collectAsState()
    val syncState by viewModel.syncUiState.collectAsState()
    val alertNotification by viewModel.alertNotification.collectAsState()
    val selectedParticipantForKokarde by viewModel.selectedParticipantForKokarde.collectAsState()
    val selectedParticipantForChecklist by viewModel.selectedParticipantForChecklist.collectAsState()
    val isAddParticipantDialogOpen by viewModel.isAddParticipantDialogOpen.collectAsState()
    val selectedParticipantForEdit by viewModel.selectedParticipantForEdit.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppHeader(
                syncState = syncState,
                onSettingsClick = {
                    if (currentTab == AppTab.SETTINGS) {
                        viewModel.switchTab(AppTab.HOME)
                    } else {
                        viewModel.switchTab(AppTab.SETTINGS)
                    }
                }
            )
        },
        bottomBar = {
            if (currentTab != AppTab.SETTINGS) {
                Surface(
                    modifier = Modifier.shadow(8.dp),
                    color = Color.White
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.HOME,
                            onClick = { viewModel.switchTab(AppTab.HOME) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Beranda",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Beranda", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PksOrangePrimary,
                                selectedTextColor = PksOrangePrimary,
                                indicatorColor = Color(0xFFFFEDD5),
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.SCAN,
                            onClick = { viewModel.switchTab(AppTab.SCAN) },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (currentTab == AppTab.SCAN) PksOrangePrimary else Color(0xFFFFEDD5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = if (currentTab == AppTab.SCAN) Color.White else PksOrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = { Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = PksOrangePrimary,
                                indicatorColor = Color.Transparent,
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_item_scan")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.PESERTA,
                            onClick = { viewModel.switchTab(AppTab.PESERTA) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = "Peserta",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Peserta", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PksOrangePrimary,
                                selectedTextColor = PksOrangePrimary,
                                indicatorColor = Color(0xFFFFEDD5),
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_item_peserta")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.REKAP,
                            onClick = { viewModel.switchTab(AppTab.REKAP) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "Rekap",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text("Rekap", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PksOrangePrimary,
                                selectedTextColor = PksOrangePrimary,
                                indicatorColor = Color(0xFFFFEDD5),
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("nav_item_rekap")
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
                AppTab.HOME -> {
                    HomeScreen(
                        stats = todayStats,
                        participants = todayParticipants,
                        schedule = internshipSchedule,
                        onScanClick = { viewModel.switchTab(AppTab.SCAN) },
                        onViewRekapClick = { viewModel.switchTab(AppTab.REKAP) },
                        onAddParticipantClick = { viewModel.openAddParticipantDialog() },
                        onParticipantClick = { item -> viewModel.openChecklistDialog(item) }
                    )
                }

                AppTab.SCAN -> {
                    ScanScreen(
                        participants = allParticipants,
                        onScanParticipant = { id, status ->
                            viewModel.recordAttendance(id, status)
                        },
                        onCheckOutParticipant = { id ->
                            viewModel.recordCheckOut(id)
                        },
                        onOpenChecklistForId = { id: String ->
                            val item = todayParticipants.find { it.participant.id == id }
                                ?: rekapParticipants.find { it.participant.id == id }
                                ?: allParticipants.find { it.id == id }?.let { p ->
                                    ParticipantWithAttendance(
                                        participant = p,
                                        todayStatus = "",
                                        todayTime = "-",
                                        todayTimeOut = "-",
                                        isQuranRead = false,
                                        quranSurahJuz = "",
                                        dailyActivities = emptyList(),
                                        activityNotes = "",
                                        totalAlfa = 0
                                    )
                                }
                            if (item != null) {
                                viewModel.openChecklistDialog(item)
                            }
                        },
                        onSyncHostScanned = { hostStr ->
                            viewModel.connectAndSyncHost(hostStr)
                        }
                    )
                }

                AppTab.PESERTA -> {
                    PesertaScreen(
                        participantsWithStats = todayParticipants,
                        onAddParticipantClick = { viewModel.openAddParticipantDialog() },
                        onEditParticipant = { p -> viewModel.openEditParticipant(p) },
                        onOpenKokarde = { p -> viewModel.openKokarde(p) },
                        onDeleteParticipant = { id -> viewModel.deleteParticipant(id) }
                    )
                }

                AppTab.REKAP -> {
                    RekapScreen(
                        selectedDate = selectedRekapDate,
                        participantsWithStats = rekapParticipants,
                        onSelectDate = { date -> viewModel.setSelectedRekapDate(date) },
                        onUpdateStatus = { id, status, date ->
                            viewModel.recordAttendance(id, status, date)
                        },
                        onClearAttendance = { id, date ->
                            viewModel.clearAttendance(id, date)
                        },
                        onOpenChecklist = { item ->
                            viewModel.openChecklistDialog(item)
                        },
                        onExportCsv = { date ->
                            scope.launch {
                                val schedule = viewModel.repository.getInternshipSchedule()
                                val participants = viewModel.repository.participantDao.getAllParticipantsSnapshot()
                                val records = viewModel.repository.attendanceDao.getAttendanceForDateSnapshot(date)
                                QrCodeUtil.generateAndShareCsv(context, schedule, participants, records, date)
                            }
                        },
                        onExportPdf = { date ->
                            scope.launch {
                                val schedule = viewModel.repository.getInternshipSchedule()
                                val participants = viewModel.repository.participantDao.getAllParticipantsSnapshot()
                                val records = viewModel.repository.attendanceDao.getAttendanceForDateSnapshot(date)
                                QrCodeUtil.generateAndSharePdf(context, schedule, participants, records, date)
                            }
                        },
                        onExportAllCsv = {
                            scope.launch {
                                val schedule = viewModel.repository.getInternshipSchedule()
                                val participants = viewModel.repository.participantDao.getAllParticipantsSnapshot()
                                val records = viewModel.repository.attendanceDao.getAllAttendanceSnapshot()
                                QrCodeUtil.generateAndShareCsv(context, schedule, participants, records, null)
                            }
                        },
                        onExportAllPdf = {
                            scope.launch {
                                val schedule = viewModel.repository.getInternshipSchedule()
                                val participants = viewModel.repository.participantDao.getAllParticipantsSnapshot()
                                val records = viewModel.repository.attendanceDao.getAllAttendanceSnapshot()
                                QrCodeUtil.generateAndSharePdf(context, schedule, participants, records, null)
                            }
                        }
                    )
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        workDays = workDays,
                        internshipSchedule = internshipSchedule,
                        syncState = syncState,
                        syncManager = viewModel.syncManager,
                        repository = viewModel.repository,
                        onToggleWorkDay = { day -> viewModel.toggleWorkDay(day) },
                        onSaveSchedule = { start, end, days, timeIn, timeOut ->
                            viewModel.saveInternshipSchedule(start, end, days, timeIn, timeOut)
                        },
                        onStartHost = { port -> viewModel.startHostServer(port) },
                        onStopHost = { viewModel.stopHostServer() },
                        onConnectHost = { host -> viewModel.connectAndSyncHost(host) },
                        onReloadData = { viewModel.reloadInternshipSchedule() },
                        onBack = { viewModel.switchTab(AppTab.HOME) }
                    )
                }
            }

            // Dialogs
            if (isAddParticipantDialogOpen) {
                AddParticipantDialog(
                    onDismiss = { viewModel.closeAddParticipantDialog() },
                    onSave = { name, nim, div, uni, nick ->
                        viewModel.addParticipant(name, nim, div, uni, nick)
                    }
                )
            }

            selectedParticipantForEdit?.let { p ->
                EditParticipantDialog(
                    participant = p,
                    onDismiss = { viewModel.closeEditParticipant() },
                    onSave = { id, name, nim, div, uni, nick ->
                        viewModel.updateParticipant(id, name, nim, div, uni, nick)
                    }
                )
            }

            selectedParticipantForKokarde?.let { p ->
                KokardeDialog(
                    participant = p,
                    onDismiss = { viewModel.closeKokarde() }
                )
            }

            selectedParticipantForChecklist?.let { item ->
                DailyChecklistDialog(
                    item = item,
                    dateStr = if (currentTab == AppTab.REKAP) selectedRekapDate else QrCodeUtil.getTodayDateString(),
                    onDismiss = { viewModel.closeChecklistDialog() },
                    onClearAttendance = {
                        val targetDate = if (currentTab == AppTab.REKAP) selectedRekapDate else QrCodeUtil.getTodayDateString()
                        viewModel.clearAttendance(item.participant.id, targetDate)
                    },
                    onSave = { st, timeIn, timeOut, isQuran, quranSurah, acts, notes ->
                        val targetDate = if (currentTab == AppTab.REKAP) selectedRekapDate else QrCodeUtil.getTodayDateString()
                        viewModel.updateDailyChecklist(
                            participantId = item.participant.id,
                            dateStr = targetDate,
                            status = st,
                            timeIn = timeIn,
                            timeOut = timeOut,
                            isQuranRead = isQuran,
                            quranSurahJuz = quranSurah,
                            activities = acts,
                            notes = notes
                        )
                    }
                )
            }

            alertNotification?.let { notif ->
                AlertModalDialog(
                    notification = notif,
                    onDismiss = { viewModel.dismissAlert() }
                )
            }
        }
    }
}
