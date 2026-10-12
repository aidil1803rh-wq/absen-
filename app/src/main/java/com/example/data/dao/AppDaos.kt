package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.Participant
import com.example.data.model.SettingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants ORDER BY id ASC")
    fun getAllParticipants(): Flow<List<Participant>>

    @Query("SELECT * FROM participants ORDER BY id ASC")
    suspend fun getAllParticipantsSnapshot(): List<Participant>

    @Query("SELECT * FROM participants WHERE id = :id LIMIT 1")
    suspend fun getParticipantById(id: String): Participant?

    @Query("SELECT * FROM participants WHERE id = :idOrNim OR nim = :idOrNim LIMIT 1")
    suspend fun getParticipantByIdOrNim(idOrNim: String): Participant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: Participant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<Participant>)

    @Update
    suspend fun updateParticipant(participant: Participant)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun deleteParticipantById(id: String)

    @Query("SELECT COUNT(*) FROM participants")
    suspend fun countParticipants(): Int
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAttendanceForDateSnapshot(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    suspend fun getAllAttendanceSnapshot(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE participantId = :participantId AND date = :date LIMIT 1")
    suspend fun getRecord(participantId: String, date: String): AttendanceRecord?

    @Query("SELECT COUNT(*) FROM attendance_records WHERE participantId = :participantId AND status = 'Alfa'")
    fun countAlfaForParticipant(participantId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE participantId = :participantId AND status = 'Alfa'")
    suspend fun countAlfaSnapshot(participantId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecord>)

    @Query("DELETE FROM attendance_records WHERE participantId = :participantId AND date = :date")
    suspend fun deleteRecord(participantId: String, date: String)

    @Query("DELETE FROM attendance_records WHERE participantId = :participantId")
    suspend fun deleteRecordsForParticipant(participantId: String)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(item: SettingItem)

    @Query("SELECT * FROM settings")
    suspend fun getAllSettings(): List<SettingItem>
}
