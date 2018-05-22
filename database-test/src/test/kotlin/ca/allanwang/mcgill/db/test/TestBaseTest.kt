package ca.allanwang.mcgill.db.test

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestBaseTest {

    @Test
    fun test() {
        withTables(TestItems) {
            val db = TestItemDb.new {
                name = "test item"
            }
            println(db)
            assertEquals(1, TestItemDb.count(), "Did not save new test item")
        }
    }
}