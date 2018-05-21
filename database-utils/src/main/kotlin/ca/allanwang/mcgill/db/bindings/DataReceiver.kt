package ca.allanwang.mcgill.db.bindings

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

    /**
     * Given data, assign variables to table columns
     */
    fun toTable(u: UpdateBuilder<*>, one: T, many: V)

    /**
     * Given main data, return child associates
     */
    fun getMany(one: T): List<V>

}

/**
 * Allows any data model to map its content to table columns
 */
interface ColMapper {
    fun colMap(): Map<Column<*>, Any?>
}

fun ColMapper.matches(map: Map<String, Any?>): Boolean {
    val colMap = colMap()
    if (colMap.size != map.size)
        return false
    return colMap.all { (k, v) -> v == map[k.name] }
}

/*
 * -----------------------------------------------------
 * DataReceiver Extensions
 * -----------------------------------------------------
 */

/**
 * Save data, overwriting if there is a conflict in the provided columns
 */
fun <T : Any, M> M.save(data: T) where M : DataReceiver<T>, M : Table {
    replace { toTable(it, data) }
//    insertOrUpdate(uniqueUpdateColumns) { toTable(it, data) }
}

//fun <T : Any, M> M.save(c: List<Column<*>>, data: T) where M : DataReceiver<T>, M : Table {
//    insertOrUpdate(c) { toTable(it, data) }
////    insertOrUpdate(uniqueUpdateColumns) { toTable(it, data) }
//}

fun <T : Any, M> M.save(data: List<T>) where M : DataReceiver<T>, M : Table {
    data.forEach { save(it) }
//    batchInsertOnDuplicateKeyUpdate(data, uniqueUpdateColumns) { toTable(this, it) }
}

/**
 * To save a one to many data set,
 * the child items are first created, followed by the one to many rows
 * How the children are saved are at the discretion of their table,
 * but by default, updated content will overwrite the data
 */
fun <T : Any, V : Any, M, C> M.save(data: T, childTable: C)
        where M : OneToManyReceiver<T, V>, M : Table,
              C : DataReceiver<V>, C : Table {
    val children = getMany(data)
    if (children.isEmpty()) return
    childTable.save(children)
    batchInsert(children, true) { toTable(this, data, it) }
//    batchInsertOrIgnore(getMany(data)) { toTable(this, data, it) }
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

