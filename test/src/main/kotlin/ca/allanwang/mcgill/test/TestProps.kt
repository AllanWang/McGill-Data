package ca.allanwang.mcgill.test

import ca.allanwang.kit.props.PropHolder

object TestProps : PropHolder("priv.properties", "../priv.properties") {

    val auth: Pair<String, String> by lazy { user to password }
    val user: String by string("TEST_USER", errorMessage = "No user provided")
    val password: String by string("TEST_PASSWORD", errorMessage = "No password provided")

    val testLdap: Boolean by boolean("TEST_LDAP", false)
    val hasTestUser: Boolean by lazy { user.isNotBlank() && password.isNotBlank() }

    val db: String by string("TEST_DB")
    val driver: String by string("TEST_DRIVER", "org.postgresql.Driver")
    val dbUser: String by string("TEST_DB_USER", "postgres")
    val dbPassword: String by string("TEST_DB_PASSWORD")
}