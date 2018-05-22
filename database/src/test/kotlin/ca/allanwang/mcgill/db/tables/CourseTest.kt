package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.test.withTables
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import org.junit.Test
import kotlin.test.assertEquals

class CourseTest {

    @Test
    fun creation() {
        withTables(Courses) {
            val course = CourseDb.new("comp202") {
                season = Season.FALL
                year = 2018
            }

            val expected = Course(name = "comp202",
                    description = null,
                    teacher = null,
                    season = Season.FALL,
                    year = 2018)

            assertEquals(expected, course.toJson(), "Course data mismatch")

            course.description = "test description"

            assertEquals(expected.copy(description = "test description"), course.toJson(), "Course update data mismatch")
        }
    }

}