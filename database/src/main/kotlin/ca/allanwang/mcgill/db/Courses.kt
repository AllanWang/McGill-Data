package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.DataMapper
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/**
 * Course detail per semester
 */
object Courses : Table(), DataMapper<Course> {
    val courseName = varchar("course_name", 10).primaryKey()
    val description = varchar("course_description", 200).nullable()
    val teacher = varchar("teacher", 20).nullable()
    val season = UserCourses.enumerationByName("season", 10, Season::class.java)
    val year = UserCourses.integer("year")

    override fun Query.toData(): List<Course> =
            map {
                Course(
                        courseName = it[courseName],
                        description = it[description],
                        teacher = it[teacher],
                        season = it[season],
                        year = it[year]
                )
            }

    override fun toTable(u: UpdateBuilder<Int>, d: Course) {
        u[courseName] = d.courseName
        u[description] = d.description
        u[teacher] = d.teacher
        u[season] = d.season
        u[year] = d.year
    }

    override fun SqlExpressionBuilder.mapper(data: Course): Op<Boolean> =
            courseName eq data.courseName

    operator fun get(sam: String): List<Course> {
        val user = Users[sam] ?: return emptyList()
        return emptyList()
//        return UserCourses.select {UserCourses.shortUser eq user.shortUser}.map {
//
//        }
    }
}

/**
 * Intermediate table connecting [Users] and [Courses]
 */
object UserCourses : Table() {
    val id = varchar("id", 20).primaryKey().autoIncrement()
    val shortUser = varchar("short_user", 20) references Users.shortUser
    val courseName = varchar("course_name", 10) references Courses.courseName
}
