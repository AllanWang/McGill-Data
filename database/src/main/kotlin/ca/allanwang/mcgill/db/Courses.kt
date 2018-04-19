package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.*
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import ca.allanwang.mcgill.models.data.Semester
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */

fun Table.courseName() = varchar("course_name", 10)
fun Table.courseNameRef() = courseName() references Courses.courseName

/**
 * Course detail per semester
 */
object Courses : Table(), DataMapper<Course> {
    val courseName = courseName().primaryKey()
    val description = varchar("course_description", 200).nullable()
    val teacher = varchar("teacher", 20).nullable()
    val season = enumerationByName("season", 10, Season::class.java)
    val year = integer("year")

    override fun toData(row: ResultRow): Course = Course(
            courseName = row[courseName],
            description = row[description],
            teacher = row[teacher],
            season = row[season],
            year = row[year])


    override fun toTable(u: UpdateBuilder<Int>, d: Course) {
        u[courseName] = d.courseName
        u[description] = d.description
        u[teacher] = d.teacher
        u[season] = d.season
        u[year] = d.year
    }

    override fun SqlExpressionBuilder.mapper(data: Course): Op<Boolean> =
            (courseName eq data.courseName) and (year eq data.year)

    /**
     * Retrieve list of courses by course name
     */
    operator fun get(courseName: String): List<Course> =
            selectDataCollection { Courses.courseName eq courseName }

    /**
     * Retrieve list of courses by semester
     */
    operator fun get(semester: Semester): List<Course> =
            selectDataCollection {
                (Courses.season eq semester.season) and (Courses.year eq semester.year)
            }

}

fun Course.delete() = Courses.delete(this)

/**
 * Intermediate table connecting [Users] and [Courses]
 */
object UserCourses : IntIdTable() {
    val shortUser = shortUserRef()
    val courseName = courseNameRef()

    operator fun get(sam: String): List<Course> {
        val shortUser = if (User.isShortUser(sam))
            sam else Users[sam]?.shortUser ?: return emptyList()
        return (Courses innerJoin UserCourses).select { UserCourses.shortUser eq shortUser }
                .mapWith(Courses::toData)
    }

    fun save(user: User) {
        Courses.save(user.courses)
        batchInsert(user.courses) {
            this[shortUser] = user.shortUser
            this[courseName] = it.courseName
        }
    }

    fun delete(user: User) {
        deleteWhere { UserCourses.shortUser eq user.shortUser }
    }
}
