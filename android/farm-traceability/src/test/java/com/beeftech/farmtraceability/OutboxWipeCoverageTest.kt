package com.beeftech.farmtraceability

import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.farmtraceability.worker.TraceabilityOutboxWorker
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The Day-7 wipe throws on a pending_sync type it does not know, which rolls the
 * whole wipe back. Every type the outbox can queue must be wipeable.
 */
class OutboxWipeCoverageTest {

    @Test
    fun everyOutboxTypeIsHandledByTheDay7Wipe() {

        val unhandled =
            TraceabilityOutboxWorker.SUPPORTED_ENTITY_TYPES -
                SyncSecurityDao.WIPEABLE_ENTITY_TYPES

        assertTrue(
            "Outbox types missing from SyncSecurityDao.enforceDay7: $unhandled",
            unhandled.isEmpty()
        )
    }
}
