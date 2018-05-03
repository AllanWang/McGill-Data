package ca.allanwang.mcgill.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.getMap
import ca.allanwang.mcgill.db.tables.*
import ca.allanwang.mcgill.test.Props
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DbTest {

    companion object : WithLogging() {

        @BeforeClass
        @JvmStatic
        fun before() {
            log.info("Connecting to ${Props.testDb} with ${Props.testDbUser}")
            Database.connect(url = Props.testDb,
                    user = Props.testDbUser,
                    password = Props.testDbPassword,
                    driver = Props.testDriver)
        }
    }

    fun Table.assertCount(count: Int, message: String? = null) =
            assertEquals(count, selectAll().count(),
                    message ?: "$tableName count mismatch\n\n${getMap()}")

    private fun assertTableCounts(userCount: Int) {
        assertTrue(userCount > 0, "Table count assertion must have at least one user")
        TestUsers.assertCount(userCount)
        TestGroups.assertCount(userCount + 1) // one shared group
        TestUserGroups.assertCount(userCount * 2)
    }

    @Test
    fun test() {

        transaction {

            logger.addLogger(StdOutSqlLogger)

            drop(TestUsers, TestGroups, TestUserGroups)

            create(TestUsers, TestGroups, TestUserGroups)

            TestUsers.assertCount(0,
                    "Dropping tables still resulted in existing test users")

            TestGroups.assertCount(0)
            TestUserGroups.assertCount(0)

            val test1 = testUser(1)

            // first save
            test1.save()

            assertTableCounts(1)
            test1.assertMatch()

            test1.name = test1.name + "$2"

            // updating attribute; table size should not change
            test1.save()
            assertTableCounts(1)
            test1.assertMatch()

            val test2 = testUser(2)

            // no change should occur
            test2.delete()
            assertTableCounts(1)

            // saving new user
            test2.save()
            assertTableCounts(2)
            test2.assertMatch()

            // deleting valid user
            test1.delete()
            TestUsers.assertCount(1)        // only user2 remains
            TestGroups.assertCount(3)       // user1's group still exists
            TestUserGroups.assertCount(2)   // only mappings for user2
        }
    }
}