package com.example.integrations.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class ImageInspectionOutput(
    val extractedText: String,
    val qrPayloads: List<String>,
    val ocrEmpty: Boolean
)

object ScreenshotOcrProcessor {

    suspend fun inspectImageUri(context: Context, uri: Uri): ImageInspectionOutput =
        withContext(Dispatchers.IO) {
            val bitmap = decodeScaledBitmap(context, uri)
                ?: return@withContext ImageInspectionOutput("", emptyList(), true)

            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val latinText = runTextRecognizer(
                    inputImage,
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                )
                val devText = runTextRecognizer(
                    inputImage,
                    TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                )

                val mergedText = mergeOcrResults(latinText, devText)
                val mlkitQrs = runMlKitBarcode(inputImage)
                val zxingQr = runZxingQr(bitmap)
                val allQrs = (mlkitQrs + listOfNotNull(zxingQr)).distinct()

                ImageInspectionOutput(
                    extractedText = mergedText,
                    qrPayloads = allQrs,
                    ocrEmpty = mergedText.isBlank() && allQrs.isEmpty()
                )
            } finally {
                bitmap.recycle()
            }
        }

    suspend fun decodeQrFromGalleryUri(context: Context, uri: Uri): List<String> =
        withContext(Dispatchers.IO) {
            val bitmap = decodeScaledBitmap(context, uri) ?: return@withContext emptyList()
            try {
                val mlkitQrs = runMlKitBarcode(InputImage.fromBitmap(bitmap, 0))
                val zxingQr = runZxingQr(bitmap)
                (mlkitQrs + listOfNotNull(zxingQr)).distinct()
            } finally {
                bitmap.recycle()
            }
        }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        var sample = 1
        val maxDim = 1600
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    private suspend fun runTextRecognizer(
        image: InputImage,
        recognizer: com.google.mlkit.vision.text.TextRecognizer
    ): String = suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                recognizer.close()
                cont.resume(visionText.text.trim())
            }
            .addOnFailureListener {
                recognizer.close()
                cont.resume("")
            }
    }

    private suspend fun runMlKitBarcode(image: InputImage): List<String> =
        suspendCancellableCoroutine { cont ->
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    scanner.close()
                    cont.resume(barcodes.mapNotNull { it.rawValue?.trim() }.filter { it.isNotEmpty() })
                }
                .addOnFailureListener {
                    scanner.close()
                    cont.resume(emptyList())
                }
        }

    private fun runZxingQr(bitmap: Bitmap): String? {
        return runCatching {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text?.trim()
        }.getOrNull()
    }

    private fun mergeOcrResults(latin: String, devanagari: String): String {
        val hasDevInSecond = devanagari.any { it in '\u0900'..'\u097F' }
        return when {
            latin.isBlank() -> devanagari
            !hasDevInSecond -> latin
            devanagari == latin -> latin
            else -> {
                // Keep unique lines from Devanagari pass that aren't already in Latin pass
                val latinLines = latin.lines().map { it.trim() }.toSet()
                val extraLines = devanagari.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it !in latinLines }
                if (extraLines.isEmpty()) latin else "$latin\n${extraLines.joinToString("\n")}"
            }
        }
    }
}
