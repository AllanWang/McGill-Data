package ca.allanwang.mcgill.db.test

import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import java.util.concurrent.ThreadLocalRandom

fun Transaction.stdlog() = logger.addLogger(StdOutSqlLogger)

fun sleep(min: Int, max: Int) {
    val duration = ThreadLocalRandom.current().nextInt(max - min) + min
    Thread.sleep(duration.toLong())
}