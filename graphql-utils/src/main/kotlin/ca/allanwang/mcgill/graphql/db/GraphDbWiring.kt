package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
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

    fun getQuery(wiring: FieldDbWiring<*, *>): Query? = wiring.run {
        val conditions: MutableMap<GraphDbConditionArg, String> = mutableMapOf()
        val extensions: MutableMap<GraphDbExtensionArg, String> = mutableMapOf()
        for ((key, value) in this@FieldDbEnvironment.argMap) {
            val arg = argMap[key] ?: continue
            when (arg) {
                is GraphDbConditionArg -> conditions[arg] = value
                is GraphDbExtensionArg -> extensions[arg] = value
            }
        }
        val condition: Op<Boolean>? = GraphDbConditionArg.fold(conditions)
        val extension: Query.() -> Query = { GraphDbExtensionArg.fold(extensions, this) }
        val selection = table.run { if (condition == null) selectAll() else select(condition) }
        val statement = selection.extension()
        statement
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

abstract class FieldDbWiring<F : GraphDbField, T : Any>(val name: String,
                                                            val table: Table,
                                                            val returnsList: Boolean) : WithLogging() {

    abstract val fieldMap: Map<String, F>

    open val argMap: Map<String, GraphDbArg> = (if (returnsList) table.columns.map(::GraphDbConditionArg) + listOf(limit)
    else table.indices.filter(Index::unique).flatMap { it.columns.toList() }.map(::GraphDbConditionArg)).toMap()

    /**
     * Fetches data
     * Returns null if invalid
     * Returns map if single selection
     * Returns list of maps if list selection
     * All outputs should be properly serializable
     */
    fun fetch(env: FieldDbEnvironment): Any? {
        if (env.selections.isEmpty()) return null
        val query = env.getQuery(this) ?: return null
        return transaction {
            val transition = transition(env, query)
            if (returnsList) transition.map { it.toOutput(env) }
            else transition.firstOrNull()?.toOutput(env)
        }
    }

    abstract fun transition(env: FieldDbEnvironment, query: Query): SizedIterable<T>

    abstract fun T.toOutput(env: FieldDbEnvironment): Map<String, Any?>

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

abstract class FieldTableWiring(name: String, table: Table, returnsList: Boolean) : FieldDbWiring<GraphDbColField, ResultRow>(name, table, returnsList) {

    override val fieldMap: Map<String, GraphDbColField> = table.columns.map(::GraphDbColField).toMap()

    override fun transition(env: FieldDbEnvironment, query: Query): SizedIterable<ResultRow> = query

    override fun ResultRow.toOutput(env: FieldDbEnvironment): Map<String, Any?> = env.selections.mapNotNull {
        val col = fieldMap[it]?.column ?: return@mapNotNull null
        it to this[col]
    }.toMap()

}

//abstract class FieldEntityWiring<ID : Comparable<ID>, E : Entity<ID>>(name: String, entityClass: EntityClass<ID, E>, returnsList: Boolean)
//    : FieldDbWiring<GraphDbRetrievalField<ID, E>>(name, entityClass.table, returnsList) {
//
//    override fun fetch(env: FieldDbEnvironment, conditions: Map<GraphDbConditionArg, String>, extensions: Map<GraphDbExtensionArg, String>): Any? {
//
//    }
//}