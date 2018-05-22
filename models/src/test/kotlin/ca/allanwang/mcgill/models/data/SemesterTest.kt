package ca.allanwang.mcgill.models.data

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class SemesterTest {

    @Test
    fun seasonComparisons() {
        assertTrue(Semester(Season.WINTER, 2018) < Semester(Season.FALL, 2018), "Winter 2018 should be before Fall 2018")
        assertTrue(Semester(Season.WINTER, 2018) < Semester(Season.SUMMER, 2018), "Winter 2018 should be before Summer 2018")
        assertTrue(Semester(Season.SUMMER, 2018) < Semester(Season.FALL, 2018), "Summer 2018 should be before Fall 2018")
        assertTrue(Semester(Season.FALL, 2018) == Semester(Season.FALL, 2018), "Fall 2018 == Fall 2018")
    }

    @Test
    fun mixedComparisons() {
        assertTrue(Semester(Season.FALL, 2018) < Semester(Season.FALL, 2019), "Fall 2018 should be before Fall 2019")
        assertTrue(Semester(Season.FALL, 2018) < Semester(Season.WINTER, 2019), "Fall 2018 should be before Winter 2019")
    }

}