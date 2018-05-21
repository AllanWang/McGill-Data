package ca.allanwang.mcgill.db.test

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.utils.DbConfigs
import ca.allanwang.mcgill.db.utils.stdlog
import org.h2.jdbcx.JdbcDataSource
import org.h2.tools.DeleteDbFiles
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import kotlin.test.fail

open class DbTestConfigs(project: String, vararg val tables: Table) : DbConfigs, WithLogging() {

    private val projectName = "test-$project"
    override val db: String = "jdbc:h2:~/$projectName"
    override val dbUser: String = "test"
    override val dbPassword: String = "test"
    override val dbDriver: String = "org.h2.Driver"

    init {
        connectImpl()
    }

    protected fun <T> exposedTransaction(statement: Transaction.() -> T) =
            org.jetbrains.exposed.sql.transactions.transaction(statement = statement)

    protected fun connectImpl() {
        JdbcDataSource().apply {
            setURL(db)
            user = dbUser
            password = dbPassword
        }
        super.connect()
        try {
            exposedTransaction {
                drop(*tables)
                create(*tables)
            }
            log.info("Connected to $projectName")
        } catch (e: Exception) {
            fail("Could not connect to db $db: ${e.message}")
        }
    }

    fun resetTables() = exposedTransaction {
        tables.forEach { it.deleteAll() }
    }

    fun <T> transaction(log: Boolean = true,
                        resetTables: Boolean = true,
                        statement: Transaction.() -> T): T = try {
        exposedTransaction {
            if (resetTables)
                resetTables()
            if (log)
                stdlog()
            try {
                statement()
            } catch (e: Exception) {
                fail("Test statement failed: ${e}")
            }
        }
    } catch (e: Exception) {
        fail("Test transaction failed: ${e.message}")
    }

    fun delete() {
        DeleteDbFiles.execute("~", projectName, false)
    }

}