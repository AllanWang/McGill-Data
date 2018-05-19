package ca.allanwang.mcgill.graphql

import ca.allanwang.kit.logger.LogUtils
import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.graphql.db.KGraphDb
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.DispatcherServlet


fun main(args: Array<String>) {
    if (Props.debug)
        LogUtils.setLoggingLevel(LogManager.getLogger("LogUtils"), Level.TRACE)
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

    @Autowired
    private lateinit var servlet: DispatcherServlet

    @Bean
    fun getCommandLineRunner(context: ApplicationContext): CommandLineRunner {
        servlet.setThrowExceptionIfNoHandlerFound(true)
        log.info("CommandLineRunner")
        return CommandLineRunner { }
    }


}