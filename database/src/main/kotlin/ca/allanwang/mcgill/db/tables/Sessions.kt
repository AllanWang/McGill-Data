package ca.allanwang.mcgill.db.tables

import ca.allanwang.kit.logger.Loggable
import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.models.data.Session
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

object Sessions : IdTable<String>(), Loggable by WithLogging() {
    override val id = varchar("id", 255).primaryKey().clientDefault { BigInteger(130, random).toString(32) }.entityId()
    val shortUser = reference("short_user", Users, ReferenceOption.CASCADE)
    val expiration = long("expiration").clientDefault { System.currentTimeMillis() + defaultExpiresIn }

    val defaultExpiresIn: Long = TimeUnit.DAYS.toMillis(1)

    private val random = SecureRandom()

    private fun SqlExpressionBuilder.matches(id: String, shortUser: String): Op<Boolean> =
            (Sessions.id eq id) and (Sessions.shortUser eq shortUser)

    private fun SqlExpressionBuilder.expired(): Op<Boolean> =
            (Sessions.expiration neq -1L) and (Sessions.expiration lessEq System.currentTimeMillis())

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

    operator fun get(id: String, shortUser: String): Session? = transaction {
        SessionDb[id, shortUser]?.toJson()
    }

    fun deleteAll(shortUser: String): Int = transaction {
        deleteIgnoreWhere { Sessions.shortUser eq shortUser }
    }

    /**
     * Delete expired sessions
     */
    fun purge(): Int = transaction {
        deleteIgnoreWhere { expired() }
    }

}

class SessionDb(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, SessionDb>(Sessions) {

        operator fun get(id: String, shortUser: String): SessionDb? = transaction {
            findById(id)?.takeIf { it.shortUser.value == shortUser }
        }

    }

    var shortUser by Sessions.shortUser
    var user by UserDb referencedOn Sessions.shortUser
    var expiration by Sessions.expiration
    val role: String
        get() = transaction {
            Sessions.role(user.groups().map(GroupDb::groupName).toSet())
        }


    fun toJson(): Session = transaction {
        Session(id.value, shortUser.value, role)
    }
}

