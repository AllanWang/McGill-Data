package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.tables.*
import ca.allanwang.mcgill.db.utils.DbConfigs
import ca.allanwang.mcgill.graphql.*
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLOutputType
import graphql.servlet.GraphQLContextBuilder
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Bean

object KGraphDb : WithLogging() {
    private val registration: MutableMap<TableWiring, GraphQLOutputType> = mutableMapOf()

    fun getObjectType(wiring: TableWiring): GraphQLOutputType {
        if (wiring in registration)
//            return GraphQLTypeReference(wiring.tableName)
            return registration[wiring]!! // todo figure out if references is the right type
        registration[wiring] = wiring.objectType()
        return registration[wiring]!!
    }

    fun dbFields(): List<GraphQLFieldDefinition> = listOf(UserWiring, GroupWiring, CourseWiring).flatMap {
        listOf(it.singleQueryField(), it.listQueryField())
    }

    fun start(configs: DbConfigs?) {
        log.info("Initializing")
        registration.clear()
        configs?.connect()
        McGillGraphQL.setup()
        if (McGillGraphQL.ldapEnabled.get())
            Auth.deleteTestUser()
        log.info("Initialized")
    }

    fun destroy() {
        registration.clear()
    }

}

