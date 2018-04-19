package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.DataMapper
import ca.allanwang.mcgill.db.bindings.delete
import ca.allanwang.mcgill.db.bindings.save
import ca.allanwang.mcgill.db.bindings.selectData
import ca.allanwang.mcgill.models.data.User
import ca.allanwang.mcgill.models.data.UserQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

object Users : Table(), DataMapper<User> {
    val shortUser = varchar("short_user", 20).primaryKey()
    val id = varchar("id", 20).uniqueIndex()
    val longUser = varchar("long_user", 30).uniqueIndex()
    //TODO add table for courses
    //TODO add table for groups
    //TODO add table for preferredName
    val activeSince = long("active_since")
    val displayName = varchar("display_name", 30)
    val email = varchar("email", 30)
    val faculty = varchar("faculty", 40).nullable()
    val givenName = varchar("given_name", 20)
    val lastName = varchar("last_name", 20)
    val middleName = varchar("middle_name", 20).nullable()

    override fun Query.toData(): List<User> =
            map {
                User(
                        activeSince = it[activeSince],
                        courses = emptyList() /* TODO update */,
                        displayName = it[displayName],
                        email = it[email],
                        faculty = it[faculty],
                        givenName = it[givenName],
                        groups = emptyList() /* TODO update */,
                        id = it[id],
                        lastName = it[lastName],
                        longUser = it[longUser],
                        middleName = it[middleName],
                        preferredName = emptyList() /* TODO update */,
                        shortUser = it[shortUser]
                )
            }

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

    /**
     * Retrieve user by [User.id], [User.shortUser], or [User.longUser]
     */
    operator fun get(sam: String): User? =
            selectData { (longUser eq sam) or (shortUser eq sam) or (id eq sam) }

    fun getUserQuery(sam: String): UserQuery? =
            slice(shortUser, longUser, id, email, displayName)
                    .select { (longUser eq sam) or (shortUser eq sam) or (id eq sam) }
                    .limit(1)
                    .map {
                        UserQuery(shortUser = it[shortUser],
                                longUser = it[longUser],
                                id = it[id],
                                email = it[email],
                                displayName = it[displayName])
                    }.getOrNull(0)

    fun test() {
        (Users innerJoin Courses).slice(*Users.columns.toTypedArray(), Courses.courseName)
    }

    override fun SqlExpressionBuilder.mapper(data: User): Op<Boolean> =
            id eq data.id
}


/*
 * -----------------------------------------------------
 * Model bindings
 * -----------------------------------------------------
 */
fun User.save() = Users.save(this)

fun User.delete() = Users.delete(this)