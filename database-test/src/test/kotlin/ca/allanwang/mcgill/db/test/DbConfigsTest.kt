package ca.allanwang.mcgill.db.test

import org.junit.Test
import kotlin.test.assertEquals

object TestDb : DbTestConfigs("db-utils", TestItems, TestSubItems)

class DbConfigsTest {

    @Test
    fun basic() {
        TestDb.transaction {
            val db = TestItemDb.new {
                name = "test item"
            }
            println(db)
            assertEquals(1, TestItemDb.count(), "Did not save new test item")
        }
        TestDb.delete()
    }

}