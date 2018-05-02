package ca.allanwang.mcgill.graphql.kotlin

import graphql.schema.DataFetchingEnvironment

fun <T : Any> DataFetchingEnvironment.arg(key: String): T? = arguments[key] as? T
fun <T : Any> DataFetchingEnvironment.arg(key: String, default: T): T = arguments[key] as? T ?: default