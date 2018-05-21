package ca.allanwang.mcgill.db.internal

import ca.allanwang.mcgill.db.utils.DbConfigs
import ca.allanwang.mcgill.db.utils.connect
import ca.allanwang.mcgill.db.utils.stdlog
import ca.allanwang.mcgill.db.tables.TestGroups
import ca.allanwang.mcgill.db.tables.TestUserGroups
import ca.allanwang.mcgill.db.tables.TestUsers
import ca.allanwang.mcgill.test.Props
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

object DbSetup {
    fun connect() {
        val configs: DbConfigs = object : DbConfigs {
            override val db: String = Props.testDb
            override val dbUser: String = Props.testDbUser
            override val dbPassword: String = Props.testDbPassword
            override val dbDriver: String = Props.testDriver
        }
        configs.connect()
    }
}

fun <T> testTransaction(vararg tables: Table = arrayOf(TestUsers, TestGroups, TestUserGroups),
                        statement: Transaction.() -> T): T = transaction {
    stdlog()
    drop(*tables)
    create(*tables)
    statement()
}