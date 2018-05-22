package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.test.withTables
import ca.allanwang.mcgill.db.testUser
import ca.allanwang.mcgill.models.data.Session
import org.junit.Test
import kotlin.test.assertEquals

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

}