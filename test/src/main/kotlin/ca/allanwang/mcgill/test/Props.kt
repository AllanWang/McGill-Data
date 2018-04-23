package ca.allanwang.mcgill.test

import ca.allanwang.kit.props.PropHolder

object Props : PropHolder("priv.properties", "../priv.properties") {

    val testAuth: Pair<String, String> by lazy { testUser to testPassword }
    val testUser: String by string("TEST_USER", errorMessage = "No user provided")
    val testPassword: String by string("TEST_PASSWORD", errorMessage = "No password provided")

    val hasTestUser: Boolean by lazy { testUser.isNotBlank() && testPassword.isNotBlank() }

    val testDb: String by string("TEST_DB")
    val testDriver: String by string("TEST_DRIVER", "org.postgresql.Driver")
    val testDbUser: String by string("TEST_DB_USER", "postgres")
    val testDbPassword: String by string("TEST_DB_PASSWORD")
}