package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyAttendanceStats
import com.example.data.model.InternshipSchedule
import com.example.data.model.Participant
import com.example.data.model.ParticipantWithAttendance
import com.example.sync.SyncManager
import com.example.sync.SyncRole
import com.example.sync.SyncUiState
import com.example.util.QrCodeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    HOME, SCAN, PESERTA, REKAP, SETTINGS
}

data class AlertNotification(
    val title: String,
    val message: String,
    val type: AlertType = AlertType.INFO,
    val isVisible: Boolean = true
)

enum class AlertType {
    SUCCESS, ERROR, WARNING, INFO
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = AttendanceRepository(db.participantDao(), db.attendanceDao(), db.settingDao())
    val syncManager = SyncManager(application, repository, viewModelScope)

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedRekapDate = MutableStateFlow(QrCodeUtil.getTodayDateString())
    val selectedRekapDate: StateFlow<String> = _selectedRekapDate.asStateFlow()

    val todayStats: StateFlow<DailyAttendanceStats> = repository.getDailyStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailyAttendanceStats(QrCodeUtil.getTodayDateString(), 0, 0, 0, 0, 0)
        )

    val todayParticipants: StateFlow<List<ParticipantWithAttendance>> = repository.getTodayParticipantsWithAttendance()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val rekapParticipants: StateFlow<List<ParticipantWithAttendance>> = _selectedRekapDate
        .flatMapLatest { date -> repository.getTodayParticipantsWithAttendance(date) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allParticipants: StateFlow<List<Participant>> = repository.getParticipants()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _workDays = MutableStateFlow<List<Int>>(listOf(1, 2, 3, 4, 5))
    val workDays: StateFlow<List<Int>> = _workDays.asStateFlow()

    private val _internshipSchedule = MutableStateFlow(InternshipSchedule())
    val internshipSchedule: StateFlow<InternshipSchedule> = _internshipSchedule.asStateFlow()

    val syncUiState: StateFlow<SyncUiState> = syncManager.uiState

    private val _alertNotification = MutableStateFlow<AlertNotification?>(null)
    val alertNotification: StateFlow<AlertNotification?> = _alertNotification.asStateFlow()

    private val _selectedParticipantForKokarde = MutableStateFlow<Participant?>(null)
    val selectedParticipantForKokarde: StateFlow<Participant?> = _selectedParticipantForKokarde.asStateFlow()

    private val _selectedParticipantForChecklist = MutableStateFlow<ParticipantWithAttendance?>(null)
    val selectedParticipantForChecklist: StateFlow<ParticipantWithAttendance?> = _selectedParticipantForChecklist.asStateFlow()

    private val _isAddParticipantDialogOpen = MutableStateFlow(false)
    val isAddParticipantDialogOpen: StateFlow<Boolean> = _isAddParticipantDialogOpen.asStateFlow()

    private val _selectedParticipantForEdit = MutableStateFlow<Participant?>(null)
    val selectedParticipantForEdit: StateFlow<Participant?> = _selectedParticipantForEdit.asStateFlow()

    init {
        viewModelScope.launch {
            _workDays.value = repository.getWorkDays()
            _internshipSchedule.value = repository.getInternshipSchedule()
        }
    }

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSelectedRekapDate(date: String) {
        _selectedRekapDate.value = date
    }

    fun showAlert(title: String, message: String, type: AlertType = AlertType.INFO) {
        _alertNotification.value = AlertNotification(title, message, type, true)
    }

    fun dismissAlert() {
        _alertNotification.value = null
    }

    fun openKokarde(participant: Participant) {
        _selectedParticipantForKokarde.value = participant
    }

    fun closeKokarde() {
        _selectedParticipantForKokarde.value = null
    }

    fun openChecklistDialog(item: ParticipantWithAttendance) {
        _selectedParticipantForChecklist.value = item
    }

    fun closeChecklistDialog() {
        _selectedParticipantForChecklist.value = null
    }

    fun openAddParticipantDialog() {
        _isAddParticipantDialogOpen.value = true
    }

    fun closeAddParticipantDialog() {
        _isAddParticipantDialogOpen.value = false
    }

    fun openEditParticipant(participant: Participant) {
        _selectedParticipantForEdit.value = participant
    }

    fun closeEditParticipant() {
        _selectedParticipantForEdit.value = null
    }

    fun updateParticipant(id: String, name: String, nim: String, division: String, university: String, nickname: String = "") {
        if (name.isBlank() || division.isBlank()) {
            showAlert("Peringatan", "Nama dan Divisi wajib diisi!", AlertType.WARNING)
            return
        }
        viewModelScope.launch {
            try {
                val updated = repository.updateParticipant(id, name, nim, division, university, nickname)
                closeEditParticipant()
                showAlert("Berhasil", "Data peserta ${updated.name} berhasil diperbarui.", AlertType.SUCCESS)
            } catch (e: Exception) {
                showAlert("Gagal", e.localizedMessage ?: "Gagal memperbarui data peserta.", AlertType.ERROR)
            }
        }
    }


    fun recordAttendance(
        participantId: String,
        status: String,
        dateStr: String = QrCodeUtil.getTodayDateString(),
        timeStr: String = if (status == "Hadir") QrCodeUtil.getCurrentTimeString() else "-"
    ) {
        viewModelScope.launch {
            val result = repository.recordAttendance(participantId, status, dateStr, timeStr)
            result.onSuccess { participant ->
                showAlert(
                    title = "Absensi Masuk Tercatat!",
                    message = "Peserta ${participant.name} ($participantId) berhasil dicatat '$status' jam $timeStr.",
                    type = AlertType.SUCCESS
                )

                // Push to Host server if connected in client mode
                if (syncUiState.value.role == SyncRole.CLIENT && syncUiState.value.isConnectedToHost) {
                    syncManager.pushAttendanceToHost(
                        AttendanceRecord(
                            participantId = participantId,
                            date = dateStr,
                            status = status,
                            time = timeStr
                        )
                    )
                }

                // Check Yellow card
                val alfaCount = repository.countAlfa(participantId)
                if (alfaCount >= 3) {
                    showAlert(
                        title = "PERINGATAN KARTU KUNING 🟨",
                        message = "Peserta magang '${participant.name}' telah mencapai $alfaCount kali Alfa! Harap segera diberi pembinaan.",
                        type = AlertType.WARNING
                    )
                }
            }.onFailure { err ->
                showAlert(
                    title = "Gagal Mencatat Absensi",
                    message = err.localizedMessage ?: "Terjadi kesalahan.",
                    type = AlertType.ERROR
                )
            }
        }
    }

    fun recordCheckOut(participantId: String, dateStr: String = QrCodeUtil.getTodayDateString()) {
        viewModelScope.launch {
            val timeOutStr = QrCodeUtil.getCurrentTimeString()
            val result = repository.recordCheckOut(participantId, dateStr, timeOutStr)
            result.onSuccess { participant ->
                showAlert(
                    title = "Absen Keluar Berhasil! 👋",
                    message = "Peserta ${participant.name} ($participantId) telah absen pulang pukul $timeOutStr. Selamat beristirahat!",
                    type = AlertType.SUCCESS
                )

                if (syncUiState.value.role == SyncRole.CLIENT && syncUiState.value.isConnectedToHost) {
                    syncManager.pushAttendanceToHost(
                        AttendanceRecord(
                            participantId = participantId,
                            date = dateStr,
                            status = "Hadir",
                            time = "-",
                            timeOut = timeOutStr
                        )
                    )
                }
            }.onFailure { err ->
                showAlert(
                    title = "Gagal Absen Keluar",
                    message = err.localizedMessage ?: "Terjadi kesalahan.",
                    type = AlertType.ERROR
                )
            }
        }
    }

    fun updateDailyChecklist(
        participantId: String,
        dateStr: String,
        status: String,
        timeIn: String,
        timeOut: String,
        isQuranRead: Boolean,
        quranSurahJuz: String,
        activities: List<String>,
        notes: String
    ) {
        viewModelScope.launch {
            val result = repository.recordAttendance(
                participantId = participantId,
                status = status,
                dateStr = dateStr,
                timeStr = timeIn,
                timeOutStr = timeOut,
                isQuranRead = isQuranRead,
                quranSurahJuz = quranSurahJuz,
                dailyActivities = activities,
                activityNotes = notes
            )
            closeChecklistDialog()
            result.onSuccess { participant ->
                val quranText = if (isQuranRead) "✅ Membaca Al-Qur'an ($quranSurahJuz)" else "Belum Tilawah"
                showAlert(
                    title = "Logbook & Presensi Disimpan!",
                    message = "${participant.name}: Status $status, Masuk $timeIn, Keluar $timeOut. $quranText",
                    type = AlertType.SUCCESS
                )

                if (syncUiState.value.role == SyncRole.CLIENT && syncUiState.value.isConnectedToHost) {
                    syncManager.pushAttendanceToHost(
                        AttendanceRecord(
                            participantId = participantId,
                            date = dateStr,
                            status = status,
                            time = timeIn,
                            timeOut = timeOut,
                            isQuranRead = isQuranRead,
                            quranSurahJuz = quranSurahJuz,
                            dailyActivities = activities.joinToString(";"),
                            activityNotes = notes
                        )
                    )
                }
            }.onFailure { err ->
                showAlert(
                    title = "Gagal Menyimpan",
                    message = err.localizedMessage ?: "Terjadi kesalahan.",
                    type = AlertType.ERROR
                )
            }
        }
    }

    fun addParticipant(name: String, nim: String = "", division: String, university: String, nickname: String = "") {
        if (name.isBlank() || division.isBlank() || university.isBlank()) {
            showAlert("Peringatan", "Nama, Divisi, dan Kampus wajib diisi!", AlertType.WARNING)
            return
        }
        viewModelScope.launch {
            val created = repository.addParticipant(name, nim, division, university, nickname)
            closeAddParticipantDialog()
            val nimInfo = if (created.nim.isNotBlank()) " (NIM: ${created.nim})" else ""
            showAlert("Berhasil", "Peserta magang baru ${created.name}$nimInfo berhasil didaftarkan.", AlertType.SUCCESS)
        }
    }

    fun pullFromGitHub(customTarget: String? = null, onResult: ((Boolean, String) -> Unit)? = null) {
        syncManager.pullFromGitHub(customTarget, onResult)
    }

    fun pushToGitHub(gistId: String? = null, token: String? = null, onResult: ((Boolean, String) -> Unit)? = null) {
        syncManager.pushToGitHub(gistId, token, onResult)
    }

    fun saveGithubConfig(gistId: String, token: String, rawUrl: String) {
        syncManager.saveGithubConfig(gistId, token, rawUrl)
    }

    fun deleteParticipant(id: String) {
        viewModelScope.launch {
            repository.deleteParticipant(id)
            showAlert("Dihapus", "Peserta magang dengan ID $id telah dihapus.", AlertType.INFO)
        }
    }

    fun toggleWorkDay(day: Int) {
        viewModelScope.launch {
            val current = _workDays.value.toMutableList()
            if (current.contains(day)) {
                current.remove(day)
            } else {
                current.add(day)
            }
            repository.setWorkDays(current)
            _workDays.value = current
            showAlert("Pengaturan Disimpan", "Hari operasional magang berhasil diperbarui.", AlertType.SUCCESS)
        }
    }

    fun startHostServer(port: Int = 8888) {
        syncManager.startHost(port)
        showAlert("Server Host Aktif", "HP ini sekarang menjadi Host di port $port. HP lain dapat menghubungkan untuk sinkron.", AlertType.INFO)
    }

    fun stopHostServer() {
        syncManager.stopHost()
        showAlert("Server Berhenti", "Host server telah dinonaktifkan.", AlertType.INFO)
    }

    fun clearAttendance(participantId: String, dateStr: String = QrCodeUtil.getTodayDateString()) {
        viewModelScope.launch {
            val result = repository.clearAttendance(participantId, dateStr)
            result.onSuccess { participant ->
                showAlert(
                    title = "Keterangan Absen Dikosongkan",
                    message = "Status & catatan presensi ${participant.name} tanggal $dateStr berhasil dikosongkan.",
                    type = AlertType.INFO
                )
            }.onFailure { err ->
                showAlert("Gagal Mengosongkan", err.localizedMessage ?: "Terjadi kesalahan.", AlertType.ERROR)
            }
        }
    }

    fun saveInternshipSchedule(
        startDate: String,
        endDate: String,
        workDays: List<Int>,
        timeIn: String,
        timeOut: String
    ) {
        viewModelScope.launch {
            repository.saveInternshipSchedule(startDate, endDate, workDays, timeIn, timeOut)
            _workDays.value = workDays
            _internshipSchedule.value = repository.getInternshipSchedule()
            showAlert(
                title = "Pengaturan Jadwal Disimpan!",
                message = "Periode magang, jam masuk ($timeIn), jam keluar ($timeOut), dan ${workDays.size} hari kerja berhasil disimpan.",
                type = AlertType.SUCCESS
            )
        }
    }

    fun reloadInternshipSchedule() {
        viewModelScope.launch {
            _workDays.value = repository.getWorkDays()
            _internshipSchedule.value = repository.getInternshipSchedule()
        }
    }

    fun connectAndSyncHost(hostAddress: String) {
        syncManager.setClientTarget(hostAddress)
        syncManager.syncWithHost(hostAddress) { success, msg ->
            if (success) {
                reloadInternshipSchedule()
            }
            showAlert(
                title = if (success) "Sinkronisasi Berhasil" else "Sinkronisasi Gagal",
                message = msg,
                type = if (success) AlertType.SUCCESS else AlertType.ERROR
            )
        }
    }
}
