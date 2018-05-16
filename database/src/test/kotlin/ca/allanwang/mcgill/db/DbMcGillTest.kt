package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.internal.DbSetup
import ca.allanwang.mcgill.db.internal.testTransaction
import ca.allanwang.mcgill.db.tables.Groups
import ca.allanwang.mcgill.db.tables.UserGroups
import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.db.tables.save
import ca.allanwang.mcgill.models.data.User
import org.junit.BeforeClass
import org.junit.Test

class DbMcGillTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun before() = DbSetup.connect()
    }

    fun testUser(id: Int) = User(
            shortUser = "testUser$id",
            id = "testUserId$id",
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

    @Test
    fun test() {
        testTransaction(Users, Groups, UserGroups) {
            val user1 = testUser(1)
            user1.save()
        }
    }
}