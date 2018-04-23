package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel

data class SessionRequest(
        var username: String,
        var password: String,
        var expiration: Long = -1L,
        var persistent: Boolean = true
) : McGillModel