package ca.allanwang.mcgill.graphql

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.graphql.db.KGraphDb
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.io.File

fun main(args: Array<String>) {
    KGraphDb.start()
    SpringApplication.run(ApplicationBootConfiguration::class.java, *args)
}

@SpringBootApplication
class ApplicationBootConfiguration : WithLogging("McGill GraphQL") {

    @Bean
    fun schema(): GraphQLSchema {
        return GraphQLSchema.newSchema()
                .query(GraphQLObjectType.newObject()
                        .name("query")
                        .fields(KGraphDb.dbFields())
                        .build())
                .build()
    }


}