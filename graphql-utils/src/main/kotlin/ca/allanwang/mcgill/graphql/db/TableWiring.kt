package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.utils.toMap
import ca.allanwang.mcgill.graphql.kotlin.graphQLFieldDefinition
import ca.allanwang.mcgill.graphql.kotlin.graphQLObjectType
import graphql.Scalars
import graphql.language.*
import graphql.schema.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * A direct extraction of attributes from [DataFetchingEnvironment]
 */
data class FieldDbEnvironment(
        /**
         * The context value retrieved from [DataFetchingEnvironment.getContext]
         * without any casting
         */
        val context: Any?,
        /**
         * All the query information related to the current field
         * selection set fields can be used to further propagate the environment
         */
        val field: Field
) {

    val selections by lazy {
        field.selectionSet.selections.mapNotNull { (it as? Field)?.name }
    }

    val argMap: Map<String, String> by lazy {
        field.arguments.mapNotNull {
            val value = it.value.extractString() ?: return@mapNotNull null
            it.name to value
        }.toMap()
    }

    private fun Value.extractString(): String? = when (this) {
        is ArrayValue -> values.toString()
        is BooleanValue -> isValue.toString()
        is EnumValue -> name
        is FloatValue -> value.toString()
        is IntValue -> value.toString()
        is NullValue -> null
        is ObjectValue -> null // todo see if we want to support it
        is StringValue -> value
        is VariableReference -> name
        else -> {
            log.warn("Unknown value type $this")
            null
        }
    }

    private companion object : WithLogging()
}

abstract class FieldDbWiring<out FIELD : GraphDbField>(val name: String,
                                                       val table: Table,
                                                       val returnsList: Boolean) : WithLogging() {

    abstract val fieldMap: Map<String, FIELD>

    abstract val argMap: Map<String, GraphDbArg>

    /**
     * Fetches data
     * Returns null if invalid
     * Returns map if single selection
     * Returns list of maps if list selection
     * All outputs should be properly serializable
     */
    abstract fun fetch(env: FieldDbEnvironment): Any?

    /**
     * Attempts to fetch the field environment for the current wiring
     * from the provided data environment
     */
    protected open fun toDbEnvironment(env: DataFetchingEnvironment): FieldDbEnvironment? {
        val field = env.fields.firstOrNull { it.name == name } ?: return null
        return FieldDbEnvironment(env.getContext(), field)
    }

    fun field(container: GraphQLWiring): GraphQLFieldDefinition = graphQLFieldDefinition {
        name(name)
        argument(argMap.values.map { it.graphQLArg() })
        type(type(container))
        dataFetcher {
            val fieldEnv = toDbEnvironment(it) ?: return@dataFetcher null
            fetch(fieldEnv)
        }
    }

    fun type(container: GraphQLWiring): GraphQLOutputType =
            container.type(this).run { if (returnsList) GraphQLList(this) else this }

    internal open fun objectTypeFactory() = graphQLObjectType {
        name(name)
        description("SQL access to $name")
        fields(fieldMap.values.map { it.graphQLField() })
    }

    companion object {

        val limit = GraphDbExtensionArg("limit", Scalars.GraphQLInt, { limit(it.toInt()) },
                default = 100,
                description = "Upper limit for number of items to retrieve")

        /**
         * Get the column typing
         * Note that objects are wrapped with a reference.
         * [GraphQLOutputType] are already registered when the wiring is registered
         * todo Make sure that any [GraphQLInputType] is also registered in the process
         */
        private tailrec fun graphQLType(column: Column<*>): GraphQLType {
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

open class FieldTableWiring(name: String, table: Table, returnsList: Boolean) : FieldDbWiring<GraphDbColField>(name, table, returnsList) {

    override val fieldMap: Map<String, GraphDbColField> = table.columns.map(::GraphDbColField).toMap()

    override val argMap: Map<String, GraphDbArg> = (if (returnsList) table.columns.map(::GraphDbConditionArg) + listOf(limit)
    else table.indices.filter(Index::unique).flatMap { it.columns.toList() }.map(::GraphDbConditionArg)).toMap()

    override fun fetch(env: FieldDbEnvironment): Any? {
        val columns = env.selections.mapNotNull { fieldMap[it]?.column }
        if (columns.isEmpty()) return null
        var condition: Op<Boolean>? = null
        val extensions = mutableListOf<Query.() -> Query>()
        for ((key, value) in env.argMap) {
            val arg = argMap[key] ?: continue
            when (arg) {
                is GraphDbConditionArg -> {
                    val op = arg.call(value)
                    if (op != null)
                        condition = if (condition == null) op else condition and op
                }
                is GraphDbExtensionArg -> extensions.add({ arg.call(this, value) })
            }
        }
        // todo validate condition and extensions?
        return transaction {
            val selection = table.run { if (condition == null) selectAll() else select(condition) }
            val statement = extensions.fold(selection) { base, extension -> base.extension() }
            if (returnsList) statement.map { row -> row.toMap(columns) }
            else statement.firstOrNull()?.toMap(columns)
        }
    }

}