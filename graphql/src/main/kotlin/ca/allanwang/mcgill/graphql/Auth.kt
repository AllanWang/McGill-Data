package ca.allanwang.mcgill.graphql

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.mapSingle
import ca.allanwang.mcgill.db.tables.Groups
import ca.allanwang.mcgill.db.tables.Sessions
import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.ldap.McGillLdap
import ca.allanwang.mcgill.ldap.Sam
import ca.allanwang.mcgill.models.data.Session
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object Auth : WithLogging() {

    const val defaultExpiresIn: Long = 30L * 24 * 60 * 60 * 1000 // 30 days

    /**
     * Authenticate a user given some [sam] and a [password]
     * If the sam is not a shortUser, an attempt will be made to find it through our own db
     * This call guarantees one pass through [McGillLdap], to ensure that the user is authenticated
     *
     * Note that if ldap is disabled, we will allow sam=unit, password=test as a valid user
     */
    fun authenticate(sam: String, password: String, expiresIn: Long = defaultExpiresIn): Session? {
        if (!Props.ldapEnabled) {
            if (sam != "unit" || password != "test") {
                log.warn("Ldap disabled")
                return null
            }
            // allow for basic test user
            return createTestUser()
        }
        if (McGillLdap.samType(sam) == Sam.SHORT_USER)
            return authenticateImpl(sam, password, expiresIn)
        val shortUser = Users.slice(Users.shortUser).select { Users.run { samMatcher(sam) } }
                .mapSingle { it[Users.shortUser] } ?: return null
        return authenticateImpl(shortUser, password, expiresIn)
    }

    /**
     * Authenticator that guarantees the username is a shortUser
     */
    private fun authenticateImpl(shortUser: String, password: String, expiresIn: Long = defaultExpiresIn): Session? {
        val user = McGillLdap.queryUser(shortUser, shortUser to password) ?: return null
        return Sessions.create(user, expiresIn)
    }

    private const val testShortUser = "unit123"

    fun createTestUser(): Session {
        val testUser = User(
                shortUser = testShortUser,
                id = "unittest",
                longUser = "unit.test",
                displayName = "Unit Test",
                givenName = "Unit",
                lastName = "Test",
                email = "unit.test@mail.mcgill.ca",
                faculty = "Unit Test",
                groups = listOf("Unit Test"))
        return Sessions.create(testUser, -1)
    }

    fun deleteTestUser() {
        Sessions.deleteAll(testShortUser)
        transaction {
            Users.deleteWhere { Users.shortUser eq testShortUser }
            Groups.deleteWhere { Groups.groupName eq "Unit Test" }
        }
    }


    /**
     * Get the [Session] matching the given [id] and [shortUser] if it exists
     */
    fun validate(id: String, shortUser: String): Session? = Sessions[id, shortUser]

    fun delete(id: String, shortUser: String): Boolean = Sessions.delete(id, shortUser)

}