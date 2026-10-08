package com.beeftech.demoapp

import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.SyncRunSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncAllDispatcherTest {

    @Test
    fun `every module has its own work name, including the traceability outbox`() {
        assertEquals(7, SyncAllDispatcher.WORK_NAMES.size)
        assertEquals(SyncAllDispatcher.WORK_NAMES.size, SyncAllDispatcher.WORK_NAMES.toSet().size)
        assertEquals(true, "traceability-outbox-scheduled-sync" in SyncAllDispatcher.WORK_NAMES)
    }

    @Test
    fun `the trigger travels to the workers as input data`() {
        val data = SyncAllDispatcher.triggerData(SyncRunTrigger.EVENING)

        assertEquals(SyncRunTrigger.EVENING, data.getString(SyncRunSummary.TRIGGER_INPUT_KEY))
    }
}
