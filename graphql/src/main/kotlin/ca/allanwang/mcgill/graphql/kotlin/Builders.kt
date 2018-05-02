package ca.allanwang.mcgill.graphql.kotlin

import graphql.Scalars
import graphql.schema.*

fun graphQLObjectType(name: String,
                      description: String? = null,
                      builder: GraphQLObjectType.Builder.() -> Unit = {}): GraphQLObjectType =
        GraphQLObjectType.newObject().apply {
            name(name)
            description(description)
            builder()
        }.build()

fun graphQLInputObjectType(name: String,
                           description: String? = null,
                           builder: GraphQLInputObjectType.Builder.() -> Unit = {}): GraphQLInputObjectType =
        GraphQLInputObjectType.newInputObject().apply {
            name(name)
            description(description)
            builder()
        }.build()

fun graphQLEnumType(name: String,
                    description: String? = null,
                    vararg values: String = emptyArray(),
                    builder: GraphQLEnumType.Builder.() -> Unit = {}): GraphQLEnumType =
        GraphQLEnumType.newEnum().apply {
            name(name)
            description(description)
            values.forEach { value(it) }
            builder()
        }.build()

fun graphQLArgument(name: String,
                    type: GraphQLInputType,
                    description: String? = null,
                    builder: GraphQLArgument.Builder.() -> Unit = {}): GraphQLArgument =
        GraphQLArgument.newArgument().apply {
            name(name)
            description(description)
            type(type)
            builder()
        }.build()

fun graphQLStringArgument(name: String,
                          description: String? = null,
                          builder: GraphQLArgument.Builder.() -> Unit = {}) =
        graphQLArgument(name, Scalars.GraphQLString, description, builder)

fun graphQLIntArgument(name: String,
                       description: String? = null,
                       builder: GraphQLArgument.Builder.() -> Unit = {}) =
        graphQLArgument(name, Scalars.GraphQLInt, description, builder)

fun graphQLFieldDefinition(name: String,
                           type: GraphQLOutputType,
                           description: String? = null,
                           builder: GraphQLFieldDefinition.Builder.() -> Unit = {}): GraphQLFieldDefinition =
        GraphQLFieldDefinition.newFieldDefinition().apply {
            name(name)
            description(description)
            type(type)
            builder()
        }.build()

fun graphQLStringField(name: String,
                       description: String? = null,
                       builder: GraphQLFieldDefinition.Builder.() -> Unit = {}) =
        graphQLFieldDefinition(name, Scalars.GraphQLString, description, builder)

fun graphQLIntField(name: String,
                    description: String? = null,
                    builder: GraphQLFieldDefinition.Builder.() -> Unit = {}) =
        graphQLFieldDefinition(name, Scalars.GraphQLInt, description, builder)

fun graphQLInputObjectField(name: String,
                            type: GraphQLInputType,
                            description: String? = null,
                            builder: GraphQLInputObjectField.Builder.() -> Unit = {}): GraphQLInputObjectField =
        GraphQLInputObjectField.newInputObjectField().apply {
            name(name)
            description(description)
            type(type)
            builder()
        }.build()

fun graphQLInputStringField(name: String,
                            description: String? = null,
                            builder: GraphQLInputObjectField.Builder.() -> Unit = {}) =
        graphQLInputObjectField(name, Scalars.GraphQLString, description, builder)

fun graphQLInputIntField(name: String,
                         description: String? = null,
                         builder: GraphQLInputObjectField.Builder.() -> Unit = {}) =
        graphQLInputObjectField(name, Scalars.GraphQLInt, description, builder)