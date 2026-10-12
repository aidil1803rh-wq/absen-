package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyAttendanceStats
import com.example.data.model.ParticipantWithAttendance
import com.example.ui.components.StatusBadge
import com.example.ui.components.YellowCardBadge
import com.example.ui.theme.PksAlfaRed
import com.example.ui.theme.PksAlfaRedBg
import com.example.ui.theme.PksHadirGreen
import com.example.ui.theme.PksHadirGreenBg
import com.example.ui.theme.PksIzinAmber
import com.example.ui.theme.PksIzinAmberBg
import com.example.ui.theme.PksOrangeDark
import com.example.ui.theme.PksOrangeLight
import com.example.ui.theme.PksOrangePrimary
import com.example.ui.theme.PksSakitBlue
import com.example.ui.theme.PksSakitBlueBg
import com.example.util.QrCodeUtil

@Composable
fun HomeScreen(
    stats: DailyAttendanceStats,
    participants: List<ParticipantWithAttendance>,
    schedule: com.example.data.model.InternshipSchedule = com.example.data.model.InternshipSchedule(),
    onScanClick: () -> Unit,
    onViewRekapClick: () -> Unit,
    onParticipantClick: ((ParticipantWithAttendance) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
            // Hero Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_hero_banner")
            ) {
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Komdigi DPW PKS Sumbar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Dashboard Kehadiran",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFFFFEDD5),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = QrCodeUtil.getTodayFormattedIndonesian(),
                                fontSize = 12.sp,
                                color = Color(0xFFFFEDD5),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Internship schedule & working hours pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📅 Periode: ${schedule.startDate} s/d ${schedule.endDate} • ${schedule.workDays.size} Hari/Pekan (${schedule.timeIn} - ${schedule.timeOut} WIB)",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom action inside banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Peserta: ${stats.totalParticipants} Orang",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Button(
                                onClick = onScanClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = PksOrangePrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                modifier = Modifier.testTag("btn_banner_scan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scan Absen",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Counters
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rekap Hari Ini",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Otomatis Alfa jika tak scan",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        title = "Hadir",
                        count = stats.hadirCount,
                        icon = Icons.Default.Check,
                        bgColor = PksHadirGreenBg,
                        textColor = PksHadirGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "Izin",
                        count = stats.izinCount,
                        icon = Icons.Default.MailOutline,
                        bgColor = PksIzinAmberBg,
                        textColor = PksIzinAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        title = "Sakit",
                        count = stats.sakitCount,
                        icon = Icons.Default.Hotel,
                        bgColor = PksSakitBlueBg,
                        textColor = PksSakitBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "Alfa",
                        count = stats.alfaCount,
                        icon = Icons.Default.Close,
                        bgColor = PksAlfaRedBg,
                        textColor = PksAlfaRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Highlight Row: Ceklis Al-Qur'an & Absen Keluar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        title = "Tilawah Qur'an",
                        count = stats.quranCompletedCount,
                        icon = Icons.Default.AutoStories,
                        bgColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "Absen Keluar",
                        count = stats.checkedOutCount,
                        icon = Icons.Default.ExitToApp,
                        bgColor = Color(0xFFEFF6FF),
                        textColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Today's Activity List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AKTIVITAS HARI INI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                TextButton(
                    onClick = onViewRekapClick,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Lihat Rekap",
                        color = PksOrangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (participants.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Belum ada peserta magang terdaftar.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(participants) { item ->
                ParticipantTodayCard(
                    item = item,
                    onCardClick = { onParticipantClick?.invoke(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatBox(
    title: String,
    count: Int,
    icon: ImageVector,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.testTag("stat_box_$title")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = textColor
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(textColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun ParticipantTodayCard(
    item: ParticipantWithAttendance,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
            )
            .testTag("card_participant_${item.participant.id}")
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
                // Avatar with initials
                val initials = item.participant.name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                    .ifBlank { "MG" }

                Box(
                    modifier = Modifier
                        .size(42.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

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
                        text = "Magang • ${item.participant.division}",
                        fontSize = 10.5.sp,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Keluar: ${if (item.todayTimeOut != "-") item.todayTimeOut else "Belum"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.todayTimeOut != "-") Color(0xFF2563EB) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quran & Activity Badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quran Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.isQuranRead) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = if (item.isQuranRead) Color(0xFF16A34A) else Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isQuranRead) {
                                if (item.quranSurahJuz.isNotBlank()) "Qur'an: ${item.quranSurahJuz}" else "Qur'an: Sudah Dibaca"
                            } else "Qur'an: Belum Dibaca",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isQuranRead) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }

                // Tap for checklist hint
                Text(
                    text = "Ketuk: Ceklis Kegiatan 📝",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
