package ca.allanwang.mcgill.db.tables

import ca.allanwang.kit.logger.Loggable
import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.referenceCol
import ca.allanwang.mcgill.models.data.Session
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

object Sessions : Table(), Loggable by WithLogging() {
    val id = varchar("id", 255).primaryKey()
    val shortUser = referenceCol(Users.shortUser)
    val expiration = long("expiration")

    private val random = SecureRandom()

    private fun SqlExpressionBuilder.matches(id: String, shortUser: String): Op<Boolean> =
            (Sessions.id eq id) and (Sessions.shortUser eq shortUser)

    private fun SqlExpressionBuilder.expired(): Op<Boolean> =
            (Sessions.expiration neq -1L) and (Sessions.expiration lessEq System.currentTimeMillis())

    /**
     * Get the [Session] matching the given [id] and [shortUser] if it exists
     */
    operator fun get(id: String, shortUser: String): Session? = transaction {
        if (select { matches(id, shortUser) and not(expired()) }.count() == 0) return@transaction null // no record found
        val groups = UserGroups.select { UserGroups.shortUser eq shortUser }.map { it[UserGroups.groupName] }
        log.trace("Groups for $shortUser: $groups")
        val role = role(groups.toSet())
        return@transaction Session(id, shortUser, role)
    }

    /**
     * Create a new session for the provided user
     */
    fun create(user: User, expiresIn: Long): Session {
        val session = Session(id = BigInteger(130, random).toString(32),
                shortUser = user.shortUser,
                role = role(user.groups.toSet()))
        transaction {
            insert {
                it[id] = session.id
                it[shortUser] = user.shortUser
                it[expiration] = if (expiresIn <= 0L) -1L else System.currentTimeMillis() + expiresIn
            }
        }
        return session
    }

    private val elderGroups = arrayOf("520-Infopoint Admins")
    private val ctferGroups = arrayOf("520-CTF Members", "520-CTF Probationary Members")
    private val userGroups: Array<String>
        get() {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = if (cal.get(Calendar.MONTH) < 8) "W" else "F"
            return arrayOf("000-21st Century Fund",
                    "520-Exchange-$year$month")
        }


    /**
     * Get a user's role based on the groups they are in
     */
    fun role(groups: Set<String>): String = when {
        elderGroups.any(groups::contains) -> Session.ELDER
        ctferGroups.any(groups::contains) -> Session.CTFER
        userGroups.any(groups::contains) -> Session.USER
        else -> Session.NONE
    }

    /**
     * Deletes the session with the matching [id] and [shortUser]
     * Returns true if an item was deleted, and false otherwise
     */
    fun delete(id: String, shortUser: String): Boolean = transaction {
        val result = deleteIgnoreWhere { matches(id, shortUser) }
        result != 0
    }

    /**
     * Delete expired sessions
     */
    fun sweep(): Int = transaction {
        deleteIgnoreWhere { expired() }
    }

}

