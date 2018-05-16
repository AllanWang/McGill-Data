package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {

    companion object {

        private val mapper: ObjectMapper by lazy { jacksonObjectMapper() }

        private inline fun <reified T : Any> T.passThroughJackson(): T =
                mapper.treeToValue(mapper.valueToTree(this))

        /**
         * Simplified sanity test where there exists a constructor
         * without any mandatory parameters
         */
        private inline fun <reified T : McGillModel> sanityTest() =
                sanityTest { T::class.java.newInstance() }

        /**
         * A sanity test guarantees that _id and _rev do not exist until created
         * And that they have the right keys
         */
        private inline fun <T : McGillModel> sanityTest(supplier: () -> T) {
            val blank = supplier()
            val msg = "found in ${blank::class.java}:"
            val blankJson = mapper.valueToTree<JsonNode>(blank)
            val full = supplier()
            val id = blank.hashCode().toString()
            val rev = full.hashCode().toString()
            val fullJson = mapper.valueToTree<JsonNode>(full)
            val fullDup = mapper.treeToValue(fullJson, full::class.java)
            assertEquals(full, fullDup)
        }

        /**
         * Validates the json conversion to and from the given model
         */
        private inline fun <reified T : Any> checkModel(model: T = T::class.java.newInstance(),
                                                        action: T.() -> Unit) {
            model.action()
            val json = mapper.valueToTree<JsonNode>(model)
            val newModel = mapper.treeToValue<T>(json)
            assertEquals(model, newModel, "Model mismatch for ${T::class.java}")
        }
    }

//
//    @Test
//    fun session() {
//        sanityTest { Session(user = User()) }
//    }
//
//    @Test
//    fun publicSession() {
//        val session = Session(user = User())
//        session._id = "hello"
//        val publicSession = session.toSession().passThroughJackson()
//        assertEquals(session._id, publicSession._id)
//    }
}