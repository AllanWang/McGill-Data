package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.graphql.kotlin.graphQLFieldDefinition
import ca.allanwang.mcgill.graphql.kotlin.graphQLObjectType
import graphql.Scalars
import graphql.language.EnumTypeDefinition
import graphql.language.Field
import graphql.schema.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


/**
 * Generic wiring for SQL tables
 */
abstract class TableWiring<T : Table>(val table: T) : WithLogging("TableWiring ${table.tableName}") {

    inline val tableName
        get() = table.tableName
    /**
     * Base name to be used for single result queries
     * By convention, [Table] names should end with an s
     * Set to null to disable single result queries
     */
    protected open val singleFieldName: String? = tableName.toLowerCase().trimEnd('s')
    /**
     * Base name to be used for list result queries
     * Set to null to disable
     */
    protected open val listFieldName: String? = tableName.toLowerCase()

    open fun T.includedColumns(): List<Column<*>> = emptyList()
    open fun T.excludedColumns(): List<Column<*>> = emptyList()
    open fun T.singleQueryArgs(): List<GraphDbArg> = emptyList()
    open fun T.listQueryArgs(): List<GraphDbArg> = listOf(limit)

    /**
     * Maps the graphQL field to the associated column
     */
    private val fieldMap: Map<String, GraphDbField> by lazy {
        val includedColumns = table.includedColumns()
        val columns = (if (includedColumns.isEmpty()) table.columns else includedColumns) - table.excludedColumns()
        log.info("Creating field map of $columns")
        columns.map { table.columnToField(it) }.map { it.name to it }.toMap()
    }

    private val columnMap: Map<Column<*>, GraphDbField> by lazy {
        fieldMap.values.map { it.column to it }.toMap()
    }

    private val Column<*>.fieldName
        get() = columnMap[this]?.name ?: name

    private val singleQueryArgMap: Map<String, GraphDbArg> by lazy {
        table.singleQueryArgs().map { it.name to it }.toMap()
    }

    private val listQueryArgMap: Map<String, GraphDbArg> by lazy {
        table.listQueryArgs().map { it.name to it }.toMap()
    }

    fun fields(container: GraphQLWiring): List<GraphQLFieldDefinition> {
        val list = mutableListOf<GraphQLFieldDefinition>()
        if (singleFieldName != null)
            list.add(graphQLFieldDefinition {
                name(singleFieldName)
                argument(singleQueryArgMap.values.map { it.graphQLArg() })
                type(container.type(this@TableWiring))
                dataFetcher { it.fetchDbData() }
            })
        if (listFieldName != null)
            list.add(graphQLFieldDefinition {
                name(listFieldName)
                argument(listQueryArgMap.values.map { it.graphQLArg() })
                type(GraphQLList(container.type(this@TableWiring)))
                dataFetcher { it.fetchDbData() }
            })
        return list
    }

    val graphQLArguments
        get() = singleQueryArgMap.values + listQueryArgMap.values

    open fun T.columnToField(column: Column<*>): GraphDbField = GraphDbField(column)

    /**
     * Attempt to fetch sql data with the given environments
     */
    fun fetch(env: DataFetchingEnvironment) = env.fetchDbData()

    /**
     * Fetches data
     * Returns null if invalid
     * Returns map if single selection
     * Returns list of maps if list selection
     * All outputs should be properly serializable
     */
    private fun DataFetchingEnvironment.fetchDbData(): Any? {
        val fields = dbFields()
        if (fields.isEmpty()) return null
        val columns = fields.mapNotNull { fieldMap[it]?.column }
        var condition: Op<Boolean>? = null
        val extensions = mutableListOf<Query.() -> Query>()
        val single = fieldType !is GraphQLList
        println("Query single $single")
        arguments.entries.forEach { (key, query) ->
            if (query == null) return@forEach
            val arg = (if (single) singleQueryArgMap else listQueryArgMap)[key] ?: return@forEach
            when (arg) {
                is GraphDbConditionArg -> {
                    val op = arg.where(query)
                    condition = if (condition == null) op else condition!! and op
                }
                is GraphDbExtensionArg -> extensions.add({ arg.modifier(this, query.toString()) })
            }
        }
        if (condition == null) {
            // todo reject if conditionless not allowed
        }
        return transaction {
            val selection = table.run { if (condition == null) selectAll() else select(condition!!) }
            val statement = extensions.fold(selection) { base, extension -> base.extension() }
            if (single) statement.firstOrNull()?.toMap(columns)
            else statement.map { row -> row.toMap(columns) }
        }
    }

    private fun DataFetchingEnvironment.dbFields(): List<String> {
        val selections = fields.firstOrNull { it.name == fieldDefinition.name }
                ?.selectionSet?.selections ?: return emptyList()
        log.info("Selections $selections")
        return selections.mapNotNull { (it as? Field)?.name }
    }

    private fun ResultRow.toMap(columns: List<Column<*>>): Map<String, Any?> =
            columns.map { it.fieldName to this[it] }.toMap()


    internal fun objectTypeFactory() = graphQLObjectType {
        name(tableName)
        description("SQL access to $tableName")
        fields(fieldMap.values.map { it.graphQLField() })
    }

    fun singleArgDefinitions(vararg columns: Column<*>) = columns.map {
        GraphDbConditionArg(it.fieldName, it)
    }

    fun listArgDefinitions(vararg columns: Column<*>) = columns.map {
        GraphDbConditionArg("${it.fieldName}s", it)
    }

    companion object {

        val limit = GraphDbExtensionArg("limit", Scalars.GraphQLInt, { limit(it.toInt()) }, "Upper limit for number of items to retrieve")

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
            val type = column.columnType
            return when (type) {
                is IntegerColumnType -> Scalars.GraphQLInt
                is LongColumnType -> Scalars.GraphQLLong
                is DecimalColumnType -> Scalars.GraphQLFloat
                is StringColumnType -> Scalars.GraphQLString
                is EntityIDColumnType<*> -> return graphQLType(type.idColumn)
                is EnumerationColumnType<*> -> scalarType(type.klass)
                else -> throw RuntimeException("Unknown type ${type::class.java}: ${type.sqlType()}")
            }
        }

        fun inputType(column: Column<*>): GraphQLInputType = graphQLType(column) as GraphQLInputType

        fun outputType(column: Column<*>): GraphQLOutputType {
            val type = graphQLType(column)
            return (if (column.columnType.nullable) type else GraphQLNonNull(type)) as GraphQLOutputType
        }

        fun scalarType(klass: Class<out Enum<*>>): GraphQLEnumType {
            val name = klass.simpleName
            println("Enum $name")
            return GraphQLEnumType.Builder()
                    .name(name)
                    .definition(EnumTypeDefinition(name))
//                            , klass.enumConstants.map(Any::toString).map { EnumValueDefinition(it) }, emptyList()))
                    .build()
        }
    }

}
