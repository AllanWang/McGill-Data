package ca.allanwang.db.mcgill

import org.jetbrains.exposed.sql.Table

object Courses : Table() {
    val courseName = varchar("course_name", 10).primaryKey()
    val description = varchar("course_description", 200)
}

object StudentCourses : Table() {
    val id = varchar("id", 20).primaryKey()
    val studentId = integer("student_id") references Students.id
    val courseName = varchar("course_name", 10) references Courses.courseName
    val season = enumerationByName("season", 10, Season::class.java)
    val year = integer("year")
}

enum class Season {
    FALL, WINTER, SUMMER
}
