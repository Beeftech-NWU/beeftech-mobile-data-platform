package com.beeftech.backend.api.auth

enum class Role(val id: Int) {
    ADMIN(1),
    MANAGER(2),
    WORKER(3);

    /* Lower id = more privilege; each role inherits the ones below it. */
    fun atLeast(other: Role): Boolean = id <= other.id

    companion object {
        fun fromId(id: Int?): Role? = entries.firstOrNull { it.id == id }
    }
}
