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
fun Table.shortUserRef() = shortUser() references Users.shortUser

object Users : Table(), DataMapper<User> {
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

    override fun toData(row: ResultRow): User = User(
            activeSince = row[activeSince],
            courses = UserCourses[row[shortUser]],
            displayName = row[displayName],
            email = row[email],
            faculty = row[faculty],
            givenName = row[givenName],
            groups = emptyList() /* TODO update */,
            id = row[id],
            lastName = row[lastName],
            longUser = row[longUser],
            middleName = row[middleName],
            shortUser = row[shortUser])

    override fun toTable(u: UpdateBuilder<Int>, d: User) {
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

    private fun SqlExpressionBuilder.samMatcher(sam: String): Op<Boolean> =
            (Users.longUser eq sam) or (Users.shortUser eq sam) or (Users.id eq sam)

    /**
     * Retrieve user by [User.id], [User.shortUser], or [User.longUser]
     */
    operator fun get(sam: String): User? =
            selectData { samMatcher(sam) }

    fun toUserQuery(row:ResultRow): UserQuery =  UserQuery(shortUser = row[shortUser],
            longUser = row[longUser],
            id = row[id],
            email = row[email],
            displayName = row[displayName])

    fun getUserQuery(sam: String): UserQuery? =
            slice(shortUser, longUser, id, email, displayName)
                    .select { samMatcher(sam) }
                    .limit(1)
                    .mapSingle(Users::toUserQuery)

    override fun SqlExpressionBuilder.mapper(data: User): Op<Boolean> =
            id eq data.id
}


/*
 * -----------------------------------------------------
 * Model bindings
 * -----------------------------------------------------
 */
fun User.save() {
    Users.save(this)
    UserCourses.save(this)
}

fun User.delete() {
    Users.delete(this)
    UserCourses.delete(this)
}