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


@SpringBootApplication
class ApplicationBootConfiguration : WithLogging("McGill GraphQL") {

    @Bean
    fun schema(): GraphQLSchema {
        return GraphQLSchema.newSchema()
                .query(GraphQLObjectType.newObject()
                        .name("query")
                        .fields(KGraphDb.dbFields())
//                        .field { field ->
//                            field
//                                    .name("test")
//                                    .type(Scalars.GraphQLString)
//                                    .dataFetcher { environment -> "response" }
//                        }
                        .build())
                .build()
    }


//    @Bean
//    fun schema(): GraphQLSchema =
//            SchemaGenerator().makeExecutableSchema(loadSchema("ldap.graphqls"), buildRuntimeWiring())

    private fun buildRuntimeWiring(): RuntimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("QueryType") {
                it.apply { queryBuilder() }
            }
            .build()

    private fun loadSchema(vararg name: String): TypeDefinitionRegistry {
        val files = name.mapNotNull {
            File(this::class.java.classLoader.getResource(it)?.file ?: return@mapNotNull null)
        }.filter(File::isFile)
        if (files.size < name.size) {
            if (files.isEmpty())
                log.error("No schema models loaded")
            else
                log.error("Some schema models are not valid files")
        }
        val parser = SchemaParser()
        val registry = TypeDefinitionRegistry()
        files.forEach { registry.merge(parser.parse(it)) }
        return registry
    }

//            querySchema {
//                string("hello", "Basic static response") {
//                    "world"
//                }
//                string("echo", "Repeat") {
//                    it.arguments.keys.joinToString()
//                }
//            }
}

fun main(args: Array<String>) {
    KGraphDb.start()
    SpringApplication.run(ApplicationBootConfiguration::class.java, *args)
}