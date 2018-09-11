package ca.allanwang.mcgill.server

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.graphql.McGillGraphQL
import ca.allanwang.mcgill.server.utils.SessionContext
import graphql.schema.GraphQLSchema
import graphql.servlet.GraphQLContext
import graphql.servlet.GraphQLContextBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.DispatcherServlet
import javax.servlet.http.HttpServletRequest
import javax.websocket.server.HandshakeRequest


fun main(args: Array<String>) {
    McGillServer.start()
    SpringApplication.run(ApplicationBootConfiguration::class.java, *args)
}

@SpringBootApplication
class ApplicationBootConfiguration : WithLogging("McGill GraphQL") {

    @Bean
    fun schema(): GraphQLSchema = McGillGraphQL.schema()

    @Autowired
    private lateinit var servlet: DispatcherServlet

    @Bean
    fun getCommandLineRunner(context: ApplicationContext): CommandLineRunner {
        servlet.setThrowExceptionIfNoHandlerFound(true)
        log.info("CommandLineRunner")
        return CommandLineRunner { }
    }

    @Bean
    fun context(): GraphQLContextBuilder = object : GraphQLContextBuilder {
        override fun build(httpServletRequest: HttpServletRequest?) = SessionContext(httpServletRequest)

        override fun build(handshakeRequest: HandshakeRequest?) = SessionContext(null)

        override fun build(): GraphQLContext = SessionContext(null)

    }

}