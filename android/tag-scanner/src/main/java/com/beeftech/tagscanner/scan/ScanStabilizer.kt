package com.beeftech.tagscanner.scan

import com.beeftech.tagscanner.model.EarTagScanResult

/** Emits a result once the same (sequence, colour) appears [required] times in the last [window] offers. */
class ScanStabilizer(private val window: Int = 5, private val required: Int = 3) {
    private val history = ArrayDeque<EarTagScanResult>()

    fun offer(result: EarTagScanResult): EarTagScanResult? {
        history.addLast(result)
        while (history.size > window) history.removeFirst()
        val votes = history.count { it.sequence == result.sequence && it.colour == result.colour }
        return if (votes >= required) result else null
    }

    fun reset() = history.clear()
}
