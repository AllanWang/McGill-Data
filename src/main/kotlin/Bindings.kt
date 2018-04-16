import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement

/**
 * Parameterless helper methods for data models
 */
interface DataDsl {
    fun save()
    fun delete()
}

/**
 * Unified data db mapping
 */
interface DataMapper<T : Any> {
    /**
     * Given a query, map results to a data list
     */
    fun Query.toData(): List<T>

    /**
     * Given data, assign variables to table columns
     */
    fun toTable(u: UpdateStatement, d: T)

    /**
     * Given data, return db query statement to find that data if it exists
     */
    fun SqlExpressionBuilder.mapper(data: T): Op<Boolean>
}

/*
 * -----------------------------------------------------
 * DataMapper Extensions
 * -----------------------------------------------------
 */

/**
 * Queries for data in db and updates the values
 */
fun <T : Any, M> M.save(data: T): Int where M : DataMapper<T>, M : Table =
        update({ mapper(data) }, 1) {
            toTable(it, data)
        }

/**
 * Queries for data in db and deletes it
 */
fun <T : Any, M> M.delete(data: T): Int where M : DataMapper<T>, M : Table =
        deleteWhere { mapper(data) }

/**
 * Queries for one data model
 */
fun <T : Any, M> M.selectData(where: SqlExpressionBuilder.() -> Op<Boolean>): T?
        where M : DataMapper<T>, M : FieldSet =
        select(where).limit(1, 0).toData().getOrNull(0)