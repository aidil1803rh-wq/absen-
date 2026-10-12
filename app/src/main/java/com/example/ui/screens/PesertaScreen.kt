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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Participant
import com.example.data.model.ParticipantWithAttendance
import com.example.ui.components.YellowCardBadge
import com.example.ui.theme.PksOrangeLight
import com.example.ui.theme.PksOrangePrimary
import com.example.util.QrCodeUtil

@Composable
fun PesertaScreen(
    participantsWithStats: List<ParticipantWithAttendance>,
    onAddParticipantClick: () -> Unit,
    onEditParticipant: (Participant) -> Unit,
    onOpenKokarde: (Participant) -> Unit,
    onDeleteParticipant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var participantToDelete by remember { mutableStateOf<Participant?>(null) }

    val filteredList = remember(participantsWithStats, searchQuery) {
        if (searchQuery.isBlank()) {
            participantsWithStats
        } else {
            participantsWithStats.filter {
                it.participant.name.contains(searchQuery, ignoreCase = true) ||
                        it.participant.division.contains(searchQuery, ignoreCase = true) ||
                        it.participant.university.contains(searchQuery, ignoreCase = true) ||
                        it.participant.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
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
                            text = "Data Peserta & Kokarde",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Rasio standar 1 : 1.58 • Siap cetak ke galeri",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (participantsWithStats.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    var count = 0
                                    participantsWithStats.forEach {
                                        if (QrCodeUtil.downloadKokarde(context, it.participant)) count++
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 8.dp
                                ),
                                modifier = Modifier.testTag("btn_download_all_kokarde")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Unduh Semua",
                                    tint = PksOrangePrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unduh Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PksOrangePrimary)
                            }
                        }

                        Button(
                            onClick = onAddParticipantClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),
                            modifier = Modifier.testTag("btn_add_participant")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nama, NIM, atau divisi...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_participant")
        )

        // Participants List
        if (filteredList.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Belum ada data peserta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "Ketuk tombol '+ Tambah' untuk mendaftarkan peserta magang baru.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onAddParticipantClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_add_participant_empty_state")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Peserta Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { item ->
                    ParticipantRowCard(
                        item = item,
                        onEditClick = { onEditParticipant(item.participant) },
                        onKokardeClick = { onOpenKokarde(item.participant) },
                        onDeleteClick = { participantToDelete = item.participant }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    participantToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { participantToDelete = null },
            title = { Text("Hapus Peserta Magang?", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus data '${p.name}' (${p.id}) beserta seluruh riwayat absensinya?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteParticipant(p.id)
                        participantToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { participantToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ParticipantRowCard(
    item: ParticipantWithAttendance,
    onEditClick: () -> Unit,
    onKokardeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("participant_item_${item.participant.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PksOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PksOrangePrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.participant.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF0F172A)
                    )
                    val nick = item.participant.nickname.ifBlank { item.participant.name.split(" ").firstOrNull() ?: "" }
                    if (nick.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PksOrangeLight)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "\"$nick\"",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PksOrangePrimary
                            )
                        }
                    }
                }

                val nimDisplay = item.participant.nim.ifBlank { item.participant.id }
                Text(
                    text = "NIM: $nimDisplay",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Text(
                    text = "${item.participant.division} • ${item.participant.university}",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Alfa: ${item.totalAlfa}x",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (item.totalAlfa >= 3) {
                        Spacer(modifier = Modifier.width(6.dp))
                        YellowCardBadge(alfaCount = item.totalAlfa)
                    }
                }
            }

            // Buttons: Edit, Kokarde, Quick Download & Delete
            val rowContext = LocalContext.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .testTag("btn_edit_${item.participant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Peserta",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = onKokardeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PksOrangeLight,
                        contentColor = PksOrangePrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 9.dp,
                        vertical = 6.dp
                    ),
                    modifier = Modifier.testTag("btn_kokarde_${item.participant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Kokarde",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kokarde", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        QrCodeUtil.downloadKokarde(rowContext, item.participant)
                    },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF7ED))
                        .testTag("btn_quick_download_${item.participant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Unduh Kokarde",
                        tint = PksOrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(32.dp)
                        .testTag("btn_delete_${item.participant.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
