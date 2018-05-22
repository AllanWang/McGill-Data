package ca.allanwang.mcgill.ldap

import ca.allanwang.kit.logger.WithLogging
import org.junit.Assume
import org.junit.Test
import kotlin.test.*

class LdapTest {

    companion object : WithLogging() {

        private const val CSCIEN2 = "cscien2"

        init {
            Assume.assumeTrue("Testing LDAP connection",
                    McGillLdap.queryUser(CSCIEN2, Props.testAuth) != null)
        }

    }

    @Test
    fun bindCscien2() {
        val user = McGillLdap.queryUser(CSCIEN2, Props.testAuth)
        assertNotNull(user)
        log.info(user!!)
        assertEquals("Ctf Science", user.displayName)
        assertEquals(CSCIEN2, user.shortUser)
        assertTrue(user.groups.any { it.name == "520-Resource Accounts" })
        log.info(user)
    }

    @Test
    fun bindOtherLong() {
        val user = McGillLdap.queryUser("allan.wang", Props.testAuth)
        assertNotNull(user)
        log.info(user!!)
    }

    /**
     * Thank you yiwei for volunteering as tribute
     */
    @Test
    fun bindOtherUser() {
        val shortUser = "yxia19"
        val user = McGillLdap.queryUser(shortUser, Props.testAuth) ?: fail("Null user")
        log.info(user)
        assertEquals(shortUser, user.shortUser, "short user mismatch")
    }

    @Test
    fun bind() {
        val user = McGillLdap.queryUser("azsedzzz", Props.testAuth)
        assertNull(user, "User azsedzzz should be null")
    }

}
