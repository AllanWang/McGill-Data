package ca.allanwang.mcgill.db.bindings

import ca.allanwang.mcgill.db.statements.insertOrUpdate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/**
 * Table that can receive and handle data
 */
interface DataReceiver<in T : Any> {
    /**
     * Given data, assign variables to table columns
     */
    fun toTable(u: UpdateBuilder<*>, d: T)

    /**
     * Given data, return db query statement to find that data if it exists
     * Mapping expression should be one to one
     */
    fun SqlExpressionBuilder.mapper(data: T): Op<Boolean>
}

/**
 * Unified data db mapping
 */
interface DataMapper<T : Any> : DataReceiver<T> {
    /**
     * Given a result row, map to the data
     */
    fun toData(row: ResultRow): T

}

/*
 * -----------------------------------------------------
 * Query Extensions
 * -----------------------------------------------------
 */

/**
 * Allow any query to apply a mapping function
 * No safety checks are made to ensure that the mapping is actually possible
 */
fun <T : Any> Query.mapWith(mapper: (row: ResultRow) -> T): List<T> =
        map { mapper(it) }

/**
 * Check if element exists in iterator,
 * and map the first one only if it exists
 */
fun <T : Any> Query.mapSingle(mapper: (row: ResultRow) -> T): T? =
        iterator().run { if (hasNext()) mapper(next()) else null }

/*
 * -----------------------------------------------------
 * DataReceiver Extensions
 * -----------------------------------------------------
 */

/**
 * Queries for a match in the db, and either updates the existing data or inserts a new one
 * Note that this only affects the table implementing the receiver!
 * This may not fully save all the data in [data]
 *
 * Create in extension function directly from [data] to update the required queries
 * to save to completion.
 */
fun <T : Any, M> M.save(data: T) where M : DataReceiver<T>, M : Table {
    if (select({ mapper(data) }).empty())
        insert { toTable(it, data) }
    else
        update({ mapper(data) }) {
            toTable(it, data)
        }
}

fun <T : Any, M> M.save(key: Column<*>, data: T) where M : DataReceiver<T>, M : Table {
    insertOrUpdate(key) { toTable(it, data) }
}

fun <T : Any, M> M.save(data: Collection<T>) where M : DataReceiver<T>, M : Table {
//    if (data.isNotEmpty())
//        batchInsert(data, true) { toTable(this, it) }
    data.forEach { save(it) } // todo improve
}

fun <T : Any, M> M.save(key: Column<*>, data: Collection<T>) where M : DataReceiver<T>, M : Table {
//    if (data.isNotEmpty())
//        batchInsert(data) { toTable(this, it) }
    data.forEach { save(key, it) } // todo improve
}

/**
 * Queries for data in db and deletes it
 * Note that this only affects the table implementing the receiver!
 * This may not fully delete all the data in [data]
 *
 * Create in extension function directly from [data] to update the required queries
 * to delete to completion.
 */
fun <T : Any, M> M.delete(data: T) where M : DataReceiver<T>, M : Table {
    deleteWhere { mapper(data) }
}

/*
 * -----------------------------------------------------
 * DataMapper Extensions
 * -----------------------------------------------------
 */

/**
 * Queries for one data model
 */
fun <T : Any, M> M.selectData(where: SqlExpressionBuilder.() -> Op<Boolean>): T?
        where M : DataMapper<T>, M : FieldSet =
        select(where).limit(1, 0).mapSingle(this::toData)

fun <T : Any, M> M.selectDataCollection(where: SqlExpressionBuilder.() -> Op<Boolean>): List<T>
        where M : DataMapper<T>, M : FieldSet =
        select(where).map(this::toData)