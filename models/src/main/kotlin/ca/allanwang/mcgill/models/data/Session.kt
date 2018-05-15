package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.bindings.McGillModel
import ca.allanwang.mcgill.models.internal.Base64
import com.fasterxml.jackson.annotation.JsonIgnore

data class Session(
        val id: String,
        val shortUser: String,
        val role: String
) : McGillModel {

    val token: String
        @JsonIgnore
        get() = "$shortUser:$id"

    companion object {

        const val NONE = "none"
        const val USER = "user"
        const val CTFER = "ctfer"
        const val ELDER = "elder"

        private const val BASE_64_FLAGS = Base64.NO_WRAP or Base64.URL_SAFE

        /**
         * Encode the supplied [shortUser] and [id] into a Base64 header
         * Naturally, this will only be valid to the server if the supplied
         * parameters are valid
         */
        fun encodeToHeader(shortUser: String?, id: String?): String =
                encodeToHeader("$shortUser:$id")

        fun encodeToHeader(token: String): String =
                Base64.encodeToString(token.toByteArray(), BASE_64_FLAGS)

        /**
         * Decode the input string, returning null if invalid
         * This ensures that the decode always matches the encoder
         * used to find [authHeader]
         */
        fun decodeHeader(header: String): String? =
                try {
                    String(Base64.decode(header, BASE_64_FLAGS))
                } catch (e: Exception) {
                    null
                }
    }
}
