package ca.allanwang.mcgill.db.utils

import ca.allanwang.mcgill.db.test.TestItemDb
import ca.allanwang.mcgill.db.test.TestItems
import ca.allanwang.mcgill.db.test.withTables
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExposeExtensionTest {

    @Test
    fun newOrUpdate() {
        withTables(TestItems) {
            assertEquals(0, TestItemDb.count())
            val testItem = TestItemDb.newOrUpdate(5) {
                name = "test 5"
            }
            assertEquals(1, TestItemDb.count())
            val testItem2 = TestItemDb.newOrUpdate(5) {
                name = "test 5 v2"
            }
            assertEquals(1, TestItemDb.count())
            assertEquals("test 5 v2", testItem.name)
            testItem.name = "test 5 v3"
            assertEquals(testItem.name, testItem2.name)
        }
    }
}