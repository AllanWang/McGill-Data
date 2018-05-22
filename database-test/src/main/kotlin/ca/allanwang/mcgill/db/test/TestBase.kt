package ca.allanwang.mcgill.db.test

import ca.allanwang.mcgill.db.utils.stdlog
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ThreadLocalRandom

fun sleep(min: Int, max: Int) {
    val duration = ThreadLocalRandom.current().nextInt(max - min) + min
    Thread.sleep(duration.toLong())
}

fun withTables(vararg tables: Table, log: Boolean = false, statement: Transaction.() -> Unit) {
    val connection = Database.connect("jdbc:h2:mem:test;MODE=MySQL", "org.h2.Driver", "test", "test")
    transaction(connection.connector().metaData.defaultTransactionIsolation, repetitionAttempts = 1) {
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

inline fun tryLog(action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        e.printStackTrace()
        System.err.println("TryLog failed with ${e.message}")
    }
}