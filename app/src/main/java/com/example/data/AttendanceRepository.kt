package com.example.data

import com.example.data.dao.AttendanceDao
import com.example.data.dao.ParticipantDao
import com.example.data.dao.SettingDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyAttendanceStats
import com.example.data.model.Participant
import com.example.data.model.ParticipantWithAttendance
import com.example.data.model.SettingItem
import com.example.util.QrCodeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AttendanceRepository(
    private val participantDao: ParticipantDao,
    private val attendanceDao: AttendanceDao,
    private val settingDao: SettingDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        const val KEY_WORK_DAYS = "work_days"
        const val DEFAULT_WORK_DAYS = "1,2,3,4,5" // Mon, Tue, Wed, Thu, Fri

        const val KEY_INTERN_START_DATE = "intern_start_date"
        const val DEFAULT_INTERN_START_DATE = "2024-09-02"

        const val KEY_INTERN_END_DATE = "intern_end_date"
        const val DEFAULT_INTERN_END_DATE = "2024-12-31"

        const val KEY_WORK_TIME_IN = "work_time_in"
        const val DEFAULT_WORK_TIME_IN = "08:00"

        const val KEY_WORK_TIME_OUT = "work_time_out"
        const val DEFAULT_WORK_TIME_OUT = "17:00"

        val DEFAULT_INTERNS = listOf(
            Participant(
                id = "2312016106",
                name = "Fernanda Saputra",
                division = "Komdigi & IT",
                university = "UNAND",
                status = "Aktif",
                nim = "2312016106"
            ),
            Participant(
                id = "2312010143",
                name = "Yogi Pradinata",
                division = "Multimedia & Kreatif",
                university = "UNP",
                status = "Aktif",
                nim = "2312010143"
            ),
            Participant(
                id = "2312010080",
                name = "Bintang Resapta. S.",
                division = "Humas & Protokoler",
                university = "UNAND",
                status = "Aktif",
                nim = "2312010080"
            ),
            Participant(
                id = "2312010101",
                name = "Abdul Bahri",
                division = "Komdigi & IT",
                university = "UIN Imam Bonjol",
                status = "Aktif",
                nim = "2312010101"
            ),
            Participant(
                id = "2312010088",
                name = "M. Astomo",
                division = "Multimedia & Kreatif",
                university = "UNP",
                status = "Aktif",
                nim = "2312010088"
            ),
            Participant(
                id = "2312010150",
                name = "Feri Saputra",
                division = "Publikasi & Desain",
                university = "UNP",
                status = "Aktif",
                nim = "2312010150"
            ),
            Participant(
                id = "2312010145",
                name = "Ade Okta Firdaus",
                division = "Logistik & Teknis",
                university = "UNAND",
                status = "Aktif",
                nim = "2312010145"
            )
        )
    }

    init {
        scope.launch {
            seedDefaultDataIfEmpty()
            checkAndProcessAutoAlfa()
        }
    }

    private suspend fun seedDefaultDataIfEmpty() {
        val existing = participantDao.getAllParticipantsSnapshot().associateBy { it.id }
        // Ensure all 7 requested interns exist and their data is preserved
        for (intern in DEFAULT_INTERNS) {
            if (!existing.containsKey(intern.id)) {
                participantDao.insertParticipant(intern)
            }
        }

        // Clean up legacy placeholder records if they have no attendance history
        val dummyIds = listOf("MG001", "MG002", "MG003")
        for (dummyId in dummyIds) {
            if (existing.containsKey(dummyId)) {
                val recs = attendanceDao.getAllAttendanceSnapshot().filter { it.participantId == dummyId }
                if (recs.isEmpty()) {
                    participantDao.deleteParticipantById(dummyId)
                }
            }
        }

        if (settingDao.getSetting(KEY_WORK_DAYS) == null) {
            settingDao.setSetting(SettingItem(KEY_WORK_DAYS, DEFAULT_WORK_DAYS))
        }
        if (settingDao.getSetting(KEY_INTERN_START_DATE) == null) {
            settingDao.setSetting(SettingItem(KEY_INTERN_START_DATE, DEFAULT_INTERN_START_DATE))
        }
        if (settingDao.getSetting(KEY_INTERN_END_DATE) == null) {
            settingDao.setSetting(SettingItem(KEY_INTERN_END_DATE, DEFAULT_INTERN_END_DATE))
        }
        if (settingDao.getSetting(KEY_WORK_TIME_IN) == null) {
            settingDao.setSetting(SettingItem(KEY_WORK_TIME_IN, DEFAULT_WORK_TIME_IN))
        }
        if (settingDao.getSetting(KEY_WORK_TIME_OUT) == null) {
            settingDao.setSetting(SettingItem(KEY_WORK_TIME_OUT, DEFAULT_WORK_TIME_OUT))
        }
    }

    suspend fun getWorkDays(): List<Int> {
        val raw = settingDao.getSetting(KEY_WORK_DAYS) ?: DEFAULT_WORK_DAYS
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    suspend fun setWorkDays(days: List<Int>) {
        val str = days.sorted().joinToString(",")
        settingDao.setSetting(SettingItem(KEY_WORK_DAYS, str))
        checkAndProcessAutoAlfa()
    }

    suspend fun getInternshipSchedule(): com.example.data.model.InternshipSchedule {
        val start = settingDao.getSetting(KEY_INTERN_START_DATE) ?: DEFAULT_INTERN_START_DATE
        val end = settingDao.getSetting(KEY_INTERN_END_DATE) ?: DEFAULT_INTERN_END_DATE
        val timeIn = settingDao.getSetting(KEY_WORK_TIME_IN) ?: DEFAULT_WORK_TIME_IN
        val timeOut = settingDao.getSetting(KEY_WORK_TIME_OUT) ?: DEFAULT_WORK_TIME_OUT
        val days = getWorkDays()
        return com.example.data.model.InternshipSchedule(
            startDate = start,
            endDate = end,
            workDays = days,
            timeIn = timeIn,
            timeOut = timeOut
        )
    }

    suspend fun saveInternshipSchedule(
        startDate: String,
        endDate: String,
        workDays: List<Int>,
        timeIn: String,
        timeOut: String
    ) {
        settingDao.setSetting(SettingItem(KEY_INTERN_START_DATE, startDate.trim()))
        settingDao.setSetting(SettingItem(KEY_INTERN_END_DATE, endDate.trim()))
        settingDao.setSetting(SettingItem(KEY_WORK_TIME_IN, timeIn.trim()))
        settingDao.setSetting(SettingItem(KEY_WORK_TIME_OUT, timeOut.trim()))
        val str = workDays.sorted().joinToString(",")
        settingDao.setSetting(SettingItem(KEY_WORK_DAYS, str))
        checkAndProcessAutoAlfa()
    }

    suspend fun checkAndProcessAutoAlfa(dateStr: String = QrCodeUtil.getTodayDateString()) {
        val workDays = getWorkDays()
        val cal = Calendar.getInstance()
        val isoDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        val workTimeOutStr = settingDao.getSetting(KEY_WORK_TIME_OUT) ?: DEFAULT_WORK_TIME_OUT
        val outHour = workTimeOutStr.substringBefore(":").toIntOrNull() ?: 17
        val outMinute = workTimeOutStr.substringAfter(":", "0").toIntOrNull() ?: 0

        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val isPastWorkHours = (currentHour > outHour) || (currentHour == outHour && currentMinute >= outMinute)
        val isToday = (dateStr == QrCodeUtil.getTodayDateString())

        // Only mark Alfa for past dates, or after workday end time on today
        if (workDays.contains(isoDay) && (!isToday || isPastWorkHours)) {
            val participants = participantDao.getAllParticipantsSnapshot()
            val existing = attendanceDao.getAttendanceForDateSnapshot(dateStr).associateBy { it.participantId }

            for (p in participants) {
                if (!existing.containsKey(p.id)) {
                    attendanceDao.insertRecord(
                        AttendanceRecord(
                            participantId = p.id,
                            date = dateStr,
                            status = "Alfa",
                            time = "-",
                            timeOut = "-",
                            isQuranRead = false,
                            quranSurahJuz = "",
                            dailyActivities = "",
                            activityNotes = "" // Keterangan absensi dikosongkan
                        )
                    )
                }
            }
        }
    }

    fun getParticipants(): Flow<List<Participant>> = participantDao.getAllParticipants()

    fun getTodayParticipantsWithAttendance(dateStr: String = QrCodeUtil.getTodayDateString()): Flow<List<ParticipantWithAttendance>> {
        return combine(
            participantDao.getAllParticipants(),
            attendanceDao.getAttendanceForDate(dateStr),
            attendanceDao.getAllAttendance()
        ) { participants, todayLogs, allLogs ->
            val todayMap = todayLogs.associateBy { it.participantId }
            val alfaMap = allLogs.filter { it.status == "Alfa" }.groupingBy { it.participantId }.eachCount()

            participants.map { p ->
                val log = todayMap[p.id]
                val actList = log?.dailyActivities?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
                ParticipantWithAttendance(
                    participant = p,
                    todayStatus = log?.status ?: "", // Keterangan/status absen dikosongkan jika belum absen
                    todayTime = log?.time ?: "-",
                    todayTimeOut = log?.timeOut ?: "-",
                    isQuranRead = log?.isQuranRead ?: false,
                    quranSurahJuz = log?.quranSurahJuz ?: "",
                    dailyActivities = actList,
                    activityNotes = log?.activityNotes ?: "", // Keterangan catatan dikosongkan jika belum diisi
                    totalAlfa = alfaMap[p.id] ?: 0
                )
            }
        }
    }

    fun getDailyStats(dateStr: String = QrCodeUtil.getTodayDateString()): Flow<DailyAttendanceStats> {
        return combine(
            participantDao.getAllParticipants(),
            attendanceDao.getAttendanceForDate(dateStr)
        ) { participants, todayLogs ->
            val todayMap = todayLogs.associateBy { it.participantId }
            var hadir = 0
            var izin = 0
            var sakit = 0
            var alfa = 0
            var quranCount = 0
            var checkoutCount = 0

            for (p in participants) {
                val log = todayMap[p.id]
                val status = log?.status ?: ""
                when (status) {
                    "Hadir" -> hadir++
                    "Izin" -> izin++
                    "Sakit" -> sakit++
                    "Alfa" -> alfa++
                    else -> {} // Belum absen / kosong
                }
                if (log?.isQuranRead == true) {
                    quranCount++
                }
                if (log != null && log.timeOut.isNotBlank() && log.timeOut != "-") {
                    checkoutCount++
                }
            }

            DailyAttendanceStats(
                date = dateStr,
                hadirCount = hadir,
                izinCount = izin,
                sakitCount = sakit,
                alfaCount = alfa,
                totalParticipants = participants.size,
                quranCompletedCount = quranCount,
                checkedOutCount = checkoutCount
            )
        }
    }

    suspend fun clearAttendance(
        participantId: String,
        dateStr: String = QrCodeUtil.getTodayDateString()
    ): Result<Participant> {
        val participant = participantDao.getParticipantById(participantId)
            ?: return Result.failure(Exception("ID Peserta '$participantId' tidak ditemukan."))
        attendanceDao.deleteRecord(participantId, dateStr)
        return Result.success(participant)
    }

    suspend fun recordAttendance(
        participantId: String,
        status: String,
        dateStr: String = QrCodeUtil.getTodayDateString(),
        timeStr: String = if (status == "Hadir") QrCodeUtil.getCurrentTimeString() else "-",
        timeOutStr: String? = null,
        isQuranRead: Boolean? = null,
        quranSurahJuz: String? = null,
        dailyActivities: List<String>? = null,
        activityNotes: String? = null
    ): Result<Participant> {
        val participant = participantDao.getParticipantById(participantId)
            ?: return Result.failure(Exception("ID Peserta '$participantId' tidak ditemukan."))

        val existing = attendanceDao.getRecord(participantId, dateStr)
        val activitiesString = dailyActivities?.joinToString(";") ?: existing?.dailyActivities ?: ""

        val record = if (existing != null) {
            existing.copy(
                status = status,
                time = if (status == "Hadir") (if (existing.time != "-") existing.time else timeStr) else "-",
                timeOut = timeOutStr ?: existing.timeOut,
                isQuranRead = isQuranRead ?: existing.isQuranRead,
                quranSurahJuz = quranSurahJuz ?: existing.quranSurahJuz,
                dailyActivities = activitiesString,
                activityNotes = activityNotes ?: existing.activityNotes,
                timestamp = System.currentTimeMillis()
            )
        } else {
            AttendanceRecord(
                participantId = participantId,
                date = dateStr,
                status = status,
                time = timeStr,
                timeOut = timeOutStr ?: "-",
                isQuranRead = isQuranRead ?: false,
                quranSurahJuz = quranSurahJuz ?: "",
                dailyActivities = activitiesString,
                activityNotes = activityNotes ?: ""
            )
        }
        attendanceDao.insertRecord(record)
        return Result.success(participant)
    }

    suspend fun recordCheckOut(
        participantId: String,
        dateStr: String = QrCodeUtil.getTodayDateString(),
        timeOutStr: String = QrCodeUtil.getCurrentTimeString()
    ): Result<Participant> {
        val participant = participantDao.getParticipantById(participantId)
            ?: return Result.failure(Exception("ID Peserta '$participantId' tidak ditemukan."))

        val existing = attendanceDao.getRecord(participantId, dateStr)
        val record = if (existing != null) {
            existing.copy(
                timeOut = timeOutStr,
                timestamp = System.currentTimeMillis()
            )
        } else {
            AttendanceRecord(
                participantId = participantId,
                date = dateStr,
                status = "Hadir",
                time = "-",
                timeOut = timeOutStr
            )
        }
        attendanceDao.insertRecord(record)
        return Result.success(participant)
    }

    suspend fun updateDailyChecklist(
        participantId: String,
        dateStr: String = QrCodeUtil.getTodayDateString(),
        isQuranRead: Boolean,
        quranSurahJuz: String,
        activities: List<String>,
        activityNotes: String
    ): Result<Participant> {
        val participant = participantDao.getParticipantById(participantId)
            ?: return Result.failure(Exception("ID Peserta '$participantId' tidak ditemukan."))

        val existing = attendanceDao.getRecord(participantId, dateStr)
        val record = if (existing != null) {
            existing.copy(
                isQuranRead = isQuranRead,
                quranSurahJuz = quranSurahJuz,
                dailyActivities = activities.joinToString(";"),
                activityNotes = activityNotes,
                timestamp = System.currentTimeMillis()
            )
        } else {
            AttendanceRecord(
                participantId = participantId,
                date = dateStr,
                status = "Hadir",
                time = QrCodeUtil.getCurrentTimeString(),
                isQuranRead = isQuranRead,
                quranSurahJuz = quranSurahJuz,
                dailyActivities = activities.joinToString(";"),
                activityNotes = activityNotes
            )
        }
        attendanceDao.insertRecord(record)
        return Result.success(participant)
    }

    suspend fun addParticipant(
        name: String,
        nim: String = "",
        division: String = "Komdigi & IT",
        university: String = "UNAND"
    ): Participant {
        val cleanNim = nim.trim()
        val existing = participantDao.getAllParticipantsSnapshot()
        val newId = if (cleanNim.isNotBlank()) {
            cleanNim
        } else {
            val nextNum = (existing.mapNotNull { it.id.removePrefix("MG").toIntOrNull() }.maxOrNull() ?: 0) + 1
            "MG" + nextNum.toString().padStart(3, '0')
        }
        val participant = Participant(
            id = newId,
            name = name.trim(),
            division = division.trim(),
            university = university.trim(),
            status = "Aktif",
            nim = if (cleanNim.isNotBlank()) cleanNim else newId
        )
        participantDao.insertParticipant(participant)
        checkAndProcessAutoAlfa()
        return participant
    }

    suspend fun deleteParticipant(id: String) {
        participantDao.deleteParticipantById(id)
        attendanceDao.deleteRecordsForParticipant(id)
    }

    suspend fun countAlfa(participantId: String): Int {
        return attendanceDao.countAlfaSnapshot(participantId)
    }

    suspend fun exportCsv(dateStr: String? = null): String {
        val participants = participantDao.getAllParticipantsSnapshot().associateBy { it.id }
        val records = if (dateStr != null) {
            attendanceDao.getAttendanceForDateSnapshot(dateStr)
        } else {
            attendanceDao.getAllAttendanceSnapshot()
        }

        val sb = java.lang.StringBuilder()
        sb.append("ID,NIM,Nama,Divisi,Universitas,Tanggal,Jam_Masuk,Jam_Keluar,Status,Membaca_AlQuran,Detail_Tilawah,Kegiatan_Hari_Ini,Catatan\n")
        for (rec in records) {
            val p = participants[rec.participantId]
            val nimVal = p?.nim?.ifBlank { p.id } ?: rec.participantId
            val name = p?.name ?: "Unknown"
            val div = p?.division ?: "-"
            val uni = p?.university ?: "-"
            val quranStatus = if (rec.isQuranRead) "Sudah" else "Belum"
            val tilawahDetail = rec.quranSurahJuz.replace("\"", "'")
            val actDetail = rec.dailyActivities.replace(";", " | ").replace("\"", "'")
            val notes = rec.activityNotes.replace("\"", "'")
            sb.append("${rec.participantId},\"$nimVal\",\"$name\",\"$div\",\"$uni\",${rec.date},${rec.time},${rec.timeOut},${rec.status},\"$quranStatus\",\"$tilawahDetail\",\"$actDetail\",\"$notes\"\n")
        }
        return sb.toString()
    }

    // JSON export/import for multi-device sync
    suspend fun exportFullJson(): String {
        val participants = participantDao.getAllParticipantsSnapshot()
        val records = attendanceDao.getAllAttendanceSnapshot()
        val schedule = getInternshipSchedule()

        val root = JSONObject()
        root.put("version", 4)
        root.put("workDays", schedule.workDays.joinToString(","))
        root.put("startDate", schedule.startDate)
        root.put("endDate", schedule.endDate)
        root.put("timeIn", schedule.timeIn)
        root.put("timeOut", schedule.timeOut)

        val partArray = JSONArray()
        for (p in participants) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("nim", p.nim)
            obj.put("name", p.name)
            obj.put("division", p.division)
            obj.put("university", p.university)
            obj.put("status", p.status)
            obj.put("createdAt", p.createdAt)
            partArray.put(obj)
        }
        root.put("participants", partArray)

        val recArray = JSONArray()
        for (r in records) {
            val obj = JSONObject()
            obj.put("participantId", r.participantId)
            obj.put("date", r.date)
            obj.put("status", r.status)
            obj.put("time", r.time)
            obj.put("timeOut", r.timeOut)
            obj.put("isQuranRead", r.isQuranRead)
            obj.put("quranSurahJuz", r.quranSurahJuz)
            obj.put("dailyActivities", r.dailyActivities)
            obj.put("activityNotes", r.activityNotes)
            obj.put("timestamp", r.timestamp)
            recArray.put(obj)
        }
        root.put("attendance", recArray)

        return root.toString()
    }

    suspend fun importFullJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val workDaysStr = root.optString("workDays", DEFAULT_WORK_DAYS)
            val startDate = root.optString("startDate", DEFAULT_INTERN_START_DATE)
            val endDate = root.optString("endDate", DEFAULT_INTERN_END_DATE)
            val timeIn = root.optString("timeIn", DEFAULT_WORK_TIME_IN)
            val timeOut = root.optString("timeOut", DEFAULT_WORK_TIME_OUT)

            settingDao.setSetting(SettingItem(KEY_WORK_DAYS, workDaysStr))
            settingDao.setSetting(SettingItem(KEY_INTERN_START_DATE, startDate))
            settingDao.setSetting(SettingItem(KEY_INTERN_END_DATE, endDate))
            settingDao.setSetting(SettingItem(KEY_WORK_TIME_IN, timeIn))
            settingDao.setSetting(SettingItem(KEY_WORK_TIME_OUT, timeOut))

            val partArray = root.getJSONArray("participants")
            val pList = mutableListOf<Participant>()
            for (i in 0 until partArray.length()) {
                val obj = partArray.getJSONObject(i)
                val idVal = obj.getString("id")
                val nimVal = obj.optString("nim", idVal)
                pList.add(
                    Participant(
                        id = idVal,
                        name = obj.getString("name"),
                        division = obj.getString("division"),
                        university = obj.getString("university"),
                        status = obj.optString("status", "Aktif"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        nim = nimVal
                    )
                )
            }
            if (pList.isNotEmpty()) {
                participantDao.insertAll(pList)
            }

            val recArray = root.getJSONArray("attendance")
            val rList = mutableListOf<AttendanceRecord>()
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                rList.add(
                    AttendanceRecord(
                        participantId = obj.getString("participantId"),
                        date = obj.getString("date"),
                        status = obj.getString("status"),
                        time = obj.optString("time", "-"),
                        timeOut = obj.optString("timeOut", "-"),
                        isQuranRead = obj.optBoolean("isQuranRead", false),
                        quranSurahJuz = obj.optString("quranSurahJuz", ""),
                        dailyActivities = obj.optString("dailyActivities", ""),
                        activityNotes = obj.optString("activityNotes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            if (rList.isNotEmpty()) {
                attendanceDao.insertAll(rList)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
