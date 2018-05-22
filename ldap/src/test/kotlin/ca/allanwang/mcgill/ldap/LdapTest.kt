package ca.allanwang.mcgill.ldap

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.*

class LdapTest {

    companion object {

        private const val CSCIEN2 = "cscien2"

        @BeforeAll
        @JvmStatic
        fun assume() {
            Assumptions.assumeTrue(McGillLdap.queryUser(CSCIEN2, Props.testAuth) != null, "No LDAP connection")
        }

    }

    @Test
    fun bindCscien2() {
        val user = McGillLdap.queryUser(CSCIEN2, Props.testAuth)
        assertNotNull(user)
        println(user!!)
        assertEquals("Ctf Science", user.displayName)
        assertEquals(CSCIEN2, user.shortUser)
        assertTrue(user.groups.any { it.name == "520-Resource Accounts" })
    }

    @Test
    fun bindOtherLong() {
        val user = McGillLdap.queryUser("allan.wang", Props.testAuth)
        assertNotNull(user)
        println(user!!)
    }

    /**
     * Thank you yiwei for volunteering as tribute
     */
    @Test
    fun bindOtherUser() {
        val shortUser = "yxia19"
        val user = McGillLdap.queryUser(shortUser, Props.testAuth) ?: fail("Null user")
        println(user)
        assertEquals(shortUser, user.shortUser, "short user mismatch")
    }

    @Test
    fun bind() {
        val user = McGillLdap.queryUser("azsedzzz", Props.testAuth)
        assertNull(user, "User azsedzzz should be null")
    }

}
