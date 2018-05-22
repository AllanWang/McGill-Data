package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {

    companion object {

        private val mapper: ObjectMapper by lazy(::jacksonObjectMapper)

        private inline fun <reified T : Any> T.passThroughJackson(): T =
                mapper.treeToValue(mapper.valueToTree(this))

        /**
         * Ensures that the data can be properly serialized
         */
        private inline fun <reified T : McGillModel> T.assertMappable() {
            assertEquals(this, passThroughJackson())
        }
    }

    @Test
    fun user() {
        User(shortUser = "shortUser",
                userId = "userId",
                longUser = "longUser",
                displayName = "displayName",
                givenName = "givenName",
                lastName = "lastName",
                email = "email",
                faculty = "faculty").assertMappable()
    }

    @Test
    fun `full user`() {
        User(shortUser = "shortUser",
                userId = "userId",
                longUser = "longUser",
                displayName = "displayName",
                givenName = "givenName",
                lastName = "lastName",
                email = "email",
                faculty = "faculty",
                courses = listOf(Course("name", "desc", "teacher", Season.FALL, 2018)),
                groups = listOf(Group("group"))).assertMappable()
    }
}