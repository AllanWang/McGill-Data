package ca.allanwang.mcgill.db

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
        groups = listOf("Unit Test", "$id")
)

fun User.testMatches(id: Int) {
    val dbUser = Users["utest$id"]
    assertEquals(this, dbUser)
}