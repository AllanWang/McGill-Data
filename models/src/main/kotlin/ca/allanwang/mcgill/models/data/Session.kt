package ca.allanwang.mcgill.models.data

import ca.allanwang.mcgill.models.internal.Base64

data class Session(
        val id: String,
        val shortUser: String,
        var expiration: Long = -1L,
        var persistent: Boolean = true
) {

    fun isValid() = expiration == -1L || expiration > System.currentTimeMillis()

    val token: String
        get() = "$shortUser:$id"

    companion object {

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
