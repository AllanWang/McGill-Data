package ca.allanwang.mcgill.graphql

import ca.allanwang.mcgill.graphql.kotlin.arg
import graphql.schema.idl.TypeRuntimeWiring

fun TypeRuntimeWiring.Builder.queryBuilder() {
    dataFetcher("hello") { "world" }
    dataFetcher("echo") { it.arg("content") }
}