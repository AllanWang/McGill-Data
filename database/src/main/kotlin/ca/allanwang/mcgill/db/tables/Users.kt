package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.utils.newOrUpdate
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Group
import ca.allanwang.mcgill.models.data.Semester
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
object Users : IdTable<String>() {
    override val id: Column<EntityID<String>> get() = shortUser
    val shortUser = varchar("short_user", 16).primaryKey().entityId()
    val longUser = varchar("long_user", 64).uniqueIndex()
    val userId = varchar("user_id", 32).uniqueIndex()
    val displayName = varchar("display_name", 64)
    val givenName = varchar("given_name", 64)
    val lastName = varchar("last_name", 64)
    val middleName = varchar("middle_name", 32).nullable()
    val email = varchar("email", 64)
    val faculty = varchar("faculty", 32).nullable()
    val activeSince = long("active_since")

    fun tableDependents() = arrayOf(Users, UserGroups, Groups, UserCourses, Courses)
}

class UserDb(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserDb>(Users) {

        /**
         * Overridden to allow for queries matching any sam column
         */
        override fun findById(id: EntityID<String>): UserDb? {
            val sam = id._value as? String ?: return super.findById(id)
            return testCache(id) ?: with(Users) {
                find { (shortUser eq sam) or (longUser eq sam) or (userId eq sam) }.firstOrNull()
            }
        }

        fun newOrUpdate(user: User): UserDb = transaction {
            newOrUpdate(user.shortUser) {
                longUser = user.longUser
                userId = user.userId
                displayName = user.displayName
                givenName = user.givenName
                lastName = user.lastName
                middleName = user.middleName
                email = user.email
                faculty = user.faculty
                activeSince = user.activeSince
            }.saveGroups(user.groups).saveCourses(user.courses)
        }
    }

    val shortUser: String get() = id.value
    var longUser by Users.longUser
    var userId by Users.userId
    var displayName by Users.displayName
    var givenName by Users.givenName
    var lastName by Users.lastName
    var middleName by Users.middleName
    var email by Users.email
    var faculty by Users.faculty
    var activeSince by Users.activeSince

    fun newSession(expiresIn: Long? = null) = transaction {
        println("New session for ${this@UserDb.id}")
        SessionDb.new {
            user = this@UserDb
            if (expiresIn != null)
                expiration = System.currentTimeMillis() + expiresIn
        }
    }

    fun saveCourses(courses: List<Course>): UserDb = transaction {
        courses.forEach {
            CourseDb.newOrUpdate(it.name) {
                season = it.season
                year = it.year
            }.associate(this@UserDb)
        }
        this@UserDb
    }

    fun courses(semester: Semester) = transaction {
        CourseDb.wrapRows((UserCourses innerJoin Courses).slice(Courses.columns)
                .select { (UserCourses.shortUser eq shortUser) and (Courses.season eq semester.season) and (Courses.year eq semester.year) }
                .orderBy(Courses.year to SortOrder.DESC).orderBy(Courses.season to SortOrder.DESC))
    }

    fun courses(take: Int = -1) = transaction {
        CourseDb.wrapRows((UserCourses innerJoin Courses).slice(Courses.columns).select { UserCourses.shortUser eq shortUser }
                .run { if (take > 0) limit(take) else this }
                .orderBy(Courses.year to SortOrder.DESC).orderBy(Courses.season to SortOrder.DESC))
    }

    fun saveGroups(groups: List<Group>): UserDb = transaction {
        groups.forEach {
            GroupDb.newOrUpdate(it.name) {}.associate(this@UserDb)
        }
        this@UserDb
    }

    fun groups(take: Int = -1) = transaction {
        GroupDb.wrapRows((UserGroups innerJoin Groups).slice(Groups.columns).select { UserGroups.shortUser eq shortUser }
                .run { if (take > 0) limit(take) else this }
                .orderBy(Groups.groupName to SortOrder.ASC))
    }

    fun deleteSessions() = transaction {
        Sessions.deleteAll(shortUser)
    }

    fun toJson(): User = transaction {
        User(shortUser = shortUser,
                userId = userId,
                longUser = longUser,
                displayName = displayName,
                givenName = givenName,
                middleName = middleName,
                lastName = lastName,
                email = email,
                faculty = faculty,
                groups = groups().map(GroupDb::toJson), courses = courses().map(CourseDb::toJson),
                activeSince = activeSince)
    }

}