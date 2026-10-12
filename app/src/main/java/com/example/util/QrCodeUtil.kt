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
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
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

        // Header Titles
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
            letterSpacing = 0.02f
        }
        canvas.drawText("KOMDIGI DPW PKS SUMBAR", 154f, 88f, headerTitlePaint)

        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFEDD5")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 19f
            letterSpacing = 0.03f
        }
        canvas.drawText("KARTU TANDA PESERTA MAGANG", 154f, 124f, headerSubPaint)

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

        // Generate & Draw QR Code
        val qrBitmap = generateQrBitmap("PKS-MAGANG-${participant.id}", 390, 390)
        canvas.drawBitmap(qrBitmap, 205f, 230f, antiAliasPaint)

        // Participant Name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (participant.name.length > 20) 28f else 34f
        }
        canvas.drawText(participant.name, 400f, 688f, namePaint)

        // NIM below name
        val nimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 23f
            letterSpacing = 0.04f
        }
        val nimText = "NIM: " + (participant.nim.ifBlank { participant.id })
        canvas.drawText(nimText, 400f, 724f, nimPaint)

        // Subtitle "MAGANG DI KOMDIGI"
        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 21f
            letterSpacing = 0.05f
        }
        canvas.drawText("MAGANG DI KOMDIGI", 400f, 762f, rolePaint)

        // Division
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (participant.division.length > 26) 20f else 24f
        }
        canvas.drawText(participant.division, 400f, 802f, divPaint)

        // University
        val uniPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = 21f
        }
        canvas.drawText(participant.university, 400f, 840f, uniPaint)

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
        canvas.drawText("DPW PKS SUMATERA BARAT", 400f, 968f, officialInstPaint)

        val scanInstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            textSize = 18f
        }
        canvas.drawText("Pindai QR Code untuk Presensi Harian", 400f, 1024f, scanInstructionPaint)

        // 4. Bottom ID Bar
        val bottomBarRect = RectF(40f, 1162f, width - 40f, 1234f)
        val bottomBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(125, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bottomBarRect, 20f, 20f, bottomBarPaint)

        val idLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFEDD5")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
        }
        canvas.drawText("ID PESERTA:", 68f, 1206f, idLabelPaint)

        val idValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            textSize = 26f
        }
        canvas.drawText(participant.id, width - 68f, 1206f, idValuePaint)

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
        val qrBitmap = generateQrBitmap("PKS-MAGANG-${participant.id}", 600, 600)
        val filename = "QR_Magang_${participant.id}"
        val success = saveBitmapToGallery(context, qrBitmap, filename)
        if (success) {
            Toast.makeText(
                context,
                "✓ QR Code ${participant.id} berhasil disimpan ke Galeri!",
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
                putExtra(Intent.EXTRA_SUBJECT, "Rekap Absensi Magang Komdigi PKS Sumbar")
                putExtra(Intent.EXTRA_TEXT, "Data Rekap Absensi Magang Komdigi DPW PKS Sumbar")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Rekap Absensi"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
