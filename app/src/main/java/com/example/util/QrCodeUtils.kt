package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

data class ParsedQrPayload(
    val skuCode: String,
    val referralCode: String?
)

object QrCodeUtils {

    fun buildProductQrPayload(skuCode: String, referralCode: String?): String {
        val cleanSku = skuCode.trim()
        val cleanRef = referralCode?.trim()?.takeIf { it.isNotEmpty() }
        return if (cleanRef != null) {
            "https://commissionwala.app/p/$cleanSku?ref=$cleanRef"
        } else {
            "https://commissionwala.app/p/$cleanSku"
        }
    }

    fun parseCommissionWalaQr(rawText: String): ParsedQrPayload? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        // Support full URLs, deep links, or direct SKU codes
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("commissionwala://", ignoreCase = true)
        ) {
            return try {
                val uri = Uri.parse(trimmed)
                val ref = uri.getQueryParameter("ref")?.trim()?.takeIf { it.isNotEmpty() }
                val segments = uri.pathSegments
                val sku = when {
                    segments.size >= 2 && (segments[0] == "p" || segments[0] == "product") -> segments[1]
                    segments.isNotEmpty() -> segments.last()
                    else -> uri.host.orEmpty()
                }.trim()
                if (sku.isNotEmpty()) ParsedQrPayload(skuCode = sku, referralCode = ref) else null
            } catch (_: Exception) {
                null
            }
        }

        // Support pipe or query-style strings e.g. "PRD-101|CW-AARAV-1042"
        if (trimmed.contains("|")) {
            val parts = trimmed.split("|")
            val sku = parts.getOrNull(0)?.trim().orEmpty()
            val ref = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            if (sku.isNotEmpty()) return ParsedQrPayload(sku, ref)
        }

        return ParsedQrPayload(skuCode = trimmed, referralCode = null)
    }

    fun generateQrBitmap(
        payload: String,
        sizePx: Int = 720,
        darkColor: Int = Color.parseColor("#1E1B4B"),
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    fun createBrandedQrShareBitmap(
        productName: String,
        priceText: String,
        commissionLabel: String,
        skuCode: String,
        referralCode: String?,
        payload: String
    ): Bitmap {
        val width = 900
        val height = 1160
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean off-white card backdrop
        canvas.drawColor(Color.parseColor("#F8F9FE"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cardRect = RectF(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat())
        canvas.drawRoundRect(cardRect, 44f, 44f, cardPaint)
        canvas.drawRoundRect(cardRect, 44f, 44f, borderPaint)

        // Top Purple Header Strip
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5B21B6")
        }
        val headerRect = RectF(40f, 40f, (width - 40).toFloat(), 190f)
        canvas.drawRoundRect(headerRect, 44f, 44f, headerPaint)
        canvas.drawRect(40f, 140f, (width - 40).toFloat(), 190f, headerPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CommissionWala", width / 2f, 112f, brandPaint)

        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DDD6FE")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCAN QR TO VIEW & ORDER PRODUCT", width / 2f, 156f, subBrandPaint)

        // QR Code
        val qrBitmap = generateQrBitmap(payload, sizePx = 600)
        val qrLeft = (width - qrBitmap.width) / 2f
        canvas.drawBitmap(qrBitmap, qrLeft, 235f, null)

        // Product Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val truncatedName = if (productName.length > 34) productName.take(31) + "..." else productName
        canvas.drawText(truncatedName, width / 2f, 900f, titlePaint)

        // Price & Commission
        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$priceText  •  $commissionLabel", width / 2f, 956f, pricePaint)

        // Referral / SKU Pill
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EDE9FE")
        }
        val pillRect = RectF(160f, 995f, (width - 160).toFloat(), 1068f)
        canvas.drawRoundRect(pillRect, 36f, 36f, pillPaint)

        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4C1D95")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val tagText = if (!referralCode.isNullOrBlank()) {
            "REF: $referralCode  |  $skuCode"
        } else {
            "PRODUCT SKU: $skuCode"
        }
        canvas.drawText(tagText, width / 2f, 1041f, refPaint)

        return bitmap
    }

    fun saveQrToGallery(context: Context, bitmap: Bitmap, fileNamePrefix: String): Result<String> {
        return try {
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CommissionWala")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    return Result.success(fileName)
                }
            }

            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val outDir = File(baseDir, "CommissionWala").apply { mkdirs() }
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareQrAndLink(
        context: Context,
        bitmap: Bitmap,
        shareText: String,
        targetPackage: String? = null
    ) {
        val qrDir = File(context.cacheDir, "qr_codes").apply { mkdirs() }
        val file = File(qrDir, "commissionwala_qr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
        }

        try {
            if (!targetPackage.isNullOrBlank()) {
                context.startActivity(baseIntent)
            } else {
                context.startActivity(Intent.createChooser(baseIntent, "Share Product QR & Affiliate Link"))
            }
        } catch (_: Exception) {
            // Fallback to generic system share sheet if WhatsApp/Instagram is not installed on emulator
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, "Share Product QR & Affiliate Link"))
        }
    }

    fun decodeQrFromImageProxy(imageProxy: ImageProxy): String? {
        return try {
            val plane = imageProxy.planes.firstOrNull() ?: return null
            val buffer = plane.buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val yBytes = ByteArray(width * height)
            if (rowStride == width && pixelStride == 1) {
                buffer.get(yBytes, 0, width * height)
            } else {
                val rowData = ByteArray(rowStride)
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    val length = minOf(rowStride, buffer.remaining())
                    buffer.get(rowData, 0, length)
                    for (col in 0 until width) {
                        yBytes[row * width + col] = rowData[col * pixelStride]
                    }
                }
            }

            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        } catch (_: Exception) {
            null
        }
    }

    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > 1000 || boundsOpts.outHeight / sampleSize > 1000) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        } catch (_: Exception) {
            null
        }
    }
}
