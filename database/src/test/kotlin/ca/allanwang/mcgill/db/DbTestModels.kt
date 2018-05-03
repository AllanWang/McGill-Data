package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.getMap
import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import ca.allanwang.mcgill.models.data.User
import kotlin.test.assertEquals

fun testUser(id: Int) = User(
        shortUser = "utest$id",
        longUser = "unit.test$id",
        displayName = "Unit Test $id",
        email = "unit.test$id@mail.mcgill.ca",
        givenName = "Unit",
        lastName = "Test $id",
        id = "test$id",
        faculty = "Unit Test",
        courses = listOf(Course("comp303", season = Season.FALL, year = 2018)),
        groups = listOf("Unit Test", "$id")
)

fun User.testMatches(id: Int) {
    val dbUser = Users.getMap { Users.shortUser eq "utest$id" }.firstOrNull()
    assertEquals(shortUser, dbUser?.get("short_user"), "User mismatch: $this \n\nDb: $dbUser")
}