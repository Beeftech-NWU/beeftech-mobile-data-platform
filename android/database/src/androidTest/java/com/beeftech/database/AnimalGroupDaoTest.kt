package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.AnimalGroup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalGroupDaoTest {

    private lateinit var context: Context
    private var database: BeefTechDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun insertAnimalGroup_allowsReadBack() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalGroupDao()

        val group = AnimalGroup(
            animalGroupId = "GROUP-001",
            groupName = "milk cows",
            description = "Animals used for milk"
        )

        dao.insert(group)

        val resultGroup = dao.getById("GROUP-001")

        assertEquals("GROUP-001", resultGroup?.animalGroupId)
        assertEquals("milk cows", resultGroup?.groupName)
        assertEquals("Animals used for milk", resultGroup?.description)
    }

    @Test
    fun getByName_returnsMatchingGroup() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalGroupDao()

        dao.insert(
            AnimalGroup(
                animalGroupId = "GROUP-002",
                groupName = "Calves",
                description = "Young cattle"
            )
        )

        val resultGroup = dao.getByName("Calves")

        assertEquals("GROUP-002", resultGroup?.animalGroupId)
        assertEquals("Young cattle", resultGroup?.description)
    }

    @Test
    fun updateAnimalGroup_changesStoredValues() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalGroupDao()

        val originalGroup = AnimalGroup(
            animalGroupId = "GROUP-003",
            groupName = "Cows",
            description = "Adult female cattle"
        )

        dao.insert(originalGroup)

        val updatedGroup = originalGroup.copy(
            groupName = "milk cows",
            description = "Animals used for milk"
        )

        dao.update(updatedGroup)

        val resultGroup = dao.getById("GROUP-003")

        assertEquals("milk cows", resultGroup?.groupName)
        assertEquals("Animals used for milk", resultGroup?.description)
    }

    @Test
    fun deleteAnimalGroup_returnsOneDeletedRow() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalGroupDao()

        val group = AnimalGroup(
            animalGroupId = "GROUP-004",
            groupName = "Market Animals",
            description = "Animals ready for sale"
        )

        dao.insert(group)

        val deletedRows = dao.delete(group)
        val resultGroup = dao.getById("GROUP-004")

        assertEquals(1, deletedRows)
        assertEquals(null, resultGroup)
    }

    @Test
    fun duplicateGroupName_isRejectedByUniqueIndex() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database = (result as DatabaseResult.Success).database
        val dao = database!!.animalGroupDao()

        dao.insert(
            AnimalGroup(
                animalGroupId = "GROUP-005",
                groupName = "Heifers",
                description = "Young female cattle"
            )
        )

        var threwConstraintViolation = false

        try {
            dao.insert(
                AnimalGroup(
                    animalGroupId = "GROUP-006",
                    groupName = "Heifers",
                    description = "Young female cattle"
                )
            )
        } catch (_: Exception) {
            threwConstraintViolation = true
        }

        assertTrue(
            "A duplicate group name should be rejected.",
            threwConstraintViolation
        )
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
