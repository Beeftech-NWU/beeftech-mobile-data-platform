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

/** Golden test: the sample yellow tag must scan to Yel0000993 with the real ML Kit recognizer. */
class SampleEarTagTest {

    @Test
    fun sampleYellowTagScansToYel0000993() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open("eartag_yellow_000993.jpg").use { BitmapFactory.decodeStream(it) }
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

        assertEquals("000993", result.sequence)
        assertEquals("classification=$classification", TagColour.YELLOW, result.colour)
        assertEquals("Yel0000993", result.tagId)
    }
}
