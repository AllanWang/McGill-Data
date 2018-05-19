package ca.allanwang.mcgill.graphql.rest

import ca.allanwang.mcgill.graphql.Auth
import ca.allanwang.mcgill.graphql.Props
import ca.allanwang.mcgill.graphql.server.SessionResolver
import ca.allanwang.mcgill.graphql.server.failNotFound
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
              @RequestParam("expires_in", defaultValue = Auth.defaultExpiresIn.toString()) expiresIn: Long): Session =
            Auth.authenticate(username, password)
                    ?: failNotFound(if (Props.ldapEnabled) "$username not found in ldap" else "Ldap disabled")

    /**
     * Returns the session associated with the request header
     * See [SessionResolver]
     */
    @GetMapping("/session")
    fun getSession(session: Session): Session = session

}