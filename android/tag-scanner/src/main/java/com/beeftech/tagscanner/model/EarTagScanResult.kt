package com.beeftech.tagscanner.model

import com.beeftech.database.util.TagColour
import com.beeftech.database.util.TagNamingUtils

data class EarTagScanResult(
    val sequence: String,
    val colour: TagColour?,
    val colourConfidence: Float
) {
    val tagId: String? get() = colour?.let { TagNamingUtils.formatTag(it, sequence.toLong()) }
}
