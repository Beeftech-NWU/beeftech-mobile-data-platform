package com.beeftech.tagscanner.model

/** A recognised line of text with its bounding box, in upright analysis-image coordinates. */
data class TextLineBox(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}
