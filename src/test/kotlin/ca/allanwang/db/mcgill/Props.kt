package ca.allanwang.db.mcgill

import java.io.File
import java.util.*

object Props {

    private val props = Properties()

    init {
        val file = arrayOf("priv.properties")
                .map(::File)
                .firstOrNull(File::isFile)
        if (file != null) {
            println("Found properties")
            file.inputStream().use(props::load)
        } else {
            println("No properties found")
        }
    }

    fun get(key: String, default: String = ""): String =
            props.getProperty(key, default)

    fun getOrNull(key: String): String? =
            props.getProperty(key)

    val testDb = get("TEST_DB")
    val testDriver = get("TEST_DRIVER")
    val testUser = get("TEST_USER")
    val testPassword = get("TEST_PASSWORD")

}