package ca.allanwang.mcgill.db.internal

import ca.allanwang.mcgill.db.bindings.DbConfigs
import ca.allanwang.mcgill.db.bindings.connect
import ca.allanwang.mcgill.db.bindings.stdlog
import ca.allanwang.mcgill.db.tables.TestGroups
import ca.allanwang.mcgill.db.tables.TestUserGroups
import ca.allanwang.mcgill.db.tables.TestUsers
import ca.allanwang.mcgill.test.TestProps
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

object DbSetup {
    fun connect() {
        val configs: DbConfigs = object : DbConfigs {
            override val db: String = TestProps.db
            override val dbUser: String = TestProps.dbUser
            override val dbPassword: String = TestProps.dbPassword
            override val dbDriver: String = TestProps.driver
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