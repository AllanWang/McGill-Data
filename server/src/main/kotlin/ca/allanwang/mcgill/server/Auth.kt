package ca.allanwang.mcgill.server

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.tables.GroupDb
import ca.allanwang.mcgill.db.tables.SessionDb
import ca.allanwang.mcgill.db.tables.Sessions
import ca.allanwang.mcgill.db.tables.UserDb
import ca.allanwang.mcgill.ldap.McGillLdap
import ca.allanwang.mcgill.ldap.Sam
import ca.allanwang.mcgill.models.data.Group
import ca.allanwang.mcgill.models.data.Session
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.transactions.transaction

object Auth : WithLogging() {

    /**
     * Authenticate a user given some [sam] and a [password]
     * If the sam is not a shortUser, an attempt will be made to find it through our own db
     * This call guarantees one pass through [McGillLdap], to ensure that the user is authenticated
     *
     * Note that if ldap is disabled, we will allow sam=unit, password=test as a valid user
     */
    fun authenticate(sam: String, password: String, expiresIn: Long? = null): Session? {
        if (!McGillGraphQL.ldapEnabled.get()) {
            if (sam != "unit" || password != "test") {
                log.warn("Ldap disabled")
                return null
            }
            // allow for basic test user
            return createTestUser()
        }
        if (McGillLdap.samType(sam) == Sam.SHORT_USER)
            return authenticateImpl(sam, password, expiresIn)
        return transaction {
            // todo verify sam check
            val shortUser = UserDb.findById(sam)?.shortUser ?: return@transaction null
            authenticateImpl(shortUser, password, expiresIn)
        }
    }

    /**
     * Authenticator that guarantees the username is a shortUser
     */
    private fun authenticateImpl(shortUser: String, password: String, expiresIn: Long? = null): Session? {
        val user = McGillLdap.queryUser(shortUser, shortUser to password) ?: return null
        return transaction {
            UserDb.newOrUpdate(user).newSession(expiresIn).toJson()
        }
    }

    private const val testShortUser = "unit123"

    fun createTestUser(): Session = transaction {
        val testUser = User(
                shortUser = testShortUser,
                userId = "unittest",
                longUser = "unit.test",
                displayName = "Unit Test",
                givenName = "Unit",
                lastName = "Test",
                email = "unit.test@mail.mcgill.ca",
                faculty = "Unit Test",
                groups = listOf(Group("Unit Test")))

        UserDb.newOrUpdate(testUser).newSession().toJson()
    }

    fun deleteTestUser() {
        transaction {
            UserDb.findById(testShortUser)?.delete()
            GroupDb.findById("Unit Test")?.delete()
        }
    }


    /**
     * Get the [Session] matching the given [id] and [shortUser] if it exists
     */
    fun validate(id: String, shortUser: String): Session? = Sessions[id, shortUser]

    fun delete(id: String, shortUser: String): Boolean = transaction { SessionDb[id, shortUser]?.delete() != null }

}