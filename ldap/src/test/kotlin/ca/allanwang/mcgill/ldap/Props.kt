package ca.allanwang.mcgill.ldap

import ca.allanwang.kit.props.PropHolder

object Props : PropHolder("priv.properties", "../priv.properties") {

    val testLdap : Boolean by boolean("TEST_LDAP", false)
    val testAuth: Pair<String, String> by lazy { testUser to testPassword }
    val testUser: String by string("TEST_USER", errorMessage = "No user provided")
    val testPassword: String by string("TEST_PASSWORD", errorMessage = "No password provided")

    val hasTestUser: Boolean by lazy { testUser.isNotBlank() && testPassword.isNotBlank() }
}