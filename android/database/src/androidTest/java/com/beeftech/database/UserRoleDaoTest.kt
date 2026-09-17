package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Role
import com.beeftech.database.entity.User
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserRoleDaoTest {

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
    fun insertRoleAndUser_allowsReadBackAndFkConstraint() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )
        assertTrue("Database should open successfully.", result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        val roleDao = database!!.roleDao()
        val userDao = database!!.userDao()

        val roleId = roleDao.insertRole(
            Role(roleName = "ADMIN")
        )

        val user = User(
            username = "john_doe",
            pinHash = "hashed_pin_123",
            role = roleId,
            deviceAssignedId = "dev-001"
        )
        userDao.insertUser(user)

        val fetchedUser = userDao.getUserByUsername("john_doe")
        assertNotNull(fetchedUser)
        assertEquals("john_doe", fetchedUser?.username)
        assertEquals("hashed_pin_123", fetchedUser?.pinHash)
        assertEquals(roleId, fetchedUser?.role)
        assertEquals(0, fetchedUser?.failedPinAttempts)

        val fetchedRole = roleDao.getRoleByName("ADMIN")
        assertNotNull(fetchedRole)
        assertEquals("ADMIN", fetchedRole?.roleName)
    }

    @Test
    fun updatePinHashAndFailedAttempts_updatesCorrectly() = runBlocking {
        val result = DatabaseFactory.create(
            context = context,
            passphrase = createCorrectPassphrase()
        )
        assertTrue("Database should open successfully.", result is DatabaseResult.Success)
        database = (result as DatabaseResult.Success).database

        val userDao = database!!.userDao()

        val user = User(username = "jane_doe", pinHash = "initial_hash")
        userDao.insertUser(user)

        userDao.updateFailedPinAttempts(user.userId, 3)
        val userAfterFailedAttempts = userDao.getUserById(user.userId)
        assertEquals(3, userAfterFailedAttempts?.failedPinAttempts)

        userDao.updatePinHash(user.userId, "new_hash")
        val userAfterPinReset = userDao.getUserById(user.userId)
        assertEquals("new_hash", userAfterPinReset?.pinHash)
        assertEquals(0, userAfterPinReset?.failedPinAttempts)
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index -> (index + 1).toByte() }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}
