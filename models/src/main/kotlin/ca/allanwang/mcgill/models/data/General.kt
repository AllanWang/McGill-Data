package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel

/**
 * General response for executing put requests
 */
data class PutResponse(
        val ok: Boolean,
        val code: Int,
        val id: String = ""
) : McGillModel

/**
 * Response used whenever a request fails on the server end
 * Note that to keep things consistent, everything that isn't [status] and [message]
 * should have defaults.
 *
 * Extra parameters will only be updated if an error response is explicitly created
 */
data class ErrorResponse(
        val status: Int,
        val message: String,
        val extras: List<String> = emptyList()
) : McGillModel

data class ChangeDelta(
        val id: String,
        val type: String,
        val extras:List<String> = emptyList()
) : McGillModel