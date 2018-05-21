package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel
import java.util.*

/**
 * Model for a group with a semester attached
 * [name] typically follows the format NAME-NUMBER-SECTION; eg COMP-202-001
 */
data class Course(val name: String,
                  val description: String? = null,
                  val teacher: String? = null,
                  val season: Season,
                  val year: Int) : McGillModel {
    fun semester() = Semester(season, year)
}

data class Semester(val season: Season, val year: Int) : McGillModel, Comparable<Semester> {

    override fun compareTo(other: Semester): Int =
            if (year != other.year) year - other.year
            else season.compareTo(other.season)

    companion object {
        /**
         * Get the current semester
         */
        val current: Semester
            get() = with(Calendar.getInstance()) {
                Semester(Season.fromMonth(get(Calendar.MONTH)), get(Calendar.YEAR))
            }
    }
}