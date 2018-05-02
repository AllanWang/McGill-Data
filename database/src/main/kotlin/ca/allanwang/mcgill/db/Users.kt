package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.*
import ca.allanwang.mcgill.models.data.User
import ca.allanwang.mcgill.models.data.UserQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */

fun Table.shortUser() = varchar("short_user", 20)
fun Table.shortUserRef(option:ReferenceOption? = null) =
        shortUser().references(Users.shortUser, option)

object Users : Table(), DataReceiver<User> {
    val shortUser = shortUser().primaryKey()
    val id = varchar("id", 20).uniqueIndex()
    val longUser = varchar("long_user", 30).uniqueIndex()
    val activeSince = long("active_since")
    val displayName = varchar("display_name", 30)
    val email = varchar("email", 30)
    val faculty = varchar("faculty", 40).nullable()
    val givenName = varchar("given_name", 20)
    val lastName = varchar("last_name", 20)
    val middleName = varchar("middle_name", 20).nullable()
//    val lastLdapSync = long("ldap_sync_time")

    override fun toTable(u: UpdateBuilder<*>, d: User) {
        u[activeSince] = d.activeSince
        u[displayName] = d.displayName
        u[email] = d.email
        u[faculty] = d.faculty
        u[givenName] = d.givenName
        u[id] = d.id
        u[lastName] = d.lastName
        u[longUser] = d.longUser
        u[middleName] = d.middleName
        u[shortUser] = d.shortUser
    }

    override val uniqueUpdateColumns: List<Column<*>> = listOf(shortUser)

    //todo move to graphql
    private fun SqlExpressionBuilder.samMatcher(sam: String): Op<Boolean> =
            (Users.longUser eq sam) or (Users.shortUser eq sam) or (Users.id eq sam)

    override fun SqlExpressionBuilder.mapper(data: User): Op<Boolean> =
            id eq data.id
}


/*
 * -----------------------------------------------------
 * Model bindings
 * -----------------------------------------------------
 */
fun User.save() {
    Users.save(Users.shortUser, this)
    UserCourses.save(this, Courses)
    UserGroups.save(this, Groups)
}

fun User.delete() {
    UserCourses.delete(this)
    UserGroups.delete(this)
    Users.delete(this)
}