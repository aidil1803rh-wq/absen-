package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.sync.SyncRole
import com.example.sync.SyncUiState
import com.example.ui.theme.PksAlfaRed
import com.example.ui.theme.PksAlfaRedBg
import com.example.ui.theme.PksAmberAccent
import com.example.ui.theme.PksHadirGreen
import com.example.ui.theme.PksHadirGreenBg
import com.example.ui.theme.PksIzinAmber
import com.example.ui.theme.PksIzinAmberBg
import com.example.ui.theme.PksKartuKuning
import com.example.ui.theme.PksKartuKuningBg
import com.example.ui.theme.PksOrangeDark
import com.example.ui.theme.PksOrangePrimary
import com.example.ui.theme.PksSakitBlue
import com.example.ui.theme.PksSakitBlueBg

@Composable
fun AppHeader(
    syncState: SyncUiState,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PksOrangePrimary, PksOrangeDark)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PKS Logo Emblem
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_pks_logo),
                    contentDescription = "Logo PKS",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KOMDIGI DPW PKS SUMBAR",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Sistem Absensi & Kokarde Digital",
                    fontSize = 11.sp,
                    color = Color(0xFFFFE4D6),
                    fontWeight = FontWeight.Medium
                )
                // Sync status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val (dotColor, label) = when {
                        syncState.isHostRunning -> Color(0xFF22C55E) to "Host Aktif (${syncState.connectedClientsCount} Klien)"
                        syncState.role == SyncRole.CLIENT && syncState.isConnectedToHost -> Color(0xFF38BDF8) to "Terhubung ke Host"
                        else -> Color(0xFFE2E8F0) to "Mode Mandiri"
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val displayStatus = when {
        status.isBlank() || status == "-" || status == "Belum Absen" -> "Belum Absen"
        else -> status
    }
    val (bgColor, textColor) = when (status) {
        "Hadir" -> PksHadirGreenBg to PksHadirGreen
        "Izin" -> PksIzinAmberBg to PksIzinAmber
        "Sakit" -> PksSakitBlueBg to PksSakitBlue
        "Alfa" -> PksAlfaRedBg to PksAlfaRed
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = displayStatus,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun YellowCardBadge(
    alfaCount: Int,
    modifier: Modifier = Modifier
) {
    if (alfaCount >= 3) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PksKartuKuningBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Kartu Kuning",
                tint = PksKartuKuning,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Kartu Kuning (${alfaCount}x Alfa)",
                color = PksKartuKuning,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
