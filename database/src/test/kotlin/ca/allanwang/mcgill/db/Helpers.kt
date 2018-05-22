package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.models.data.User

fun testUser(id: Int) = User(
        shortUser = "testUser$id",
        userId = "testUserId$id",
        longUser = "test.user$id",
        displayName = "Test User $id",
        givenName = "Test User $id",
        middleName = null,
        lastName = "Test",
        email = "test.user$id@mail.mcgill.ca",
        faculty = "Test",
        groups = emptyList(),
        courses = emptyList(),
        activeSince = System.currentTimeMillis())