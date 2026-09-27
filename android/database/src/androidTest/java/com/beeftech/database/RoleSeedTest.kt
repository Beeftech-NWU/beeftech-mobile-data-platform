package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.User
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoleSeedTest {

    private lateinit var context: Context
    private var database: BeefTechDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun freshDatabase_seedsRoles1Through3() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )
        assertTrue("Database should open successfully.", result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        val roles = database!!.roleDao().getAllRoles()
            .map { it.roleId to it.roleName }

        assertEquals(
            listOf(
                1L to "Administrator",
                2L to "Farm Manager",
                3L to "Worker / User"
            ),
            roles
        )
    }

    @Test
    fun insertUser_withEachSeededRole_doesNotThrowFkConstraint() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )
        assertTrue("Database should open successfully.", result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        val userDao = database!!.userDao()

        listOf(1L, 2L, 3L).forEach { roleId ->
            val user = User(
                username = "user_$roleId",
                role = roleId
            )
            userDao.insertUser(user)

            val fetchedUser = userDao.getUserByUsername("user_$roleId")
            assertEquals(roleId, fetchedUser?.role)
        }
    }

    @Test
    fun reopeningDatabaseWithEmptyRolesTable_reseedsOnOpen() = runBlocking {
        val firstResult = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )
        assertTrue("Database should open successfully.", firstResult is DatabaseResult.Success)
        database = (firstResult as DatabaseResult.Success).database

        database!!.openHelper.writableDatabase.execSQL("DELETE FROM roles")
        assertTrue(database!!.roleDao().getAllRoles().isEmpty())

        database!!.close()
        database = null

        val secondResult = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )
        assertTrue("Database should reopen successfully.", secondResult is DatabaseResult.Success)
        database = (secondResult as DatabaseResult.Success).database

        val roles = database!!.roleDao().getAllRoles()
            .map { it.roleId to it.roleName }

        assertEquals(
            listOf(
                1L to "Administrator",
                2L to "Farm Manager",
                3L to "Worker / User"
            ),
            roles
        )
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
