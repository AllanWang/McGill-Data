package ca.allanwang.mcgill.db.utils

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