package ca.allanwang.mcgill.graphql

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.mapSingle
import ca.allanwang.mcgill.db.tables.Sessions
import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.ldap.McGillLdap
import ca.allanwang.mcgill.ldap.Sam
import ca.allanwang.mcgill.models.data.Session
import org.jetbrains.exposed.sql.select

object Auth : WithLogging() {

    const val defaultExpiresIn: Long = 30L * 24 * 60 * 60 * 1000 // 30 days

    /**
     * Authenticate a user given some [sam] and a [password]
     * If the sam is not a shortUser, an attempt will be made to find it through our own db
     * This call guarantees one pass through [McGillLdap], to ensure that the user is authenticated
     */
    fun authenticate(sam: String, password: String, expiresIn: Long = defaultExpiresIn): Session? {
        if (!Props.ldapEnabled) {
            log.warn("Ldap disabled")
            return null
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


    /**
     * Get the [Session] matching the given [id] and [shortUser] if it exists
     */
    fun validate(id: String, shortUser: String): Session? = Sessions[id, shortUser]

    fun delete(id: String, shortUser: String): Boolean = Sessions.delete(id, shortUser)

}