package ca.allanwang.mcgill.graphql.kotlin

import com.google.common.base.CaseFormat

fun String.toCamel(): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this)
fun String.toUnderscore(): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this)

