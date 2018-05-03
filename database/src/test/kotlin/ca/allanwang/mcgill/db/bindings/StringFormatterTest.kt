package ca.allanwang.mcgill.db.bindings

import org.junit.Test
import kotlin.test.assertEquals

class StringFormatterTest {

    @Test
    fun toCamel() {
        assertEquals("thisIsATest", "this_IS_a_TeSt".toCamel())
    }

    @Test
    fun toUnderscore() {
        assertEquals("this_is_a_test", "thisIsATest".toUnderscore())
    }
}