package ca.allanwang.mcgill.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.test.Props
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals

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

    @Test
    fun test() {

        transaction {

            logger.addLogger(StdOutSqlLogger)

            create(Users)

            val oldCount = Users.selectAll().count()

            val test1 = testUser(1)


            test1.save()

            assertEquals(oldCount + 1, Users.selectAll().count())
            test1.testMatches(1)

            val test2 = test1.copy(middleName = "Hello")
            test2.save()

            assertEquals(oldCount + 1, Users.selectAll().count())
            test2.testMatches(1)
            test2.delete()
            assertEquals(oldCount, Users.selectAll().count())
        }
    }
}