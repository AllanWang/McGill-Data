@file:Suppress("NOTHING_TO_INLINE")

package ca.allanwang.mcgill.server.utils

import ca.allanwang.mcgill.models.data.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(RestError::class)
    fun handleEntityNotFound(
            ex: RestError): ResponseEntity<Any> {
        val status = HttpStatus.resolve(ex.error.status) ?: HttpStatus.BAD_REQUEST
        return ResponseEntity(ex.error, status)
    }
}

class RestError(val error: ErrorResponse) : RuntimeException()

inline fun fail(status: HttpStatus, message: String, extras: List<String> = emptyList()): Nothing =
        throw RestError(ErrorResponse(status.value(), message, extras))

inline fun failBadRequest(message: String, extras: List<String> = emptyList()): Nothing = fail(HttpStatus.BAD_REQUEST, message, extras)
inline fun failUnauthorized(message: String, extras: List<String> = emptyList()): Nothing = fail(HttpStatus.UNAUTHORIZED, message, extras)
inline fun failForbidden(message: String, extras: List<String> = emptyList()): Nothing = fail(HttpStatus.FORBIDDEN, message, extras)
inline fun failNotFound(message: String, extras: List<String> = emptyList()): Nothing = fail(HttpStatus.NOT_FOUND, message, extras)
inline fun failTimeout(message: String, extras: List<String> = emptyList()): Nothing = fail(HttpStatus.REQUEST_TIMEOUT, message, extras)
inline fun failInternal(message: String, extras: List<String> = emptyList()): Nothing = fail(HttpStatus.INTERNAL_SERVER_ERROR, message, extras)
