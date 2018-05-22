package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.test.withTables
import ca.allanwang.mcgill.db.testUser
import ca.allanwang.mcgill.models.data.Session
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionsTest {

    @Test
    fun `basic creation and json`() {
        withTables(Sessions, *Users.tableDependents()) {
            val session = UserDb.newOrUpdate(testUser(1)).newSession().toJson()
            assertEquals("testUser1", session.shortUser)
            assertEquals(Session.NONE, session.role)
        }
    }

    @Test
    fun invalidation() {
        withTables(Sessions, *Users.tableDependents()) {
            val user1 = UserDb.newOrUpdate(testUser(1))
            val user2 = UserDb.newOrUpdate(testUser(2))
            (1..3).forEach { user1.newSession() }
            (1..4).forEach { user2.newSession() }
            assertEquals(7, SessionDb.count())
            Sessions.deleteAll(user1.shortUser)
            assertEquals(4, SessionDb.count())
        }
    }

    @Test
    fun purge() {
        withTables(Sessions, *Users.tableDependents()) {
            val user1 = UserDb.newOrUpdate(testUser(1))
            user1.newSession(1) // expires in 1 ms
            user1.newSession()
            assertEquals(2, SessionDb.count())
            Sessions.purge()
            assertEquals(1, SessionDb.count(), "Did not purge sessions that have expired")
        }
    }

    @Test
    fun query() {
        withTables(Sessions, *Users.tableDependents()) {
            val user1 = UserDb.newOrUpdate(testUser(1))
            val session = user1.newSession() // expires in 1 ms
            assertNotNull(Sessions[session.id.value, user1.shortUser], "Failed to query for session")
            assertNull(Sessions[session.id.value, user1.shortUser.substring(1)], "Should not be able to query with incorrect shortUser")
            assertNull(Sessions[session.id.value.substring(1), user1.shortUser], "Should not be able to query with incorrect session id")
            session.expiration = 9000
            assertNull(Sessions[session.id.value, user1.shortUser], "Should not be able to query expired session")
        }
    }
}