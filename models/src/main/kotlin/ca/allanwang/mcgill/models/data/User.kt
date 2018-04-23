package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Created by Allan Wang on 2017-05-14.
 *
 * The complete collection of user attributes
 * Note that this is for back end only as it has content that should not be queried
 */
data class User(
        var shortUser: String,
        var id: String,
        var longUser: String,
        var displayName: String,
        var givenName: String,
        var middleName: String? = null,
        var lastName: String,
        var email: String,
        var faculty: String? = null,
        var groups: List<String> = emptyList(),
        var courses: List<Course> = emptyList(),
        var activeSince: Long = System.currentTimeMillis()
) : McGillModel {

    /**
     * Check if supplied [name] matches
     * [shortUser] or [longUser]
     */
    fun isMatch(name: String) =
            if (name.contains(".")) longUser == name
            else shortUser == name

    /**
     * Get the set of semesters for which the user has had courses
     */
    @JsonIgnore
    fun getSemesters(): Set<Semester> =
            courses.map(Course::semester).toSet()

    fun toUserQuery(): UserQuery = UserQuery(
            shortUser = shortUser,
            id = id,
            longUser = longUser,
            email = email,
            displayName = displayName)

    companion object {

        private val shortUserRegex: Regex by lazy { Regex("[a-z]+[0-9]*") }

        fun isShortUser(sam: String?):Boolean =
                sam != null && shortUserRegex.matches(sam)
    }
}

/**
 * A simplified version of [User]
 */
data class UserQuery(
        var shortUser: String,
        var id: String,
        var longUser: String,
        var displayName: String,
        var email: String
) : McGillModel
