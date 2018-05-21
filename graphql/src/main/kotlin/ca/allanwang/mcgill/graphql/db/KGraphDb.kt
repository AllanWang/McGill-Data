package ca.allanwang.mcgill.graphql.db

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.DbConfigs
import ca.allanwang.mcgill.db.bindings.connect
import ca.allanwang.mcgill.db.tables.*
import ca.allanwang.mcgill.graphql.*
import ca.allanwang.mcgill.graphql.server.SessionContext
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

    fun start(configs: DbConfigs = Props) {
        log.info("Initializing")
        registration.clear()
        if (configs.db.isEmpty())
            throw RuntimeException("No db found; check config location")
        configs.connect()
        transaction {
            create(Sessions,
                    Users,
                    UserGroups, Groups,
                    UserCourses, Courses)
        }
        if (Props.ldapEnabled)
            Auth.deleteTestUser()
        log.info("Initialized")
    }

    fun destroy() {
        registration.clear()
    }

}

