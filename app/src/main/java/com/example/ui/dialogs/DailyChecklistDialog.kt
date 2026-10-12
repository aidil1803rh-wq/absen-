package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InternshipActivities
import com.example.data.model.ParticipantWithAttendance
import com.example.ui.theme.PksHadirGreen
import com.example.ui.theme.PksHadirGreenBg
import com.example.ui.theme.PksIzinAmber
import com.example.ui.theme.PksOrangeDark
import com.example.ui.theme.PksOrangePrimary
import com.example.ui.theme.PksSakitBlue
import com.example.util.QrCodeUtil

@Composable
fun DailyChecklistDialog(
    item: ParticipantWithAttendance,
    dateStr: String = QrCodeUtil.getTodayDateString(),
    onDismiss: () -> Unit,
    onClearAttendance: (() -> Unit)? = null,
    onSave: (
        status: String,
        timeIn: String,
        timeOut: String,
        isQuranRead: Boolean,
        quranSurahJuz: String,
        activities: List<String>,
        notes: String
    ) -> Unit
) {
    var status by remember { mutableStateOf(if (item.todayStatus.isBlank() || item.todayStatus == "Belum Absen") "Hadir" else item.todayStatus) }
    var timeIn by remember { mutableStateOf(if (item.todayTime != "-") item.todayTime else QrCodeUtil.getCurrentTimeString()) }
    var timeOut by remember { mutableStateOf(item.todayTimeOut) }
    var isQuranRead by remember { mutableStateOf(item.isQuranRead) }
    var quranSurahJuz by remember { mutableStateOf(item.quranSurahJuz) }
    var notes by remember { mutableStateOf(item.activityNotes) }

    val selectedActivities = remember {
        mutableStateListOf<String>().apply {
            addAll(item.dailyActivities)
        }
    }

    val quickSurahs = listOf("Al-Kahfi", "Al-Waqi'ah", "Ar-Rahman", "Juz 30", "Al-Mulk", "Yasin")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("dialog_daily_checklist"),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PksOrangePrimary, PksOrangeDark)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Logbook & Presensi Magang",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.participant.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        val nimDisplay = item.participant.nim.ifBlank { item.participant.id }
                        Text(
                            text = "NIM: $nimDisplay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFEDD5)
                        )
                        Text(
                            text = "${item.participant.division} • ${item.participant.university}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFFFEDD5).copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Tanggal: ${QrCodeUtil.getTodayFormattedIndonesian(dateStr)}",
                            fontSize = 11.sp,
                            color = Color(0xFFFFEDD5).copy(alpha = 0.9f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Kehadiran Selector
                    Text(
                        text = "STATUS KEHADIRAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Hadir", "Izin", "Sakit", "Alfa").forEach { st ->
                            val isSelected = status == st
                            val (bgColor, textColor) = when (st) {
                                "Hadir" -> Pair(PksHadirGreenBg, PksHadirGreen)
                                "Izin" -> Pair(Color(0xFFFEF3C7), PksIzinAmber)
                                "Sakit" -> Pair(Color(0xFFE0F2FE), PksSakitBlue)
                                else -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) textColor else bgColor)
                                    .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable { status = st }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else textColor
                                )
                            }
                        }
                    }

                    // Section Jam Masuk & Jam Keluar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card Jam Masuk
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = null,
                                        tint = PksHadirGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Absen Masuk",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (timeIn.isNotBlank() && timeIn != "-") timeIn else "-",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF14532D)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { timeIn = QrCodeUtil.getCurrentTimeString() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    Text("Set Jam Sekarang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Card Jam Keluar
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (timeOut != "-") Color(0xFFEFF6FF) else Color(0xFFFFF7ED)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (timeOut != "-") Color(0xFFBFDBFE) else Color(0xFFFFEDD5)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = if (timeOut != "-") Color(0xFF2563EB) else PksOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Absen Keluar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (timeOut != "-") Color(0xFF1E40AF) else Color(0xFF9A3412)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (timeOut.isNotBlank() && timeOut != "-") timeOut else "Belum Keluar",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (timeOut != "-") Color(0xFF1E3A8A) else Color(0xFFC2410C)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { timeOut = QrCodeUtil.getCurrentTimeString() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (timeOut != "-") Color(0xFF2563EB) else PksOrangePrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = if (timeOut != "-") "Update Jam Pulang" else "Absen Keluar!",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // UTAMA: Card Membaca Al-Qur'an di Setiap Masuk Magang
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF16A34A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF16A34A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Membaca Al-Qur'an / Tilawah",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF14532D)
                                            )
                                        }
                                        Text(
                                            text = "⭐ Kegiatan Utama di Setiap Masuk Magang",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isQuranRead,
                                    onCheckedChange = { isQuranRead = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF16A34A),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("checkbox_quran_reading")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Surah / Juz / Halaman yang Dibaca:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = quranSurahJuz,
                                onValueChange = {
                                    quranSurahJuz = it
                                    if (it.isNotBlank()) isQuranRead = true
                                },
                                placeholder = { Text("Contoh: Surat Al-Kahfi ayat 1-50 / Juz 30", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_quran_surah"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF16A34A),
                                    unfocusedBorderColor = Color(0xFF86EFAC),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Surah suggestion chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickSurahs.forEach { sName ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                                            .clickable {
                                                quranSurahJuz = if (quranSurahJuz.isBlank()) "Surat $sName" else "$quranSurahJuz, $sName"
                                                isQuranRead = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "+ $sName",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ceklis Kegiatan Magang Lainnya
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "CEKLIS KEGIATAN HARIAN MAGANG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val activitiesList = listOf(
                                InternshipActivities.AL_MATSURAT to "Dzikir Al-Ma'tsurat / Doa Pagi",
                                InternshipActivities.TUGAS_DIVISI to "Pengerjaan Tugas Divisi (Desain/IT/Liputan/Sosmed)",
                                InternshipActivities.SHOLAT_BERJAMAAH to "Sholat Berjamaah di Kantor",
                                InternshipActivities.BRIEFING_TIM to "Briefing & Diskusi Bersama Tim / Mentor",
                                InternshipActivities.LOGBOOK_HARIAN to "Pengisian Laporan / Logbook Harian"
                            )

                            activitiesList.forEach { (actKey, actLabel) ->
                                val isChecked = selectedActivities.contains(actKey)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isChecked) {
                                                selectedActivities.remove(actKey)
                                            } else {
                                                selectedActivities.add(actKey)
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { check ->
                                            if (check) selectedActivities.add(actKey) else selectedActivities.remove(actKey)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = PksOrangePrimary
                                        ),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = actLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecked) Color(0xFF0F172A) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Catatan Kegiatan / Logbook
                    Column {
                        Text(
                            text = "Catatan Kegiatan / Ringkasan Hari Ini",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Tuliskan deskripsi pekerjaan atau progres magang hari ini...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .testTag("input_activity_notes"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 3
                        )
                    }

                    // Action buttons
                    if (onClearAttendance != null && (item.todayStatus.isNotBlank() && item.todayStatus != "Belum Absen" || item.todayTime != "-")) {
                        OutlinedButton(
                            onClick = {
                                onClearAttendance()
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("btn_clear_attendance_dialog"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kosongkan Presensi & Catatan Hari Ini", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onSave(
                                    status,
                                    timeIn,
                                    timeOut,
                                    isQuranRead,
                                    quranSurahJuz,
                                    selectedActivities.toList(),
                                    notes
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                                .testTag("btn_save_daily_checklist"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Kegiatan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
