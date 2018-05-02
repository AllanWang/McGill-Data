package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.Users
import ca.allanwang.mcgill.graphql.kotlin.graphQLArgument
import ca.allanwang.mcgill.graphql.kotlin.graphQLFieldDefinition
import ca.allanwang.mcgill.graphql.kotlin.graphQLObjectType
import com.google.common.base.CaseFormat
import graphql.Scalars
import graphql.language.Field
import graphql.schema.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object UserWiring : TableWiring(Users,
        singleQueryArgs = argDefinitions(Users.shortUser,
                Users.longUser,
                Users.id),
        listQueryArgs = argDefinitions(Users.shortUser,
                Users.longUser,
                Users.id,
                Users.email,
                Users.faculty))

/**
 * Generic wiring for SQL tables
 */
abstract class TableWiring(private val table: Table,
                           name: String = table.tableName,
                           limit: (DataFetchingEnvironment.() -> Int)? = null,
                           where: (DataFetchingEnvironment.() -> Op<Boolean>)? = null,
                           private val singleQueryArgs: List<GraphQLArgument> = emptyList(),
                           private val listQueryArgs: List<GraphQLArgument> = emptyList(),
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
    private val limit: DataFetchingEnvironment.() -> Int = limit ?: { defaultLimit() }
    private val where: DataFetchingEnvironment.() -> Op<Boolean>? = where ?: { defaultWhere() }

    /**
     * Attempt to fetch sql data with the given environments
     */
    fun fetch(env: DataFetchingEnvironment): List<Map<String, Any?>> = env.run {
        transaction {
            select(where(), limit(), dbFields())
        }
    }

    /*
     * ---------------------------------------
     * Environment Helpers
     * ---------------------------------------
     */

    private fun DataFetchingEnvironment.defaultLimit(): Int {
        if (fieldType !is GraphQLList) return 1
        return 20
    }

    private fun DataFetchingEnvironment.defaultWhere(): Op<Boolean>? =
            arguments.entries.fold<MutableMap.MutableEntry<String, Any?>, Op<Boolean>?>(null) { op, (key, query) ->
                val col = columnMap[key] ?: return@fold op
                query ?: return@fold op
                val condition = EqOp(col, QueryParameter(query, col.columnType))
                if (op != null)
                    op and condition
                else
                    condition
            }


    private fun DataFetchingEnvironment.dbFields(): List<String> {
        val selections = fields.firstOrNull { it.name == fieldDefinition.name }
                ?.selectionSet?.selections ?: return emptyList()
        log.info("Selections $selections")
        val (valid, invalid) = selections
                .mapNotNull { (it as? Field)?.name }
                .partition { it in columnMap }
        if (invalid.isNotEmpty())
            log.warn("Invalid fields found within $tableName query: $invalid")
        log.info("Valid $valid")
        return valid
    }

    /**
     * Directly query the [table] with the provided [fields] and [where] condition
     */
    private fun select(where: Op<Boolean>?, limit: Int = 20, fields: Collection<String>): List<Map<String, Any?>> {
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

    fun singleQueryField() = graphQLFieldDefinition {
        name(name)
        argument(singleQueryArgs)
        type(KGraphDb.getObjectType(this@TableWiring))
        dataFetcher { fetch(it).firstOrNull() }
    }

    fun listQueryField() = graphQLFieldDefinition {
        name("${name}s")
        argument(listQueryArgs)
        type(GraphQLList(KGraphDb.getObjectType(this@TableWiring)))
        dataFetcher { fetch(it) }
    }


    fun objectType() = graphQLObjectType {
        name(tableName)
        description("SQL access to $tableName")
        fields(table.columns.map { fieldDefinition(it) })
    }


    companion object {

        fun String.toCamel(): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this)
        fun String.toUnderscore(): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this)

        fun argDefinitions(vararg column: Column<*>) = column.map(this::argDefinition)

        fun argDefinition(column: Column<*>) = graphQLArgument {
            name(column.name.toCamel())
            description("Query for exact match with ${column.name}")
            type(graphQLType(column) as GraphQLInputType)
        }

        fun fieldDefinition(column: Column<*>) = graphQLFieldDefinition {
            name(column.name.toCamel())
            val type = graphQLType(column) as GraphQLOutputType
            type(if (!column.columnType.nullable) GraphQLNonNull(type) else type)
        }

        /**
         * Get the column typing
         * Note that objects are wrapped with a reference.
         * [GraphQLOutputType] are already registered when the wiring is registered
         * todo Make sure that any [GraphQLInputType] is also registered in the process
         */
        private fun graphQLType(column: Column<*>): GraphQLType {
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