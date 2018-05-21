package ca.allanwang.mcgill.graphql

import ca.allanwang.kit.props.PropHolder
import ca.allanwang.mcgill.db.utils.DbConfigs

object Props : PropHolder("prod.properties", "../prod.properties"), DbConfigs {

    val auth: Pair<String, String> by lazy { user to password }
    val user: String by string("LDAP_USER", errorMessage = "No user provided")
    val password: String by string("LDAP_PASSWORD", errorMessage = "No password provided")

    val debug: Boolean by boolean("DEBUG", true)
    val ldapEnabled: Boolean by boolean("LDAP_ENABLED", !debug)

    val hasUser: Boolean by lazy { user.isNotBlank() && password.isNotBlank() }

    override val db: String by string("DB")
    override val dbDriver: String by string("DRIVER", "org.postgresql.Driver")
    override val dbUser: String by string("DB_USER", "postgres")
    override val dbPassword: String by string("DB_PASSWORD")
}