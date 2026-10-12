package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "participants")
data class Participant(
    @PrimaryKey val id: String,
    val name: String,
    val division: String,
    val university: String,
    val status: String = "Aktif",
    val createdAt: Long = System.currentTimeMillis(),
    val nim: String = id
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["participantId", "date"], unique = true)]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val participantId: String,
    val date: String, // YYYY-MM-DD
    val status: String, // Hadir, Izin, Sakit, Alfa
    val time: String = "-", // Jam Masuk (HH:mm)
    val timeOut: String = "-", // Jam Keluar (HH:mm)
    val isQuranRead: Boolean = false, // Ceklis Membaca Al-Qur'an setiap masuk magang (UTAMA)
    val quranSurahJuz: String = "", // Misal: "Surat Al-Kahfi", "Juz 30"
    val dailyActivities: String = "", // Comma-separated or JSON list of completed activities
    val activityNotes: String = "", // Catatan ringkasan kegiatan harian
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingItem(
    @PrimaryKey val key: String,
    val value: String
)

data class ParticipantWithAttendance(
    val participant: Participant,
    val todayStatus: String, // Hadir, Izin, Sakit, Alfa
    val todayTime: String, // Jam Masuk
    val todayTimeOut: String = "-", // Jam Keluar
    val isQuranRead: Boolean = false, // Ceklis baca Al-Quran
    val quranSurahJuz: String = "",
    val dailyActivities: List<String> = emptyList(),
    val activityNotes: String = "",
    val totalAlfa: Int = 0
)

data class DailyAttendanceStats(
    val date: String,
    val hadirCount: Int,
    val izinCount: Int,
    val sakitCount: Int,
    val alfaCount: Int,
    val totalParticipants: Int,
    val quranCompletedCount: Int = 0,
    val checkedOutCount: Int = 0
)

object InternshipActivities {
    const val QURAN_READING = "Membaca Al-Qur'an / Tilawah Pagi"
    const val AL_MATSURAT = "Dzikir Al-Ma'tsurat / Doa Pagi"
    const val TUGAS_DIVISI = "Tugas & Proyek Divisi"
    const val SHOLAT_BERJAMAAH = "Sholat Berjamaah"
    const val BRIEFING_TIM = "Briefing / Diskusi Tim"
    const val LOGBOOK_HARIAN = "Pengisian Logbook Harian"

    val DEFAULT_ACTIVITIES = listOf(
        QURAN_READING,
        AL_MATSURAT,
        TUGAS_DIVISI,
        SHOLAT_BERJAMAAH,
        BRIEFING_TIM,
        LOGBOOK_HARIAN
    )
}

data class InternshipSchedule(
    val startDate: String = "2024-09-02",
    val endDate: String = "2024-12-31",
    val workDays: List<Int> = listOf(1, 2, 3, 4, 5),
    val timeIn: String = "08:00",
    val timeOut: String = "17:00"
) {
    val workDaysCount: Int get() = workDays.size
}

