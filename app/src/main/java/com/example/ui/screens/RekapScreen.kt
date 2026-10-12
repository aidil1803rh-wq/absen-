package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParticipantWithAttendance
import com.example.ui.components.StatusBadge
import com.example.ui.components.YellowCardBadge
import com.example.ui.theme.PksAlfaRed
import com.example.ui.theme.PksAlfaRedBg
import com.example.ui.theme.PksHadirGreen
import com.example.ui.theme.PksHadirGreenBg
import com.example.ui.theme.PksIzinAmber
import com.example.ui.theme.PksIzinAmberBg
import com.example.ui.theme.PksOrangeLight
import com.example.ui.theme.PksOrangePrimary
import com.example.ui.theme.PksSakitBlue
import com.example.ui.theme.PksSakitBlueBg
import com.example.util.QrCodeUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RekapScreen(
    selectedDate: String,
    participantsWithStats: List<ParticipantWithAttendance>,
    onSelectDate: (String) -> Unit,
    onUpdateStatus: (participantId: String, status: String, date: String) -> Unit,
    onExportCsv: (date: String) -> Unit,
    onOpenChecklist: ((ParticipantWithAttendance) -> Unit)? = null,
    onClearAttendance: ((participantId: String, date: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Filter Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    Column {
                        Text(
                            text = "Rekap Kehadiran Magang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Edit manual Izin / Sakit / Hadir / Alfa",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Button(
                        onClick = { onExportCsv(selectedDate) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.testTag("btn_export_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker Controller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val prevDate = changeDateBy(selectedDate, -1)
                            onSelectDate(prevDate)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Sebelumnya",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                showDatePicker(context, selectedDate) { newDate ->
                                    onSelectDate(newDate)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PksOrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = QrCodeUtil.getTodayFormattedIndonesian(selectedDate),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Ketuk untuk ganti tanggal ($selectedDate)",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val nextDate = changeDateBy(selectedDate, 1)
                            onSelectDate(nextDate)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Berikutnya",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // List of Attendance on that date
        Text(
            text = "DAFTAR KEHADIRAN ($selectedDate)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )

        if (participantsWithStats.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = "Tidak ada data peserta magang.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(participantsWithStats) { item ->
                    RekapItemCard(
                        item = item,
                        onStatusChange = { newStatus ->
                            onUpdateStatus(item.participant.id, newStatus, selectedDate)
                        },
                        onClearAttendance = {
                            onClearAttendance?.invoke(item.participant.id, selectedDate)
                        },
                        onOpenChecklist = {
                            onOpenChecklist?.invoke(item)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun RekapItemCard(
    item: ParticipantWithAttendance,
    onStatusChange: (String) -> Unit,
    onClearAttendance: (() -> Unit)? = null,
    onOpenChecklist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initials = item.participant.name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                    .ifBlank { "MG" }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PksOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PksOrangePrimary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.participant.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF0F172A)
                    )
                    val nimDisplay = item.participant.nim.ifBlank { item.participant.id }
                    Text(
                        text = "NIM: $nimDisplay",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "${item.participant.division} (Alfa: ${item.totalAlfa}x)",
                        fontSize = 10.sp,
                        color = PksOrangePrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.totalAlfa >= 3) {
                        Spacer(modifier = Modifier.height(2.dp))
                        YellowCardBadge(alfaCount = item.totalAlfa)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(status = item.todayStatus)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Masuk: ${item.todayTime}",
                        fontSize = 9.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "Keluar: ${item.todayTimeOut}",
                        fontSize = 9.sp,
                        color = if (item.todayTimeOut != "-") Color(0xFF2563EB) else Color(0xFF94A3B8),
                        fontWeight = if (item.todayTimeOut != "-") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quran Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.isQuranRead) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = if (item.isQuranRead) Color(0xFF16A34A) else Color(0xFFD97706),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isQuranRead) {
                                if (item.quranSurahJuz.isNotBlank()) "Qur'an: ${item.quranSurahJuz}" else "Qur'an: Sudah"
                            } else "Qur'an: Belum dibaca",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isQuranRead) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }

                if (onOpenChecklist != null) {
                    Text(
                        text = "Detail / Edit Ceklis 📝",
                        fontSize = 10.sp,
                        color = PksOrangePrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onOpenChecklist() }
                            .padding(4.dp)
                    )
                }
            }

            if (item.activityNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Catatan: ${item.activityNotes}",
                        fontSize = 10.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            // Quick Status Changer Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ubah Status:",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    StatusButton("Hadir", item.todayStatus == "Hadir", PksHadirGreenBg, PksHadirGreen) {
                        onStatusChange("Hadir")
                    }
                    StatusButton("Izin", item.todayStatus == "Izin", PksIzinAmberBg, PksIzinAmber) {
                        onStatusChange("Izin")
                    }
                    StatusButton("Sakit", item.todayStatus == "Sakit", PksSakitBlueBg, PksSakitBlue) {
                        onStatusChange("Sakit")
                    }
                    StatusButton("Alfa", item.todayStatus == "Alfa", PksAlfaRedBg, PksAlfaRed) {
                        onStatusChange("Alfa")
                    }
                    StatusButton("Kosongkan", item.todayStatus.isBlank(), Color(0xFFF1F5F9), Color(0xFF64748B)) {
                        onClearAttendance?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    isSelected: Boolean,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) textColor else bgColor)
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else textColor
        )
    }
}

private fun changeDateBy(dateStr: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(dateStr) ?: java.util.Date()
        cal.add(Calendar.DAY_OF_YEAR, days)
        sdf.format(cal.time)
    } catch (e: Exception) {
        dateStr
    }
}

private fun showDatePicker(context: android.content.Context, currentDateStr: String, onDateSet: (String) -> Unit) {
    try {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        cal.time = sdf.parse(currentDateStr) ?: java.util.Date()

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSet(sdf.format(chosen.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
