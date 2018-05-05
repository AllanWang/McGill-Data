package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.bindings.DataReceiver
import ca.allanwang.mcgill.db.bindings.OneToManyReceiver
import ca.allanwang.mcgill.db.bindings.delete
import ca.allanwang.mcgill.db.bindings.referenceCol
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
/**
 * Course detail per semester
 */
object Courses : Table(), DataReceiver<Course> {
    val courseName = varchar("course_name", 30).primaryKey()
    val description = varchar("course_description", 200).nullable()
    val teacher = varchar("teacher", 50).nullable()
    val season = enumerationByName("season", 10, Season::class.java)
    val year = integer("year")

    override fun toTable(u: UpdateBuilder<*>, d: Course) {
        u[courseName] = d.courseName
        u[description] = d.description
        u[teacher] = d.teacher
        u[season] = d.season
        u[year] = d.year
    }

    override fun SqlExpressionBuilder.mapper(data: Course): Op<Boolean> =
            (courseName eq data.courseName) and (year eq data.year)

    override val uniqueUpdateColumns: List<Column<*>> = listOf(courseName)
}

fun Course.delete() {
    UserCourses.deleteWhere { UserCourses.courseName eq courseName }
    Courses.delete(this)
}

/**
 * Intermediate table connecting [Users] and [Courses]
 */
object UserCourses : Table(), OneToManyReceiver<User, Course> {
    val shortUser = referenceCol(Users.shortUser, 0)
    val courseName = referenceCol(Courses.courseName, 1)

    override fun toTable(u: UpdateBuilder<*>, one: User, many: Course) {
        u[shortUser] = one.shortUser
        u[courseName] = many.courseName
    }

    override fun getMany(one: User): List<Course> = one.courses
}
