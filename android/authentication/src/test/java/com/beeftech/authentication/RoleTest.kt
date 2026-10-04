package com.beeftech.authentication

import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.authentication.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleTest {

    @Test
    fun `each role inherits the ones below it`() {
        assertTrue(Role.ADMIN.atLeast(Role.MANAGER))
        assertTrue(Role.ADMIN.atLeast(Role.WORKER))
        assertTrue(Role.MANAGER.atLeast(Role.WORKER))
        assertTrue(Role.WORKER.atLeast(Role.WORKER))
        assertFalse(Role.MANAGER.atLeast(Role.ADMIN))
        assertFalse(Role.WORKER.atLeast(Role.MANAGER))
    }

    @Test
    fun `fromId keeps the seeded ids and rejects the rest`() {
        assertEquals(Role.ADMIN, Role.fromId(1))
        assertEquals(Role.MANAGER, Role.fromId(2))
        assertEquals(Role.WORKER, Role.fromId(3))
        assertNull(Role.fromId(9))
        assertNull(Role.fromId(null))
    }

    @Test
    fun `logged in user exposes its role enum`() {
        val user = LoggedInUser(userId = "u", username = "n", role = 2, deviceId = "d")

        assertEquals(Role.MANAGER, user.roleEnum)
        assertNull(user.siteId)
    }
}
