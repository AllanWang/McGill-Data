package ca.allanwang.mcgill.graphql.kotlin

import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

fun graphQLObjectType(builder: GraphQLObjectType.Builder.() -> Unit): GraphQLObjectType =
        GraphQLObjectType.newObject().apply(builder).build()

fun graphQLFieldDefinition(builder: GraphQLFieldDefinition.Builder.() -> Unit): GraphQLFieldDefinition =
        GraphQLFieldDefinition.newFieldDefinition().apply(builder).build()

fun graphQLArgument(builder: GraphQLArgument.Builder.() -> Unit): GraphQLArgument =
        GraphQLArgument.newArgument().apply(builder).build()