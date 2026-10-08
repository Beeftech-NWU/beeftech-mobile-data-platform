package com.beeftech.calfregistration.util

import com.beeftech.database.security.SyncIdentityRegistry
import com.beeftech.database.util.FileNamingUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CalfPhotoNameTest {

    @get:Rule
    val folder = TemporaryFolder()

    @After
    fun tearDown() = SyncIdentityRegistry.clear()

    private val now = 1_791_468_309_000L

    @Test
    fun `a device that knows its farm code names the photo with the agreed pattern`() {
        SyncIdentityRegistry.set("BF01", "MOB_DEV_a1b2c3d4")

        val name = CalfPhotoCapture.photoFileName("Blu1234567", now, folder.root)

        assertTrue(name, FileNamingUtils.validate(name))
        val parts = FileNamingUtils.parse(name)!!
        assertEquals("BF01", parts.farmCode)
        assertEquals("CALF_REG", parts.project)
        assertEquals("jpg", parts.extension)
    }

    @Test
    fun `without a farm code the photo keeps the older tag based name`() {
        SyncIdentityRegistry.clear()

        assertEquals("calf_Blu1234567_$now.jpg", CalfPhotoCapture.photoFileName("Blu1234567", now, folder.root))
    }

    @Test
    fun `a second photo in the same second does not reuse the name`() {
        SyncIdentityRegistry.set("BF01", "MOB_DEV_1")
        val first = CalfPhotoCapture.photoFileName("Blu1234567", now, folder.root)
        File(folder.root, first).writeText("x")

        val second = CalfPhotoCapture.photoFileName("Blu7654321", now, folder.root)

        assertEquals("calf_Blu7654321_$now.jpg", second)
    }
}
