package ca.allanwang.mcgill.ldap

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.test.TestProps
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.*


class LdapTest {

    companion object : WithLogging() {

        private const val CSCIEN2 = "cscien2"

        @BeforeClass
        @JvmStatic
        fun init() = Assume.assumeTrue("Testing LDAP connection",
                TestProps.testLdap && McGillLdap.queryUser(CSCIEN2, TestProps.auth) != null)

    }

    @Test
    fun bindCscien2() {
        val user = McGillLdap.queryUser(CSCIEN2, TestProps.auth)
        assertNotNull(user)
        log.info(user!!)
        assertEquals("Ctf Science", user.displayName)
        assertEquals(CSCIEN2, user.shortUser)
        assertTrue(user.groups.contains("520-Resource Accounts"))
        log.info(user)
    }

    @Test
    fun bindOtherLong() {
        val user = McGillLdap.queryUser("allan.wang", TestProps.auth)
        assertNotNull(user)
        log.info(user!!)
    }

    /**
     * Thank you yiwei for volunteering as tribute
     */
    @Test
    fun bindOtherUser() {
        val shortUser = "yxia19"
        val user = McGillLdap.queryUser(shortUser, TestProps.auth) ?: fail("Null user")
        log.info(user)
        assertEquals(shortUser, user.shortUser, "short user mismatch")
    }

    @Test
    fun bind() {
        val user = McGillLdap.queryUser("azsedzzz", TestProps.auth)
        assertNull(user, "User azsedzzz should be null")
    }

}
