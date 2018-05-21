package ca.allanwang.mcgill.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.utils.getMap
import ca.allanwang.mcgill.db.internal.DbSetup
import ca.allanwang.mcgill.db.internal.testTransaction
import ca.allanwang.mcgill.db.tables.*
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals

class DbTest {

    companion object : WithLogging() {
        @BeforeClass
        @JvmStatic
        fun before() = DbSetup.connect()
    }

    private fun Table.assertCount(count: Int, message: String? = null) =
            assertEquals(count, selectAll().count(),
                    message ?: "$tableName count mismatch\n\n${getMap(limit = 20)}")

    /**
     * Assert that all related tables have the appropriate counts
     */
    private fun assertUserCount(userCount: Int) {
        TestUsers.assertCount(userCount)
        TestGroups.assertCount(if (userCount == 0) 0 else userCount + 1) // one shared group
        TestUserGroups.assertCount(userCount * 2)
    }


    @Test
    fun batchInsert() {
        testTransaction {
            testUser(19).save()
        }
    }

    @Test
    fun test() {

        testTransaction {

            assertUserCount(0)

            val test1 = testUser(1)

            // first save
            test1.save()

            assertUserCount(1)
            test1.assertMatch()

            test1.name = test1.name + "$2"

            // updating attribute; table size should not change
            test1.save()
            assertUserCount(1)
            test1.assertMatch()

            val test2 = testUser(2)

            // no change should occur
            test2.delete()
            assertUserCount(1)

            // saving new user
            test2.save()
            assertUserCount(2)
            test2.assertMatch()

            // deleting valid user
            test1.delete()
            TestUsers.assertCount(1)        // only user2 remains
            TestGroups.assertCount(3)       // user1's group still exists
            TestUserGroups.assertCount(2)   // only mappings for user2
        }
    }
}