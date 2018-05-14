package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.DbConfigs
import ca.allanwang.mcgill.db.bindings.connect
import ca.allanwang.mcgill.graphql.Props
import ca.allanwang.mcgill.graphql.UserWiring
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLOutputType

object KGraphDb : WithLogging() {
    private val registration: MutableMap<TableWiring, GraphQLOutputType> = mutableMapOf()

    fun getObjectType(wiring: TableWiring): GraphQLOutputType {
        if (wiring in registration)
//            return GraphQLTypeReference(wiring.tableName)
            return registration[wiring]!! // todo figure out if references is the right type
        registration[wiring] = wiring.objectType()
        return registration[wiring]!!
    }

    fun dbFields(): List<GraphQLFieldDefinition> = listOf(UserWiring).flatMap {
        listOf(it.singleQueryField(), it.listQueryField())
    }

    fun start(configs: DbConfigs = Props) {
        registration.clear()
        if (configs.db.isEmpty())
            throw RuntimeException("No db found; check config location")
        configs.connect()
    }

    fun destroy() {
        registration.clear()
    }
}

