package com.beeftech.tagscanner.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.beeftech.tagscanner.colour.TagColourClassifier
import com.beeftech.tagscanner.colour.TagRegionSampler
import com.beeftech.tagscanner.model.EarTagScanResult
import com.beeftech.tagscanner.model.GuideRect
import com.beeftech.tagscanner.model.TextLineBox
import com.beeftech.tagscanner.parse.TagTextParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Per-frame pipeline: ML Kit OCR → number parse → colour sampling → stabilizer.
 * [guideFraction] is the guide rectangle as 0..1 fractions of the upright frame.
 */
@ExperimentalGetImage
class EarTagAnalyzer(
    private val guideFraction: RectF,
    private val onStableResult: (EarTagScanResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val stabilizer = ScanStabilizer()
    private val paused = AtomicBoolean(false)

    fun pause() {
        paused.set(true)
    }

    fun resume() {
        stabilizer.reset()
        paused.set(false)
    }

    fun close() {
        recognizer.close()
    }

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (paused.get() || mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val swap = rotation == 90 || rotation == 270
        val width = if (swap) imageProxy.height else imageProxy.width
        val height = if (swap) imageProxy.width else imageProxy.height
        val guide = GuideRect(
            (guideFraction.left * width).toInt(),
            (guideFraction.top * height).toInt(),
            (guideFraction.right * width).toInt(),
            (guideFraction.bottom * height).toInt()
        )

        recognizer.process(InputImage.fromMediaImage(mediaImage, rotation))
            .addOnSuccessListener { text ->
                if (paused.get()) return@addOnSuccessListener
                val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                    line.boundingBox?.let { TextLineBox(line.text, it.left, it.top, it.right, it.bottom) }
                }
                val candidate = TagTextParser.parse(lines, guide)
                if (candidate == null) {
                    return@addOnSuccessListener
                }
                // Only build the bitmap once a number was found, to keep the per-frame cost low.
                val upright = rotate(imageProxy.toBitmap(), rotation)
                val pixels = TagRegionSampler.sampleRing(upright, candidate.box)
                val classification = TagColourClassifier.classify(pixels)
                val result = EarTagScanResult(candidate.sequence, classification.colour, classification.confidence)
                stabilizer.offer(result)?.let(onStableResult)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
