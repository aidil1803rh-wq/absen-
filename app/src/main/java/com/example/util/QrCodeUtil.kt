package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.InternshipSchedule
import com.example.data.model.Participant
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale

object QrCodeUtil {

    fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        }

        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints
        )

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Renders a high-resolution, print-ready Kokarde ID Card (aspect ratio 1 : 1.58, 800x1264 px)
     * including PKS DPW Sumbar branding, logo, QR code, participant name, division, university, and ID.
     */
    fun generateKokardeCardBitmap(context: Context, participant: Participant): Bitmap {
        val width = 800
        val height = 1264 // 800 * 1.58
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val antiAliasPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Outer Gradient Background (Orange PKS)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#EA580C"),
                    Color.parseColor("#F97316"),
                    Color.parseColor("#9A3412")
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(cardRect, 44f, 44f, bgPaint)

        // Outer White Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.WHITE
        }
        val innerBorderRect = RectF(5f, 5f, width - 5f, height - 5f)
        canvas.drawRoundRect(innerBorderRect, 40f, 40f, borderPaint)

        // 2. Top Header Pill
        val headerRect = RectF(40f, 36f, width - 40f, 156f)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(headerRect, 26f, 26f, headerBgPaint)

        // Logo circle
        val logoCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(98f, 96f, 38f, logoCirclePaint)

        // Decode PKS Logo
        try {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_pks_logo)
            if (logoBitmap != null) {
                val logoDest = RectF(66f, 64f, 130f, 128f)
                canvas.drawBitmap(logoBitmap, null, logoDest, antiAliasPaint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Header Titles - Enlarged MAGANG, removed KOMDIGI DPW text
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 38f
            letterSpacing = 0.04f
        }
        canvas.drawText("MAGANG", 154f, 92f, headerTitlePaint)

        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFEDD5")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 19f
            letterSpacing = 0.03f
        }
        canvas.drawText("KARTU TANDA PESERTA", 154f, 126f, headerSubPaint)

        // 3. Center White Box
        val centerCardRect = RectF(40f, 180f, width - 40f, 1140f)
        val centerCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(centerCardRect, 32f, 32f, centerCardPaint)

        // QR Container Border
        val qrBoxRect = RectF(190f, 215f, 610f, 635f)
        val qrBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF7ED")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(qrBoxRect, 20f, 20f, qrBoxBgPaint)

        val qrBoxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FED7AA")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(qrBoxRect, 20f, 20f, qrBoxBorderPaint)

        // Generate & Draw QR Code using Student's NIM directly
        val studentNim = participant.nim.ifBlank { participant.id }
        val qrBitmap = generateQrBitmap(studentNim, 390, 390)
        canvas.drawBitmap(qrBitmap, 205f, 230f, antiAliasPaint)

        // Participant Name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (participant.name.length > 20) 27f else 32f
        }
        canvas.drawText(participant.name, 400f, 676f, namePaint)

        // Nickname Pill below name
        val nicknameVal = participant.nickname.ifBlank { participant.name.split(" ").firstOrNull() ?: "" }
        if (nicknameVal.isNotBlank()) {
            val nickText = "\"$nicknameVal\""
            val nickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EA580C")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = 21f
            }
            val textWidth = nickPaint.measureText(nickText)
            val pillPaddingX = 22f
            val pillLeft = 400f - (textWidth / 2f) - pillPaddingX
            val pillRight = 400f + (textWidth / 2f) + pillPaddingX
            val pillTop = 688f
            val pillBottom = 720f

            val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFEDD5")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(pillLeft, pillTop, pillRight, pillBottom), 16f, 16f, pillBgPaint)
            canvas.drawText(nickText, 400f, 712f, nickPaint)
        }

        // NIM below nickname
        val nimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 22f
            letterSpacing = 0.04f
        }
        val nimText = "NIM: " + (participant.nim.ifBlank { participant.id })
        canvas.drawText(nimText, 400f, 750f, nimPaint)

        // Subtitle "PESERTA MAGANG"
        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 20f
            letterSpacing = 0.05f
        }
        canvas.drawText("PESERTA MAGANG", 400f, 784f, rolePaint)

        // Division
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (participant.division.length > 26) 20f else 23f
        }
        canvas.drawText(participant.division, 400f, 818f, divPaint)

        // University
        val uniPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = 20f
        }
        canvas.drawText(participant.university, 400f, 850f, uniPaint)

        // Divider
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            strokeWidth = 2.5f
        }
        canvas.drawLine(120f, 880f, 680f, 880f, dividerPaint)

        // Official badge text
        val officialSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = 19f
        }
        canvas.drawText("Identitas Resmi Peserta Magang", 400f, 928f, officialSubPaint)

        val officialInstPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 21f
            letterSpacing = 0.02f
        }
        canvas.drawText("PROGRAM MAGANG RESMI", 400f, 968f, officialInstPaint)

        val scanInstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            textSize = 18f
        }
        canvas.drawText("Pindai QR Code untuk Presensi Harian", 400f, 1024f, scanInstructionPaint)

        return bitmap
    }

    /**
     * Saves a Bitmap to public Pictures gallery so it appears immediately in the Photos / Gallery app.
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        subfolder: String = "Kokarde Magang"
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subfolder")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                // Fallback for devices where MediaStore insert returns null
                saveBitmapFallback(context, bitmap, displayName, subfolder)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            saveBitmapFallback(context, bitmap, displayName, subfolder)
        }
    }

    private fun saveBitmapFallback(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        subfolder: String
    ): Boolean {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, subfolder).apply { mkdirs() }
            val file = File(appDir, "$displayName.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Downloads and saves the full official Kokarde ID Card of the participant to device Gallery.
     */
    fun downloadKokarde(context: Context, participant: Participant): Boolean {
        val kokardeBitmap = generateKokardeCardBitmap(context, participant)
        val cleanName = participant.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val filename = "Kokarde_${participant.id}_$cleanName"
        val success = saveBitmapToGallery(context, kokardeBitmap, filename)
        if (success) {
            Toast.makeText(
                context,
                "✓ Kokarde ${participant.name} berhasil diunduh ke Galeri!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Gagal mengunduh Kokarde. Periksa izin penyimpanan.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return success
    }

    /**
     * Downloads and saves only the QR Code bitmap of the participant to device Gallery.
     */
    fun downloadQrCode(context: Context, participant: Participant): Boolean {
        val studentNim = participant.nim.ifBlank { participant.id }
        val qrBitmap = generateQrBitmap(studentNim, 600, 600)
        val filename = "QR_Magang_$studentNim"
        val success = saveBitmapToGallery(context, qrBitmap, filename)
        if (success) {
            Toast.makeText(
                context,
                "✓ QR Code NIM $studentNim berhasil disimpan ke Galeri!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Gagal mengunduh QR Code.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return success
    }

    /**
     * Shares the complete Kokarde card image via WhatsApp, Telegram, etc.
     */
    fun shareKokardeCard(context: Context, participant: Participant) {
        try {
            val kokardeBitmap = generateKokardeCardBitmap(context, participant)
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "Kokarde_${participant.id}.png")
            val stream = FileOutputStream(file)
            kokardeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Kokarde Magang ${participant.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Kartu Tanda Peserta Magang Resmi: ${participant.name} (${participant.id}) - Divisi ${participant.division}, Komdigi DPW PKS Sumbar"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Kokarde Magang"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan kokarde: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getTodayFormattedIndonesian(dateStr: String = getTodayDateString()): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdfInput.parse(dateStr) ?: Date()
            val sdfOutput = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            sdfOutput.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, title: String, filename: String) {
        try {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "$filename.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title - Komdigi DPW PKS Sumbar")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan $title"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDayName(day: Int): String = when (day) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        7 -> "Minggu"
        else -> "Hari $day"
    }

    fun shareCsv(context: Context, csvContent: String, filename: String) {
        try {
            val cachePath = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(cachePath, "$filename.csv")
            file.writeText(csvContent)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Absensi Magang & Pengaturan")
                putExtra(Intent.EXTRA_TEXT, "Data Rekap Absensi & Pengaturan Magang Mahasiswa")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Rekap Absensi (CSV)"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title - Absensi & Pengaturan Magang")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndSharePdf(
        context: Context,
        internshipSchedule: InternshipSchedule,
        participants: List<Participant>,
        records: List<AttendanceRecord>,
        dateFilter: String? = null
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1

            val participantMap = participants.associateBy { it.id }
            val dayNames = internshipSchedule.workDays.map { getDayName(it) }.joinToString(", ")
            val exportTime = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 15f
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 9.5f
            }
            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9f
            }
            val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 8.5f
            }
            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.5f
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 8f
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }
            val orangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F97316")
                style = Paint.Style.FILL
            }
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            val boxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFF7ED")
                style = Paint.Style.FILL
            }
            val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FED7AA")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            fun drawHeader(isFirstPage: Boolean): Float {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, orangePaint)
                if (isFirstPage) {
                    canvas.drawText("LAPORAN ABSENSI & PENGATURAN MAGANG", 36f, 34f, titlePaint)
                    canvas.drawText("Universitas Islam Negeri (UIN) Imam Bonjol Padang • KPI", 36f, 48f, subPaint)
                    canvas.drawText("Diekspor: $exportTime WIB", 36f, 60f, footerPaint)
                    canvas.drawLine(36f, 68f, 559f, 68f, linePaint)

                    val boxRect = RectF(36f, 76f, 559f, 142f)
                    canvas.drawRoundRect(boxRect, 8f, 8f, boxBgPaint)
                    canvas.drawRoundRect(boxRect, 8f, 8f, boxBorderPaint)

                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#C2410C")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 9f
                    }
                    canvas.drawText("PENGATURAN & JADWAL PROGRAM MAGANG", 48f, 92f, labelPaint)
                    canvas.drawText("• Periode Magang : ${internshipSchedule.startDate} s/d ${internshipSchedule.endDate}", 48f, 107f, normalPaint)
                    canvas.drawText("• Jam Operasional: ${internshipSchedule.timeIn} - ${internshipSchedule.timeOut} WIB", 48f, 121f, normalPaint)
                    canvas.drawText("• Hari Kerja Aktif : $dayNames (${internshipSchedule.workDays.size} hari/minggu)", 48f, 134f, normalPaint)

                    canvas.drawText("• Total Peserta: ${participants.size} orang", 310f, 107f, normalPaint)
                    canvas.drawText("• Filter: ${dateFilter ?: "Semua Tanggal (Rekap Lengkap)"}", 310f, 121f, normalPaint)
                    canvas.drawText("• Total Data Absen: ${records.size} catatan", 310f, 134f, normalPaint)

                    return 154f
                } else {
                    canvas.drawText("LAPORAN ABSENSI MAGANG (Lanjutan)", 36f, 26f, boldPaint)
                    canvas.drawLine(36f, 32f, 559f, 32f, linePaint)
                    return 42f
                }
            }

            fun drawTableHeader(startY: Float) {
                val thRect = RectF(36f, startY, 559f, startY + 18f)
                canvas.drawRect(thRect, tableHeaderPaint)

                canvas.drawText("No", 42f, startY + 12f, headerTextPaint)
                canvas.drawText("Tanggal", 62f, startY + 12f, headerTextPaint)
                canvas.drawText("NIM (Kode)", 116f, startY + 12f, headerTextPaint)
                canvas.drawText("Nama Peserta", 182f, startY + 12f, headerTextPaint)
                canvas.drawText("Masuk/Keluar", 298f, startY + 12f, headerTextPaint)
                canvas.drawText("Status", 366f, startY + 12f, headerTextPaint)
                canvas.drawText("Al-Qur'an", 412f, startY + 12f, headerTextPaint)
                canvas.drawText("Kegiatan / Logbook", 466f, startY + 12f, headerTextPaint)
            }

            var currentY = drawHeader(true)
            drawTableHeader(currentY)
            currentY += 24f

            val rowAltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val statusColors = mapOf(
                "Hadir" to Color.parseColor("#15803D"),
                "Izin" to Color.parseColor("#1D4ED8"),
                "Sakit" to Color.parseColor("#B45309"),
                "Alfa" to Color.parseColor("#B91C1C")
            )

            records.forEachIndexed { index, rec ->
                if (currentY > 790f) {
                    canvas.drawLine(36f, 810f, 559f, 810f, linePaint)
                    canvas.drawText("Dokumen Laporan Resmi Absensi Magang • Halaman $pageNumber", 36f, 824f, footerPaint)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    currentY = drawHeader(false)
                    drawTableHeader(currentY)
                    currentY += 24f
                }

                if (index % 2 == 1) {
                    canvas.drawRect(36f, currentY - 10f, 559f, currentY + 10f, rowAltPaint)
                }

                val p = participantMap[rec.participantId]
                val nim = p?.nim?.ifBlank { p.id } ?: rec.participantId
                val nick = if (p?.nickname.isNullOrBlank()) "" else " (${p?.nickname})"
                val rawName = (p?.name ?: "Unknown") + nick
                val nameDisplay = if (rawName.length > 20) rawName.take(19) + "…" else rawName
                val timeDisplay = "${rec.time} / ${rec.timeOut}"
                val quranText = if (rec.isQuranRead) "✓ ${rec.quranSurahJuz.take(7)}" else "Belum"
                val actRaw = (rec.dailyActivities.ifBlank { rec.activityNotes }).replace(";", " | ")
                val actDisplay = if (actRaw.length > 20) actRaw.take(19) + "…" else actRaw

                canvas.drawText("${index + 1}", 42f, currentY, normalPaint)
                canvas.drawText(rec.date, 62f, currentY, normalPaint)
                canvas.drawText(nim, 116f, currentY, boldPaint)
                canvas.drawText(nameDisplay, 182f, currentY, normalPaint)
                canvas.drawText(timeDisplay, 298f, currentY, normalPaint)

                val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = statusColors[rec.status] ?: Color.parseColor("#334155")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 8.5f
                }
                canvas.drawText(rec.status, 366f, currentY, statusPaint)
                canvas.drawText(quranText, 412f, currentY, normalPaint)
                canvas.drawText(actDisplay, 466f, currentY, normalPaint)

                currentY += 18f
            }

            canvas.drawLine(36f, 810f, 559f, 810f, linePaint)
            canvas.drawText("Dokumen Laporan Resmi Absensi Magang • Halaman $pageNumber (Selesai)", 36f, 824f, footerPaint)
            pdfDocument.finishPage(page)

            val cachePath = File(context.cacheDir, "exports").apply { mkdirs() }
            val filename = if (dateFilter != null) "Laporan_Absensi_Magang_$dateFilter" else "Laporan_Lengkap_Absensi_Magang"
            val file = File(cachePath, "$filename.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            sharePdf(context, file, filename)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateAndShareCsv(
        context: Context,
        internshipSchedule: InternshipSchedule,
        participants: List<Participant>,
        records: List<AttendanceRecord>,
        dateFilter: String? = null
    ) {
        val participantMap = participants.associateBy { it.id }
        val dayNames = internshipSchedule.workDays.map { getDayName(it) }.joinToString(", ")

        val sb = StringBuilder()
        sb.append("# =====================================================\n")
        sb.append("# PENGATURAN PROGRAM MAGANG\n")
        sb.append("# =====================================================\n")
        sb.append("Periode_Mulai,${internshipSchedule.startDate}\n")
        sb.append("Periode_Selesai,${internshipSchedule.endDate}\n")
        sb.append("Jam_Masuk,${internshipSchedule.timeIn}\n")
        sb.append("Jam_Keluar,${internshipSchedule.timeOut}\n")
        sb.append("Hari_Kerja_Aktif,\"$dayNames\"\n")
        sb.append("Total_Hari_Kerja_Mingguan,${internshipSchedule.workDays.size}\n")
        sb.append("Total_Peserta,${participants.size}\n")
        sb.append("Filter_Tanggal,${dateFilter ?: "Semua Tanggal (Rekap Lengkap)"}\n\n")

        sb.append("# =====================================================\n")
        sb.append("# DATA KEHADIRAN DAN LOGBOOK PESERTA MAGANG\n")
        sb.append("# =====================================================\n")
        sb.append("Tanggal,NIM,Nama_Lengkap,Nama_Panggilan,Divisi,Universitas,Jam_Masuk,Jam_Keluar,Status,Membaca_AlQuran,Detail_Tilawah,Kegiatan_Harian,Catatan\n")

        for (rec in records) {
            val p = participantMap[rec.participantId]
            val nim = p?.nim?.ifBlank { p.id } ?: rec.participantId
            val name = (p?.name ?: "Unknown").replace("\"", "'")
            val nickname = (p?.nickname ?: "").replace("\"", "'")
            val div = (p?.division ?: "-").replace("\"", "'")
            val uni = (p?.university ?: "-").replace("\"", "'")
            val quran = if (rec.isQuranRead) "Sudah" else "Belum"
            val tilawah = rec.quranSurahJuz.replace("\"", "'")
            val acts = rec.dailyActivities.replace(";", " | ").replace("\"", "'")
            val notes = rec.activityNotes.replace("\"", "'")
            sb.append("${rec.date},\"$nim\",\"$name\",\"$nickname\",\"$div\",\"$uni\",${rec.time},${rec.timeOut},${rec.status},\"$quran\",\"$tilawah\",\"$acts\",\"$notes\"\n")
        }

        val filename = if (dateFilter != null) "Rekap_Absensi_Magang_$dateFilter" else "Rekap_Lengkap_Absensi_Magang"
        shareCsv(context, sb.toString(), filename)
    }
}
