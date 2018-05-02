package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.Users
import ca.allanwang.mcgill.graphql.kotlin.graphQLFieldDefinition
import ca.allanwang.mcgill.graphql.kotlin.graphQLObjectType
import ca.allanwang.mcgill.graphql.kotlin.toCamel
import graphql.Scalars
import graphql.language.Field
import graphql.schema.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object UserWiring : TableWiring(Users,
//        singleQueryArgs = ArgEq.with(Users.shortUser,
//                Users.longUser,
//                Users.id),
//        listQueryArgs = ArgEq.with(Users.shortUser,
//                Users.longUser,
//                Users.id,
//                Users.email,
//                Users.faculty),
        allowSelectAll = true)


/**
 * Generic wiring for SQL tables
 */
abstract class TableWiring(private val table: Table,
                           name: String = table.tableName,
                           private val singleQueryArgs: List<ArgCondition> = emptyList(),
                           private val listQueryArgs: List<ArgCondition> = emptyList(),
                           /**
                            * Enable to allow queries without arguments
                            */
                           private val allowSelectAll: Boolean = singleQueryArgs.isEmpty() || listQueryArgs.isEmpty()
) : WithLogging("TableWiring ${table.tableName}") {

    /**
     * Base name to be used for queries
     * By convention, [Table] names should end with an s
     */
    private val name = name.toLowerCase().trimEnd('s')
    val tableName = table.tableName

    /**
     * Attempt to fetch sql data with the given environments
     */
    fun fetch(env: DataFetchingEnvironment): List<Map<String, Any?>> = env.run {
        transaction {
            val s = select(where(), limit(), dbFields())
            log.info("Selected $s")
            s
        }
    }

    /*
     * ---------------------------------------
     * Environment Helpers
     * ---------------------------------------
     */

    /**
     * Checks if output type is a list (or a single entity)
     */
    private val DataFetchingEnvironment.isList
        get() = fieldType is GraphQLList

    /**
     * Get the limit query value
     * If ![isList], defaults to 1
     */
    private fun DataFetchingEnvironment.limit(): Int =
            if (!isList) 1
            else arguments["limit"] as? Int ?: DEFAULT_QUERY_SIZE

    /**
     * Collects the condition mappings to form a potential op
     */
    private fun DataFetchingEnvironment.where(): Op<Boolean>? =
            (if (isList) listQueryArgs else singleQueryArgs).fold<ArgCondition, Op<Boolean>?>(null) { op, cond ->
                val query = arguments[cond.name] ?: return@fold op
                val condition = cond.handle(query) ?: return@fold op
                if (op != null)
                    op and condition
                else
                    condition
            }


    private fun DataFetchingEnvironment.dbFields(): List<String> {
        val selections = fields.firstOrNull { it.name == fieldDefinition.name }
                ?.selectionSet?.selections ?: return emptyList()
        val (valid, invalid) = selections
                .mapNotNull { (it as? Field)?.name }
                .partition { it in columnMap }
        if (invalid.isNotEmpty())
            log.warn("Invalid fields found within $tableName query: $invalid")
        return valid
    }

    /**
     * Directly query the [table] with the provided [fields] and [where] condition
     */
    private fun select(where: Op<Boolean>?, limit: Int, fields: Collection<String>): List<Map<String, Any?>> {
        if (where == null && !allowSelectAll)
            return emptyList()
        if (fields.isEmpty()) {
            log.warn("Attempting to query without field selection")
            return emptyList()
        }
        val query = if (where != null) table.select(where) else table.selectAll()
        if (limit > 0)
            query.limit(limit)
        return query.map {
            it.toData(fields)
        }
    }

    private fun ResultRow.toData(fields: Collection<String>): Map<String, Any?> =
            fields.map { it to this[columnMap[it]!!] }.toMap()

    /**
     * Maps the graphQL field to the associated column
     */
    private val columnMap: Map<String, Column<*>> = table.columns.map { it.name.toCamel() to it }.toMap()

    fun singleQueryField() = graphQLFieldDefinition(name,
            GraphQLList(KGraphDb.getObjectType(this@TableWiring))) {
        argument(singleQueryArgs.map(ArgCondition::buildArgument))
        dataFetcher { fetch(it).firstOrNull() }
    }

    fun listQueryField() = graphQLFieldDefinition("${name}s",
            GraphQLList(KGraphDb.getObjectType(this@TableWiring))) {
        argument(listQueryArgs.map(ArgCondition::buildArgument))
        argument(defaultListArgs)
        dataFetcher { fetch(it) }
    }


    fun objectType() = graphQLObjectType(tableName, "SQL access to $tableName") {
        fields(table.columns.map { fieldDefinition(it) })
    }


    companion object {

        const val DEFAULT_QUERY_SIZE = 20

        val defaultListArgs: List<GraphQLArgument> = listOf(
//                graphQLIntArgument("limit", "Cap the max list output size")
//                graphQLArgument("orderBy", graphQLInputObjectType("Order") {
//                    field(graphQLInputStringField("field", "Column key to sort by"))
//                    field(graphQLInputObjectField("direction",
//                            graphQLEnumType("SortDirection", null, "ASC", "DESC"),
//                            "Direction to sorty by"))
//                })
        )

        fun fieldDefinition(column: Column<*>) = graphQLFieldDefinition(column.name.toCamel(),
                with(graphQLType(column) as GraphQLOutputType) {
                    if (!column.columnType.nullable) GraphQLNonNull(this)
                    else this
                })

        /**
         * Get the column typing
         * Note that objects are wrapped with a reference.
         * [GraphQLOutputType] are already registered when the wiring is registered
         * todo Make sure that any [GraphQLInputType] is also registered in the process
         */
        fun graphQLType(column: Column<*>): GraphQLType {
            column.referee?.apply {
                return GraphQLTypeReference(this.table.tableName)
            }

            return when (column.columnType) {
                is IntegerColumnType -> Scalars.GraphQLInt
                is LongColumnType -> Scalars.GraphQLLong
                is DecimalColumnType -> Scalars.GraphQLFloat
                is StringColumnType -> Scalars.GraphQLString
                else -> throw RuntimeException("Unknown type ${column.columnType.sqlType()}")
            }
        }
    }

}