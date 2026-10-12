package com.example.ui.screens

import android.content.Context
import android.graphics.ImageFormat
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.Participant
import com.example.ui.theme.PksHadirGreen
import com.example.ui.theme.PksIzinAmber
import com.example.ui.theme.PksOrangePrimary
import com.example.ui.theme.PksSakitBlue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

enum class ScanMode {
    MASUK, KELUAR
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    participants: List<Participant>,
    onScanParticipant: (participantId: String, status: String) -> Unit,
    onCheckOutParticipant: ((participantId: String) -> Unit)? = null,
    onOpenChecklistForId: ((participantId: String) -> Unit)? = null,
    onSyncHostScanned: ((hostAddress: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var scanMode by remember { mutableStateOf(ScanMode.MASUK) }
    var selectedParticipant by remember { mutableStateOf(participants.firstOrNull()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var manualIdInput by remember { mutableStateOf("") }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var scanCooldown by remember { mutableStateOf(false) }

    LaunchedEffect(participants) {
        if (selectedParticipant == null && participants.isNotEmpty()) {
            selectedParticipant = participants.first()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector: Absen Masuk vs Absen Keluar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (scanMode == ScanMode.MASUK) Color(0xFF16A34A) else Color.Transparent)
                    .clickable { scanMode = ScanMode.MASUK }
                    .padding(vertical = 10.dp)
                    .testTag("tab_mode_masuk"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = null,
                        tint = if (scanMode == ScanMode.MASUK) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🟢 Absen Masuk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scanMode == ScanMode.MASUK) Color.White else Color(0xFF475569)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (scanMode == ScanMode.KELUAR) Color(0xFFDC2626) else Color.Transparent)
                    .clickable { scanMode = ScanMode.KELUAR }
                    .padding(vertical = 10.dp)
                    .testTag("tab_mode_keluar"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = if (scanMode == ScanMode.KELUAR) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🔴 Absen Keluar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scanMode == ScanMode.KELUAR) Color.White else Color(0xFF475569)
                    )
                }
            }
        }

        // Top instruction card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (scanMode == ScanMode.MASUK) Color(0xFFFFF7ED) else Color(0xFFFEF2F2)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (scanMode == ScanMode.MASUK) Color(0xFFFFEDD5) else Color(0xFFFECACA)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (scanMode == ScanMode.MASUK) "Mode: Scan Absen Masuk Magang" else "Mode: Scan Absen Keluar (Pulang)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (scanMode == ScanMode.MASUK) Color(0xFF9A3412) else Color(0xFF991B1B)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (scanMode == ScanMode.MASUK)
                        "Arahkan kamera ke QR ID Kokarde untuk mencatat jam masuk. Ceklis membaca Al-Qur'an utama diisi setelah masuk."
                    else
                        "Arahkan kamera ke QR ID Kokarde untuk mencatat jam pulang (absen keluar) peserta magang.",
                    fontSize = 11.sp,
                    color = if (scanMode == ScanMode.MASUK) Color(0xFFC2410C) else Color(0xFFB91C1C),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Camera Preview / Scanner Box
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .testTag("box_camera_scanner")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted) {
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val decoded = decodeQrFromImage(imageProxy)
                                    imageProxy.close()

                                    if (decoded != null && !scanCooldown) {
                                        scanCooldown = true
                                        lastScannedCode = decoded

                                        var cleanId = decoded.trim()
                                        if (cleanId.startsWith("PKS-SYNC:")) {
                                            onSyncHostScanned?.invoke(cleanId)
                                        } else {
                                            if (cleanId.contains("-")) {
                                                cleanId = cleanId.substringAfterLast("-").trim()
                                            }
                                            if (scanMode == ScanMode.KELUAR) {
                                                if (onCheckOutParticipant != null) {
                                                    onCheckOutParticipant(cleanId)
                                                } else {
                                                    onScanParticipant(cleanId, "Hadir")
                                                }
                                            } else {
                                                onScanParticipant(cleanId, "Hadir")
                                            }
                                        }

                                        // Reset cooldown after 3 seconds
                                        previewView.postDelayed({
                                            scanCooldown = false
                                        }, 3000)
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    DisposableEffect(Unit) {
                        onDispose {
                            cameraExecutor.shutdown()
                        }
                    }

                    // Scanner Viewfinder Overlay
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .border(2.dp, Color(0xFFF97316), RoundedCornerShape(16.dp))
                    )

                    // Animated scan laser
                    val transition = rememberInfiniteTransition(label = "scan_laser")
                    val laserOffsetY by transition.animateFloat(
                        initialValue = -80f,
                        targetValue = 80f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser"
                    )

                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .height(2.dp)
                            .background(Color(0xFFFF5722))
                    )

                    // Bottom status overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (scanCooldown) "Absen diproses: $lastScannedCode"
                            else if (scanMode == ScanMode.MASUK) "🟢 Mode Masuk: Arahkan ke QR kokarde..."
                            else "🔴 Mode Pulang: Arahkan ke QR untuk Absen Keluar...",
                            color = Color(0xFFFFD8A8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Izin Kamera Diperlukan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aktifkan kamera untuk scan QR, atau gunakan presensi manual di bawah.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = PksOrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Izinkan Kamera", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Manual Presensi Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "ATAU PRESENSI MANUAL CEPAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pilih Peserta Magang",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dropdown selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable { isDropdownExpanded = true }
                            .padding(12.dp)
                            .testTag("dropdown_select_participant")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedParticipant?.let {
                                    val nimVal = it.nim.ifBlank { it.id }
                                    "${it.name} (NIM: $nimVal)"
                                } ?: "Pilih Peserta Magang...",
                                fontSize = 12.sp,
                                color = if (selectedParticipant != null) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        participants.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val nimVal = p.nim.ifBlank { p.id }
                                        Text("NIM: $nimVal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                        Text("${p.division} • ${p.university}", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                    }
                                },
                                onClick = {
                                    selectedParticipant = p
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (scanMode == ScanMode.MASUK) {
                    // Quick 3 status buttons for Absen Masuk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedParticipant?.let { onScanParticipant(it.id, "Hadir") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PksHadirGreen),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedParticipant != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_quick_hadir")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hadir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedParticipant?.let { onScanParticipant(it.id, "Izin") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PksIzinAmber),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedParticipant != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_quick_izin")
                        ) {
                            Icon(imageVector = Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Izin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedParticipant?.let { onScanParticipant(it.id, "Sakit") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PksSakitBlue),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedParticipant != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_quick_sakit")
                        ) {
                            Icon(imageVector = Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sakit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Big Absen Keluar button for Absen Pulang
                    Button(
                        onClick = {
                            selectedParticipant?.let { p ->
                                if (onCheckOutParticipant != null) {
                                    onCheckOutParticipant(p.id)
                                } else {
                                    onScanParticipant(p.id, "Hadir")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedParticipant != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_quick_checkout")
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Catat Absen Keluar (Pulang)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ceklis Al-Quran & Logbook Button
                OutlinedButton(
                    onClick = {
                        selectedParticipant?.let { p ->
                            onOpenChecklistForId?.invoke(p.id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_open_checklist_scan"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedParticipant != null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF16A34A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📖 Ceklis Al-Qur'an & Logbook Kegiatan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun decodeQrFromImage(imageProxy: ImageProxy): String? {
    return try {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            bytes, width, height,
            0, 0, width, height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val result = reader.decode(bitmap)
        result.text
    } catch (e: Exception) {
        null
    }
}
