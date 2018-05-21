package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
/**
 * Course detail per semester
 */
object Courses : IdTable<String>() {
    override val id get() = courseName
    val courseName = varchar("course_name", 32).primaryKey().entityId()
    val season = enumeration("season", Season::class.java)
    val year = integer("year")
    val description = varchar("course_description", 256).nullable()
    val teacher = varchar("teacher", 32).nullable()
}

class CourseDb(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, CourseDb>(Courses)

    val courseName: String get() = id.value
    var description by Courses.description
    var teacher by Courses.teacher
    var season by Courses.season
    var year by Courses.year

    fun associate(user: UserDb): CourseDb = apply {
        UserCourses.insertIgnore {
            it[this.shortUser] = user.id
            it[this.courseName] = id
        }
    }

    fun toJson(): Course = transaction {
        Course(courseName, description, teacher, season, year)
    }
}

/**
 * Intermediate table connecting [Users] and [Courses]
 */
object UserCourses : Table() {
    val shortUser = reference("short_user", Users, ReferenceOption.CASCADE).primaryKey(0)
    val courseName = reference("course_name", Courses, ReferenceOption.CASCADE).primaryKey(1)
}
