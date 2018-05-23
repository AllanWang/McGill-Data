package ca.allanwang.mcgill.server.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.utils.DbConfigs
import ca.allanwang.mcgill.graphql.db.TableWiring
import ca.allanwang.mcgill.models.data.Session
import ca.allanwang.mcgill.server.*
import ca.allanwang.mcgill.server.utils.SessionContext
import graphql.GraphQLException
import graphql.schema.DataFetchingEnvironment
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

    fun dbFields(): List<GraphQLFieldDefinition> = listOf(UserWiring, GroupWiring, CourseWiring).flatMap {
        listOf(it.singleQueryField(), it.listQueryField())
    }

    // todo move where appropriate
    private val DataFetchingEnvironment.session: Session
        get() = (getContext<Any?>() as? SessionContext)?.session ?: throw GraphQLException("Not authorized")

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

