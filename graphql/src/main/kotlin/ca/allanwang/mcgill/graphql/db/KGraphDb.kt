package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.graphql.Props
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLOutputType
import org.jetbrains.exposed.sql.Database

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
        log.info("Connecting to ${configs.db} with ${configs.dbUser}")
        Database.connect(url = configs.db,
                user = configs.dbUser,
                password = configs.dbPassword,
                driver = configs.dbDriver)
    }

    fun destroy() {
        registration.clear()
    }
}

interface DbConfigs {
    val db: String
    val dbUser: String
    val dbPassword: String
    val dbDriver: String
}