package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.test.withTables
import ca.allanwang.mcgill.models.data.Group
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.junit.Test
import kotlin.test.assertEquals

class UserTest {

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

    @Test
    fun `creation and json`() {
        withTables(*Users.tableDependents()) {
            val testUser5 = testUser(5)
            val userDb = UserDb.newOrUpdate(testUser5)
            assertEquals(testUser5, userDb.toJson())
            userDb.middleName = "middle"
            userDb.faculty = "Faculty of Tests"
            assertEquals(testUser5.copy(middleName = "middle", faculty = "Faculty of Tests"), userDb.toJson(), "Updates did not register")
        }
    }

    @Test
    fun `advanced creation and json`() {
        withTables(*Users.tableDependents()) {
            val testUser9 = testUser(9)
            assertEquals(0, GroupDb.count())
            val userDb = UserDb.newOrUpdate(testUser9.copy(groups = listOf(Group("test group"), Group("test group"))))
            assertEquals(1, GroupDb.count(), "Duplicate groups should be overridden")
            assertEquals(testUser9.copy(groups = listOf(Group("test group"))), userDb.toJson())
            userDb.saveGroups(listOf(Group("a test group"), Group("z test group")))
            assertEquals(testUser9.copy(groups = listOf("a test group", "test group", "z test group").map(::Group)),
                    userDb.toJson(), "Groups should default to alphabetic sort")
            assertEquals(3, UserGroups.selectAll().count())
            GroupDb["z test group"].delete()
            assertEquals(2, UserGroups.selectAll().count(), "User group listings should cascade")
            assertEquals(testUser9.copy(groups = listOf("a test group", "test group").map(::Group)),
                    userDb.toJson(), "z test group should be gone")
            Groups.deleteAll()
            assertEquals(testUser9, userDb.toJson(), "User should no longer have any groups")
        }
    }

    @Test
    fun `retrieval by sam`() {
        withTables(*Users.tableDependents()) {
            val testUser1 = testUser(1).copy(middleName = "middle")
            assertEquals("testUser1", testUser1.shortUser, "testUser() shortUser mismatch")
            assertEquals("test.user1", testUser1.longUser, "testUser() longUser mismatch")
            assertEquals("testUserId1", testUser1.userId, "testUser() userId mismatch")
            UserDb.newOrUpdate(testUser1)
            assertEquals(testUser1, UserDb["testUser1"].toJson(), "Failed to query by shortUser")
            assertEquals(testUser1, UserDb["test.user1"].toJson(), "Failed to query by longUser")
            assertEquals(testUser1, UserDb["testUserId1"].toJson(), "Failed to query by userId")
        }
    }

}