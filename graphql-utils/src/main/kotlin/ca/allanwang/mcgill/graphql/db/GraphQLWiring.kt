package ca.allanwang.mcgill.graphql.db

import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeReference
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Helper class for generating graphql related data
 */
open class GraphQLWiring(private vararg val wirings: TableWiring<*>) {

    fun schema() = GraphQLSchema.newSchema()
            .query(GraphQLObjectType.newObject()
                    .name("query")
                    .fields(fields())
                    .build())
            .build()

    private val typeMapper: MutableMap<String, GraphQLObjectType> = mutableMapOf()

    /**
     * Returns the full object type of an object reference
     */
    fun type(tableWiring: TableWiring<*>): GraphQLOutputType {
        val name = tableWiring.tableName
        if (name in typeMapper) return GraphQLTypeReference(name)
        val type = tableWiring.objectTypeFactory()
        typeMapper[name] = type
        return type
    }

    private fun fields() = transaction {
        typeMapper.clear()
        val fields = wirings.flatMap { it.fields(this@GraphQLWiring) }
        typeMapper.clear()
        fields
    }
}