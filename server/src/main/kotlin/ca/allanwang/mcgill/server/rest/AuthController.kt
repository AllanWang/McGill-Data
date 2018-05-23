package ca.allanwang.mcgill.server.rest

import ca.allanwang.mcgill.server.Auth
import ca.allanwang.mcgill.server.McGillGraphQL
import ca.allanwang.mcgill.server.utils.SessionResolver
import ca.allanwang.mcgill.server.utils.failNotFound
import ca.allanwang.mcgill.models.data.Session
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController {

    @PostMapping("/login")
    fun login(@RequestParam("username") username: String,
              @RequestParam("password") password: String,
              @RequestParam("expires_in") expiresIn: Long?): Session =
            Auth.authenticate(username, password, expiresIn)
                    ?: failNotFound(if (McGillGraphQL.ldapEnabled.get()) "$username not found in ldap" else "Ldap disabled")

    /**
     * Returns the session associated with the request header
     * See [SessionResolver]
     */
    @GetMapping("/session")
    fun getSession(session: Session): Session = session

}