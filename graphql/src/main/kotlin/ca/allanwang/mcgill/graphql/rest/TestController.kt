package ca.allanwang.mcgill.graphql.rest

import ca.allanwang.mcgill.graphql.server.failBadRequest
import ca.allanwang.mcgill.models.data.Session
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

    /**
     * Gets the current session if it exists
     */
    @GetMapping("/test/session")
    fun getAuth(session: Session): Session = session

    @GetMapping("/test/error")
    fun getError() {
        failBadRequest("Error Request", listOf("Extra", "Args"))
    }

}