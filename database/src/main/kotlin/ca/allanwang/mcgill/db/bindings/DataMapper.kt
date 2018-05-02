package ca.allanwang.mcgill.db.bindings

import ca.allanwang.mcgill.db.statements.batchInsertOnDuplicateKeyUpdate
import ca.allanwang.mcgill.db.statements.batchInsertOrIgnore
import ca.allanwang.mcgill.db.statements.insertOrUpdate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder


/**
 * Table that can receive and save data
 */
interface DataReceiver<in T : Any> {
    /**
     * Given data, assign variables to table columns
     */
    fun toTable(u: UpdateBuilder<*>, d: T)

    /**
     * List of columns to check against during updates
     */
    val uniqueUpdateColumns: List<Column<*>>

    /**
     * Given data, return db query statement to find that data if it exists
     * Mapping expression should be one to one
     */
    fun SqlExpressionBuilder.mapper(data: T): Op<Boolean>
}

interface OneToManyReceiver<in T : Any, V : Any> {

    fun toTable(u: UpdateBuilder<*>, one: T, many: V)

    fun getMany(one: T): List<V>

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
 * Save data, overwriting if there is a conflict in the provided columns
 */
fun <T : Any, M> M.save(data: T) where M : DataReceiver<T>, M : Table {
    insertOrUpdate(uniqueUpdateColumns) { toTable(it, data) }
}

fun <T : Any, M> M.save(data: List<T>) where M : DataReceiver<T>, M : Table {
    batchInsertOnDuplicateKeyUpdate(data, uniqueUpdateColumns) { toTable(this, it) }
}

fun <T : Any, V : Any, M, C> M.save(data: T, childTable: C)
        where M : OneToManyReceiver<T, V>, M : Table,
              C : DataReceiver<V>, C : Table {
    childTable.save(getMany(data))
    batchInsertOrIgnore(getMany(data)) { toTable(this, data, it) }
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

/**
 * Replicate an existing table column
 * If an index is supplied, the value will be cascaded
 */
fun <C> Table.referenceCol(ref: Column<C>, index: Int = -1): Column<C> =
        registerColumn<C>(ref.name, ref.columnType).run {
            if (index >= 0)
                primaryKey(index).references(ref, ReferenceOption.CASCADE)
            else
                references(ref)
        }
