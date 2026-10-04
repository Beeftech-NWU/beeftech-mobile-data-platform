package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoleTest {

    @Test
    fun `each role inherits the ones below it`() {
        assertTrue(Role.ADMIN.atLeast(Role.ADMIN))
        assertTrue(Role.ADMIN.atLeast(Role.MANAGER))
        assertTrue(Role.ADMIN.atLeast(Role.WORKER))
        assertTrue(Role.MANAGER.atLeast(Role.WORKER))
        assertFalse(Role.MANAGER.atLeast(Role.ADMIN))
        assertFalse(Role.WORKER.atLeast(Role.MANAGER))
    }

    @Test
    fun `fromId maps the seeded ids and rejects the rest`() {
        assertEquals(Role.ADMIN, Role.fromId(1))
        assertEquals(Role.MANAGER, Role.fromId(2))
        assertEquals(Role.WORKER, Role.fromId(3))
        assertNull(Role.fromId(4))
        assertNull(Role.fromId(null))
    }

    @Test
    fun `token carries the site claim and old tokens without it still decode`() {
        val jwt = JwtService()

        val withSite = jwt.decode(
            jwt.generateToken("jvdm", userId = "u-1", role = 3, siteId = "site-1")
        )!!
        assertEquals("site-1", withSite.siteId)
        assertEquals(Role.WORKER, withSite.roleEnum)

        val legacy = jwt.decode(
            jwt.generateToken("jvdm", userId = "u-1", role = 3)
        )!!
        assertNull(legacy.siteId)
    }
}
