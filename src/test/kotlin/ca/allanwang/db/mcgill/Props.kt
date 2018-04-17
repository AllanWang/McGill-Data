package ca.allanwang.db.mcgill

import ca.allanwang.kit.props.PropHolder

object Props : PropHolder("priv.properties") {

    val testDb: String by string("TEST_DB")
    val testDriver: String by string("TEST_DRIVER", "org.postgresql.Driver")
    val testUser: String by string("TEST_USER", "postgres")
    val testPassword: String by string("TEST_PASSWORD")

}