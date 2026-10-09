package com.beeftech.calfregistration.util

import androidx.core.content.FileProvider

/**
 * A FileProvider of our own. A host app often declares the stock androidx FileProvider too, and
 * two providers of the same class cannot be merged into one manifest.
 */
class CalfFileProvider : FileProvider()
