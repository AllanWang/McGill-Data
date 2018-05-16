package ca.allanwang.mcgill.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.readMap
import ca.allanwang.mcgill.db.internal.DbSetup
import ca.allanwang.mcgill.db.internal.testTransaction
import ca.allanwang.mcgill.db.tables.*
import org.jetbrains.exposed.sql.selectAll
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
        val result = testTransaction {

            (10..20).map(::testUser).forEach(TestUser::save)

            (TestUsers innerJoin TestUserGroups innerJoin TestGroups)
                    .slice(TestUsers.columns + TestGroups.columns)
                    .selectAll()
                    .readMap {
                        list("users") {
                            attrs(TestUsers.columns)
                            list("groups") {
                                attrs(TestGroups.columns)
                            }
                        }
                    }

        }
        println(result)
    }
}