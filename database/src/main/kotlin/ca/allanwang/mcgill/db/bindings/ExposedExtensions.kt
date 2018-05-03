package ca.allanwang.mcgill.db.bindings

import org.jetbrains.exposed.sql.*


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

/**
 * Replicate an existing table column
 * If an index is supplied, the value will be cascaded
 */
fun <C> Table.referenceCol(ref: Column<C>, index: Int = -1): Column<C> =
        registerColumn<C>("${ref.table.tableName.toUnderscore()}_${ref.name}", ref.columnType).run {
            if (index >= 0)
                primaryKey(index).references(ref, ReferenceOption.CASCADE)
            else
                references(ref)
        }

/**
 * Given expression, convert rows to map
 */
fun Table.getMap(columns: Collection<Column<*>> = this.columns,
                 limit: Int = -1,
                 where: (SqlExpressionBuilder.() -> Op<Boolean>)? = null): List<Map<String, Any?>> =
        (if (where != null) select(where) else selectAll()).apply {
            if (limit > 0) limit(limit)
        }.map { row ->
            columns.map { it.name to row[it] }.toMap()
        }