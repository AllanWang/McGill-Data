package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.TepidDbDelegate
import ca.allanwang.mcgill.models.bindings.TepidIdDelegate
import ca.allanwang.mcgill.models.bindings.withDbData
import ca.allanwang.mcgill.models.bindings.withIdData
import org.junit.Test
import kotlin.test.assertEquals

class BindingTest {

    @Test
    fun propagationDb() {
        val orig = TepidDbDelegate()
        orig._id = "hello"
        val next = TepidDbDelegate().withDbData(orig)
        assertEquals(orig._id, next._id)
    }

    @Test
    fun propagationId() {
        val orig = TepidDbDelegate()
        orig._id = "hello"
        val next = TepidIdDelegate().withIdData(orig)
        assertEquals(orig._id, next._id)
    }

}