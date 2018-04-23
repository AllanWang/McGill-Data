package ca.allanwang.mcgill.ldap

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.models.data.Course
import ca.allanwang.mcgill.models.data.Season
import ca.allanwang.mcgill.models.data.Semester
import ca.allanwang.mcgill.models.data.User
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.naming.Context.*
import javax.naming.NamingException
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.SearchControls
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

interface McGillLdapContract {
    /**
     * Queries [username] (short user or long user)
     * with [auth] credentials (username to password).
     * Resulting user is nonnull if it exists
     *
     * Note that [auth] may use different credentials than the [username] in question.
     * However, if a different auth is provided (eg from our science account),
     * the studentId cannot be queried
     *
     * You must be on the mcgill network for this to work
     * On linux, you may download openconnect and run
     * `sudo openconnect "securevpn.mcgill.ca"`
     */
    fun queryUser(username: String?, auth: Pair<String, String>): User?

    /**
     * Given a short username or long username,
     * attempt to query for a matching list of users
     */
    fun autoSuggest(like: String, auth: Pair<String, String>, limit: Int): List<User>
}

object McGillLdap : McGillLdapContract, WithLogging() {


    override fun queryUser(username: String?, auth: Pair<String, String>): User? {
        if (username == null) return null
        val ctx = bindLdap(auth) ?: return null
        val searchName = if (username.contains(".")) "userPrincipalName=$username@mail.mcgill.ca"
        else "sAMAccountName=$username"
        val searchFilter = "(&(objectClass=user)($searchName))"
        val searchControls = SearchControls()
        searchControls.searchScope = SearchControls.SUBTREE_SCOPE
        val results = ctx.search(LDAP_BASE, searchFilter, searchControls)
        val searchResult = results.nextElement()
        results.close()
        val user = searchResult?.attributes?.toUser(ctx)
        ctx.close()
        return user
    }

    override fun autoSuggest(like: String, auth: Pair<String, String>, limit: Int): List<User> {
        try {
            val ctx = bindLdap(auth) ?: return emptyList()
            val searchFilter = "(&(objectClass=user)(|(userPrincipalName=$like*)(samaccountname=$like*)))"
            val searchControls = SearchControls()
            searchControls.searchScope = SearchControls.SUBTREE_SCOPE
            val results = ctx.search(LDAP_BASE, searchFilter, searchControls)
            val out = mutableListOf<User>()
            var res = 0
            val iter = results.iterator()
            while (iter.hasNext() && res++ < limit) {
                val user = iter.next().attributes.toUser(ctx)
                if (user.longUser.split("@").getOrNull(0)?.indexOf(".") ?: -1 > 0)
                    out.add(user)
            }
            //todo update; a crash here will lead to the contents not closing
            results.close()
            ctx.close()
            return out
        } catch (ne: NamingException) {
            log.error("Could not get autosuggest", ne)
            return emptyList()
        }
    }

    /**
     * Defines the environment necessary for [InitialLdapContext]
     */
    private fun createAuthMap(user: String, password: String) = Hashtable<String, String>().apply {
        put(SECURITY_AUTHENTICATION, "simple")
        put(INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        put(PROVIDER_URL, "ldap://campus.mcgill.ca:389")
        put(SECURITY_PRINCIPAL, "CAMPUS\\$user")
        put(SECURITY_CREDENTIALS, password)
        put("com.sun.jndi.ldap.read.timeout", "5000")
        put("com.sun.jndi.ldap.connect.timeout", "500")
    }

    private fun bindLdap(auth: Pair<String, String>) = bindLdap(auth.first, auth.second)

    /**
     * Create [LdapContext] for given credentials
     */
    private fun bindLdap(user: String, password: String): LdapContext? {
        try {
            val auth = createAuthMap(user, password)
            return InitialLdapContext(auth, null)
        } catch (e: Exception) {
            log.error("Failed to bind to LDAP for $user", e)
            return null
        }
    }

    /**
     * Make sure that the regex matches values located in [Semester]
     */
    private val semesterRegex: Regex by lazy { Regex("ou=(fall|winter|summer) (2[0-9]{3})[^0-9]") }
    private const val LDAP_BASE = "dc=campus,dc=mcgill,dc=ca"
    private val dateFormat: SimpleDateFormat by lazy { SimpleDateFormat("yyyyMMddHHmmss.SX") }

    /**
     * Convert attributes to attribute list
     */
    private fun Attributes.toList(): List<Attribute> {
        val ids = iDs
        val data = mutableListOf<Attribute>()
        while (ids.hasMore()) {
            val id = ids.next()
            data.add(get(id))
        }
        ids.close()
        return (data)
    }

    /**
     * Convert attribute to string list
     */
    private fun Attribute.toList() = (0 until size()).map { get(it).toString() }


    /**
     * Creates a blank user and attempts to retrieve as many attributes
     * as possible from the specified attributes
     *
     * Note that when converting
     */
    private fun Attributes.toUser(ctx: LdapContext): User {
        fun attr(name: String) = get(name)?.get()?.toString() ?: ""
        val out = User(
                displayName = attr("displayName"),
                givenName = attr("givenName"),
                lastName = attr("sn"),
                shortUser = attr("sAMAccountName"),
                longUser = attr("userPrincipalName")
                        .toLowerCase()
                        .removeSuffix("@mail.mcgill.ca"),
                email = attr("mail"),
                middleName = attr("middleName"),
                faculty = attr("department"),
                id = attr("employeeID")
        )
        try {
            out.activeSince = dateFormat.parse(attr("whenCreated")).time
        } catch (e: ParseException) {

        }

        val members = get("memberOf")?.toList()?.mapNotNull {
            try {
                val cn = ctx.getAttributes(it, arrayOf("CN"))?.get("CN")?.get()?.toString()
                val groupValues = semesterRegex.find(it.toLowerCase(Locale.CANADA))?.groupValues
                val semester = if (groupValues != null) Semester(Season(groupValues[1]), groupValues[2].toInt())
                else null
                cn to semester
            } catch (e: NamingException) {
                null
            }
        }

        val groups = mutableListOf<String>()

        val courses = mutableListOf<Course>()

        members?.forEach { (name, semester) ->
            if (name == null) return@forEach
            if (semester == null) groups.add(name)
            else courses.add(Course(name, season = semester.season, year = semester.year))
        }

        out.groups = groups
        out.courses = courses

        return out
    }

}
