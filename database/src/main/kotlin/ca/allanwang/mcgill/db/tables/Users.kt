package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.bindings.*
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
object Users : Table(), DataReceiver<User> {
    val shortUser = varchar("short_user", 16).primaryKey()
    val longUser = varchar("long_user", 64).uniqueIndex()
    val id = varchar("id", 32).uniqueIndex()
    val displayName = varchar("display_name", 64)
    val givenName = varchar("given_name", 64)
    val lastName = varchar("last_name", 64)
    val middleName = varchar("middle_name", 32).nullable()
    val email = varchar("email", 64)
    val faculty = varchar("faculty", 32).nullable()
    val activeSince = long("active_since")

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
            (longUser eq sam) or (shortUser eq sam) or (id eq sam)

    override fun SqlExpressionBuilder.mapper(data: User): Op<Boolean> =
            shortUser eq data.shortUser
}


/*
 * -----------------------------------------------------
 * Model bindings
 * -----------------------------------------------------
 */
fun User.save() {
    Users.save(this)
    UserCourses.save(this, Courses)
    UserGroups.save(this, Groups)
}

fun User.delete() {
    Users.delete(this)
}