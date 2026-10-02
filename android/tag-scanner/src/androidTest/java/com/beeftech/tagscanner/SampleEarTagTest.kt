package com.beeftech.tagscanner

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.beeftech.database.util.TagColour
import com.beeftech.tagscanner.colour.TagColourClassifier
import com.beeftech.tagscanner.colour.TagRegionSampler
import com.beeftech.tagscanner.model.EarTagScanResult
import com.beeftech.tagscanner.model.GuideRect
import com.beeftech.tagscanner.model.TextLineBox
import com.beeftech.tagscanner.parse.TagTextParser
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Golden tests: each sample tag must scan to its expected tag ID with the real ML Kit recognizer.
 * `eartag_yellow_000993.jpg` is a real photo; the others are generated from it by
 * `android/tag-scanner/tools/generate_sample_tags.sh`.
 */
@RunWith(Parameterized::class)
class SampleEarTagTest(
    private val asset: String,
    private val sequence: String,
    private val colour: TagColour,
    private val tagId: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun samples() = listOf(
            arrayOf("eartag_yellow_000993.jpg", "000993", TagColour.YELLOW, "Yel0000993"),
            arrayOf("eartag_red_000993.jpg", "000993", TagColour.RED, "Red0000993"),
            arrayOf("eartag_green_000993.jpg", "000993", TagColour.GREEN, "Grn0000993"),
            arrayOf("eartag_blue_000993.jpg", "000993", TagColour.BLUE, "Blu0000993"),
            arrayOf("eartag_yellow_004521.jpg", "004521", TagColour.YELLOW, "Yel0004521"),
            arrayOf("eartag_red_012876.jpg", "012876", TagColour.RED, "Red0012876"),
            arrayOf("eartag_green_000148.jpg", "000148", TagColour.GREEN, "Grn0000148"),
            arrayOf("eartag_blue_035062.jpg", "035062", TagColour.BLUE, "Blu0035062")
        )
    }

    @Test
    fun sampleTagScansToExpectedId() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open(asset).use { BitmapFactory.decodeStream(it) }
        assertNotNull(bitmap)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
        recognizer.close()

        val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            line.boundingBox?.let { TextLineBox(line.text, it.left, it.top, it.right, it.bottom) }
        }
        val guide = GuideRect(0, 0, bitmap.width, bitmap.height)
        val candidate = TagTextParser.parse(lines, guide)
        assertNotNull("No tag number found in lines: ${lines.map { it.text }}", candidate)

        val pixels = TagRegionSampler.sampleRing(bitmap, candidate!!.box)
        val classification = TagColourClassifier.classify(pixels)
        val result = EarTagScanResult(candidate.sequence, classification.colour, classification.confidence)

        assertEquals("lines=${lines.map { it.text }}", sequence, result.sequence)
        assertEquals("classification=$classification", colour, result.colour)
        assertEquals(tagId, result.tagId)
    }
}
