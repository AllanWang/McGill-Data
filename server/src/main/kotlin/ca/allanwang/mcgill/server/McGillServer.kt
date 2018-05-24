package ca.allanwang.mcgill.server

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.McGillDb
import ca.allanwang.mcgill.models.data.Session
import ca.allanwang.mcgill.server.utils.SessionContext
import graphql.GraphQLException
import graphql.schema.DataFetchingEnvironment
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object McGillServer : WithLogging() {

    val debug = AtomicBoolean(true)
    val ldapEnabled = AtomicBoolean(false)
    val ldapAuth = AtomicReference<Pair<String, String>>()

    fun ldapAuth(user: String, password: String) {
        val hasAuth = user.isNotBlank() && password.isNotBlank()
        ldapEnabled.set(hasAuth)
        if (hasAuth)
            ldapAuth.set(user to password)
    }

    fun start() {
        log.info("Initializing...")
        ldapAuth(Props.ldapUser, Props.ldapPassword)
        Props.connect()
        McGillDb.setup()
        if (ldapEnabled.get())
            transaction { Auth.deleteTestUser() }
        log.info("Initialized")
    }

    // todo move where appropriate
    private val DataFetchingEnvironment.session: Session
        get() = (getContext<Any?>() as? SessionContext)?.session ?: throw GraphQLException("Not authorized")

}
