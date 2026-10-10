package com.beeftech.database.repository

import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.entity.SyncRunTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRunSummaryTest {

    @Test
    fun `everything cleared is a success`() {
        assertEquals(SyncRunSummary(5, 0, SyncRunResult.SUCCESS), SyncRunSummary.of(before = 5, after = 0))
    }

    @Test
    fun `some cleared and some left is partial and counts the rest as failed`() {
        assertEquals(SyncRunSummary(3, 2, SyncRunResult.PARTIAL), SyncRunSummary.of(before = 5, after = 2))
    }

    @Test
    fun `nothing cleared while records wait is a failure`() {
        assertEquals(SyncRunSummary(0, 4, SyncRunResult.FAILED), SyncRunSummary.of(before = 4, after = 4))
    }

    @Test
    fun `an error with an empty queue is not a success`() {
        assertEquals(SyncRunResult.FAILED, SyncRunSummary.of(before = 0, after = 0, error = "boom").result)
        assertEquals(SyncRunResult.PARTIAL, SyncRunSummary.of(before = 3, after = 0, error = "late").result)
    }

    @Test
    fun `records added while the run was going never give a negative count`() {
        assertEquals(0, SyncRunSummary.of(before = 1, after = 6).syncedCount)
    }

    @Test
    fun `an automatic or scheduled run with nothing to do leaves no row but a manual one does`() {
        assertFalse(SyncRunSummary.worthRecording(SyncRunTrigger.AUTO, 0, 0, null))
        assertFalse(SyncRunSummary.worthRecording(SyncRunTrigger.MORNING, 0, 0, null))
        assertTrue(SyncRunSummary.worthRecording(SyncRunTrigger.MANUAL, 0, 0, null))
        assertTrue(SyncRunSummary.worthRecording(SyncRunTrigger.AUTO, 2, 0, null))
        assertTrue(SyncRunSummary.worthRecording(SyncRunTrigger.AUTO, 0, 0, "boom"))
    }

    @Test
    fun `an offline run is recorded once for a user triggered sync and never for automatic retries`() {
        assertTrue(SyncRunSummary.worthRecordingOffline(SyncRunTrigger.MANUAL, firstAttempt = true))
        assertTrue(SyncRunSummary.worthRecordingOffline(SyncRunTrigger.MORNING, firstAttempt = true))
        assertFalse(SyncRunSummary.worthRecordingOffline(SyncRunTrigger.MANUAL, firstAttempt = false))
        assertFalse(SyncRunSummary.worthRecordingOffline(SyncRunTrigger.AUTO, firstAttempt = true))
    }
}
