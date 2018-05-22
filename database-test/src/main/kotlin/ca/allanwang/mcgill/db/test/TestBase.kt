package ca.allanwang.mcgill.db.test

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ThreadLocalRandom

fun Transaction.stdlog() = logger.addLogger(StdOutSqlLogger)

fun sleep(min: Int, max: Int) {
    val duration = ThreadLocalRandom.current().nextInt(max - min) + min
    Thread.sleep(duration.toLong())
}

fun withTables (vararg tables: Table, log: Boolean = false, statement: Transaction.() -> Unit) {
    Database.connect("jdbc:h2:mem:regular", "org.h2.Driver", "test", "test")
    transaction {
        if (log)
            stdlog()
        SchemaUtils.create(*tables)
        try {
            statement()
            commit()
        } finally {
            SchemaUtils.drop(*tables)
            commit()
        }
    }
}