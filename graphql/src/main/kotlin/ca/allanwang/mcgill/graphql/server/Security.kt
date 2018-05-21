package ca.allanwang.mcgill.graphql.server

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.graphql.Auth
import ca.allanwang.mcgill.models.data.Session
import graphql.servlet.GraphQLContext
import graphql.servlet.GraphQLContextBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


//@Configuration
//@EnableWebSecurity
//class SecurityConfig : WebSecurityConfigurerAdapter(), Loggable by WithLogging("SecurityConfig") {
//
//    /**
//     * Note that we handle authentication lazily.
//     * We will bind a filter to check for authentication and attach a session if it exists
//     * However, all endpoints will go through unless they explicitly request a session (by having one in the method argument)
//     * and cannot get it from the request attributes
//     */
//    override fun configure(http: HttpSecurity) {
//        http.addFilterBefore(AuthenticationFilter(), BasicAuthenticationFilter::class.java)
//                .authorizeRequests().antMatchers("**").anyRequest()
//    }
//
//}

private const val AUTHORIZATION_PROPERTY = "Authorization"
private const val BASIC = "Basic"
private const val TOKEN = "Token"
private const val SESSION = "session"
private const val SHORT_EXPIRATION = 60L * 60 * 1000 // one minute

@WebFilter
@Configuration
class AuthenticationFilter : GenericFilterBean() {

    private inline val log
        get() = logger

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpRequest = request as? HttpServletRequest ?: return log.trace("Aborting non http request")
            log.info("p ${request.pathTranslated} c ${request.contextPath} s ${request.servletPath}")
            val authorization = httpRequest.getHeader(AUTHORIZATION_PROPERTY) ?: return
            val session = getSession(authorization) ?: return

            log.info("Hello $session")
            println("HHH $session ${request.pathInfo} ${request.pathTranslated} ${request.contextPath} ${request.servletPath}")
            httpRequest.setAttribute(SESSION, session)
            (response as HttpServletResponse).sendError(404, "Test")
        } catch (e: Exception) {
            log.error("Authentication Error", e)
        } finally {
            chain.doFilter(request, response)
        }
    }

    private fun getSession(authorization: String): Session? {
        val parts = authorization.split(" ")
        if (parts.size != 2)
            return null
        val (authScheme, credentials) = parts
        log.info("Auth $authScheme, $credentials")
        when (authScheme) {
            TOKEN -> {
                val samAndToken = Session.decodeHeader(credentials)?.split(":")
                val sam = samAndToken?.getOrNull(0)?.split("@")?.getOrNull(0)
                val token = samAndToken?.getOrNull(1)
                if (sam == null || token == null) {
                    sam ?: log.warn("Bad sam passed")
                    token ?: log.warn("Bad token passed")
                    return null
                }
                return Auth.validate(token, sam)
            }
            BASIC -> {
                val samAndPassword = Session.decodeHeader(credentials)?.split(":")
                val username = samAndPassword?.getOrNull(0)?.split("@")?.getOrNull(0)
                val password = samAndPassword?.getOrNull(1)
                if (username == null || password == null) {
                    username ?: log.warn("Bad username passed")
                    password ?: log.warn("Bad password passed")
                    return null
                }
                return Auth.authenticate(username, password, SHORT_EXPIRATION)
            }
            else -> {
                log.warn("Unsupported auth scheme $authScheme")
                return null
            }
        }
    }

}

class SessionContext(request: Optional<HttpServletRequest>,
                     response: Optional<HttpServletResponse>) : GraphQLContext(request, response) {

    init {
        println("Session context")
    }

    val session: Session? by lazy {
        if (!request.isPresent) return@lazy null
        request.get().getAttribute(SESSION) as? Session
    }

}

/**
 * If any rest endpoint requires a session, it can simply request it as an argument
 * This resolver will fetch the session, if provided by the [AuthenticationFilter],
 * or throw an unauthorized exception if it doesn't exist
 */
class SessionResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
            parameter.parameterType == Session::class.java

    override fun resolveArgument(parameter: MethodParameter,
                                 mavContainer: ModelAndViewContainer,
                                 webRequest: NativeWebRequest,
                                 binderFactory: WebDataBinderFactory): Any =
            webRequest.getAttribute(SESSION, RequestAttributes.SCOPE_REQUEST) as? Session
                    ?: fail(HttpStatus.UNAUTHORIZED, "No session provided")

}

@Configuration
@EnableWebMvc
class WebMvcContext : WebMvcConfigurer, WithLogging() {

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        log.info("Registering resolver")
        argumentResolvers.add(SessionResolver())
    }
}
