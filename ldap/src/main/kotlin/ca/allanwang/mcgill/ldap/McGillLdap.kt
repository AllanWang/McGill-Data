package ca.allanwang.mcgill.ldap

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.models.data.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.naming.Context
import javax.naming.Context.*
import javax.naming.NamingEnumeration
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
     * The auth username also <b>must</b> be the short user
     * Additionally, if the auth user differs from the one that is queried,
     * the studentId will not be returned
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

    /**
     * Helper function to get the [Sam] type of the provided string
     */
    fun samType(sam: String): Sam

    /**
     * Returns a nonnull ldap context if authentication is successful
     */
    fun bindLdap(shortUser: String, password: String): LdapContext?

    fun bindLdap(auth: Pair<String, String>) = bindLdap(auth.first, auth.second)
}

enum class Sam {
    SHORT_USER, LONG_USER, STUDENT_ID, NONE
}

object McGillLdap : McGillLdapContract, WithLogging() {

    private val shortUserRegex = Regex("[a-zA-Z]+[0-9]*")
    private val studentIdRegex = Regex("[0-9]+")
    private val longUserRegex = Regex("[a-zA-Z]+\\.[a-zA-Z]")

    override fun samType(sam: String): Sam = when {
        shortUserRegex.matches(sam) -> Sam.SHORT_USER
        longUserRegex.matches(sam) -> Sam.LONG_USER
        studentIdRegex.matches(sam) -> Sam.STUDENT_ID
        else -> Sam.NONE
    }

    override fun queryUser(username: String?, auth: Pair<String, String>): User? {
        if (username == null) return null
        val usernameType = samType(username)
        if (usernameType != Sam.SHORT_USER && usernameType != Sam.LONG_USER) {
            log.error("Cannot query $username")
            return null
        }
        if (samType(auth.first) != Sam.SHORT_USER) {
            log.error("Queried user auth ${auth.first} is not a short user")
            return null
        }
        val ctx = bindLdap(auth) ?: return null
        // todo check whether we should append mail.mcgill.ca ourselves, or if mcgill.ca is also valid
        val searchName = if (usernameType == Sam.LONG_USER) "userPrincipalName=$username@mail.mcgill.ca"
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

    /**
     * Wrapper to allow proper closing block
     */
    private inline fun <T : Any, R> T.use(block: (T) -> R, finally: (T) -> Unit): R {
        var exception: Throwable? = null
        try {
            return block(this)
        } catch (e: Exception) {
            exception = e
            throw e
        } finally {
            if (exception == null) finally(this)
            else try {
                finally(this)
            } catch (closeException: Throwable) {
                exception.addSuppressed(closeException)
            }
        }
    }

    private inline fun <T : Context, R> T.use(block: (T) -> R): R =
            use(block) { it.close() }

    private inline fun <T : Any, N : NamingEnumeration<T>, R> N.use(block: (N) -> R) =
            use(block) { it.close() }

    override fun autoSuggest(like: String, auth: Pair<String, String>, limit: Int): List<User> =
            bindLdap(auth)?.use { ctx ->
                val searchFilter = "(&(objectClass=user)(|(userPrincipalName=$like*)(samaccountname=$like*)))"
                val searchControls = SearchControls()
                searchControls.searchScope = SearchControls.SUBTREE_SCOPE
                ctx.search(LDAP_BASE, searchFilter, searchControls).use { results ->
                    val out = mutableListOf<User>()
                    var res = 0
                    val iter = results.iterator()
                    while (iter.hasNext() && res++ < limit) {
                        val user = iter.next().attributes.toUser(ctx)
                        if (user.longUser.split("@")
                                        .getOrNull(0)?.indexOf(".") ?: -1 > 0)
                            out.add(user)
                    }
                    out
                }
            } ?: emptyList()

    /**
     * Defines the environment necessary for [InitialLdapContext]
     * [user] must be a short user for the map to be valid
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

    override fun bindLdap(shortUser: String, password: String): LdapContext? = try {
        val auth = createAuthMap(shortUser, password)
        InitialLdapContext(auth, null)
    } catch (e: Exception) {
        log.error("Failed to bind to LDAP for $shortUser", e)
        null
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
                userId = attr("employeeID")
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

        val groups = mutableListOf<Group>()

        val courses = mutableListOf<Course>()

        members?.forEach { (name, semester) ->
            if (name == null) return@forEach
            if (semester == null) groups.add(Group(name))
            else {
                val crn = name.substringBefore(":").trim()
                val desc = name.substringAfter(":").trim()
                courses.add(Course(crn, desc, season = semester.season, year = semester.year))
            }
        }

        out.groups = groups
        out.courses = courses

        return out
    }

}
