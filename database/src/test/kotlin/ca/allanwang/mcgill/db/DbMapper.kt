package ca.allanwang.mcgill.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.readMap
import ca.allanwang.mcgill.db.bindings.stdlog
import ca.allanwang.mcgill.db.internal.DbSetup
import ca.allanwang.mcgill.db.tables.TestGroups
import ca.allanwang.mcgill.db.tables.TestUserGroups
import ca.allanwang.mcgill.db.tables.TestUsers
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.junit.Test

class DbMapper {
    companion object : WithLogging() {
        @BeforeClass
        @JvmStatic
        fun before() = DbSetup.connect()
    }

    @Test
    fun readMap() {
        val result = transaction {
            stdlog()
            (TestUsers innerJoin TestUserGroups innerJoin TestGroups)
                    .slice(TestUsers.columns + TestGroups.columns)
                    .selectAll()
                    .readMap {
                        attrs(TestUsers.columns)
                        list("groups") {
                            attrs(TestGroups.columns)
                        }
                    }

        }
        println(result)
    }
}