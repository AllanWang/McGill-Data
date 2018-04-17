package ca.allanwang.db.mcgill

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import kotlin.test.assertEquals

class DbTest {

    @Test
    fun test() {

        println("Connecting to ${Props.testDb} with ${Props.testUser}")
        Database.connect(url = Props.testDb,
                user = Props.testUser,
                password = Props.testPassword,
                driver = Props.testDriver)

        transaction {

            logger.addLogger(StdOutSqlLogger)

            create(Students)

            val oldCount = Students.selectAll().count()

            val test = Student(oldCount + 1,
                    "test",
                    System.currentTimeMillis().toString(),
                    "unit",
                    "test")

            test.save()

            assertEquals(oldCount + 1, Students.selectAll().count())
            assertEquals(test, Students[oldCount + 1])

            val test2 = test.copy(firstName = "test2")

            test2.save()

            assertEquals(oldCount + 1, Students.selectAll().count())
            assertEquals(test2, Students[oldCount + 1])

            test2.delete()

            assertEquals(oldCount, Students.selectAll().count())
        }
    }
}