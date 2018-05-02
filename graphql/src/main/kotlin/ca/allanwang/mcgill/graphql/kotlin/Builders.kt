package ca.allanwang.mcgill.graphql.kotlin

import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal fun graphQLObjectType(builder: GraphQLObjectType.Builder.() -> Unit): GraphQLObjectType =
        GraphQLObjectType.newObject().apply(builder).build()

internal fun graphQLFieldDefinition(builder: GraphQLFieldDefinition.Builder.() -> Unit): GraphQLFieldDefinition =
        GraphQLFieldDefinition.newFieldDefinition().apply(builder).build()

internal fun graphQLArgument(builder: GraphQLArgument.Builder.() -> Unit): GraphQLArgument =
        GraphQLArgument.newArgument().apply(builder).build()