package ca.allanwang.mcgill.server

import ca.allanwang.kit.props.PropHolder
import ca.allanwang.mcgill.db.utils.DbConfigs

object Props : PropHolder("priv.properties", "../priv.properties") , DbConfigs {
    val ldapUser by string("LDAP_USER", errorMessage = "Ldap user not set")
    val ldapPassword by string("LDAP_PASSWORD", errorMessage = "Ldap password not set")
    override val db by string("DB")
    override val dbUser by string("DB_USER")
    override val dbPassword by string("DB_PASSWORD")
    override val dbDriver by string("DB_DRIVER")
}