package ca.allanwang.mcgill.graphql.kotlin

import graphql.schema.DataFetchingEnvironment

inline fun <reified T : Any> DataFetchingEnvironment.arg(key: String): T? = arguments[key] as? T
inline fun <reified T : Any> DataFetchingEnvironment.arg(key: String, default: T): T = arguments[key] as? T ?: default