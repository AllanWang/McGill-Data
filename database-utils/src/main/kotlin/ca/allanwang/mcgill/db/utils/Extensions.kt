package ca.allanwang.mcgill.db.utils

private val regexUnderscoreToCamel = Regex("_([a-z\\d])")
private val regexCamelToUnderscore = Regex("[A-Z\\d]")

/**
 * Converts underscore to camel:
 * hello_world -> helloWorld
 *
 * Input is case insensitive
 */
fun String.toCamel(): String = regexUnderscoreToCamel.replace(toLowerCase()) {
    it.groupValues[1].toUpperCase()
}

/**
 * Converts camel to underscore:
 * helloWorld -> hello_world
 */
fun String.toUnderscore(): String = regexCamelToUnderscore.replace(this.decapitalize()) {
    "_" + it.groupValues[0].toLowerCase()
}
