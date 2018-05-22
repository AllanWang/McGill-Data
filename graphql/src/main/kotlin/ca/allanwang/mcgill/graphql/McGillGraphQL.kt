package ca.allanwang.mcgill.graphql

import ca.allanwang.mcgill.db.McGillDb
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object McGillGraphQL {

    val debug = AtomicBoolean(true)
    val ldapEnabled = AtomicBoolean(false)
    val ldapAuth = AtomicReference<Pair<String, String>>()

    fun setup() {
        McGillDb.setup()
        if (McGillGraphQL.ldapEnabled.get())
            transaction { Auth.deleteTestUser() }
    }
}
