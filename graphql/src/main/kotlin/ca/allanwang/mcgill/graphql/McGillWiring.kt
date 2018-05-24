package ca.allanwang.mcgill.graphql

import ca.allanwang.mcgill.db.tables.Groups
import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.graphql.db.*

object McGillGraphQL : GraphQLWiring(UserWiring, UserListWiring, GroupListWiring)

object UserWiring : FieldTableWiring("user", Users, false) {

    override val argMap: Map<String, GraphDbArg> = Users.run { listOf(shortUser, longUser, userId) }.map(::GraphDbConditionArg).toMap()

}

object UserListWiring : FieldTableWiring("users", Users, true) {
    override val argMap: Map<String, GraphDbArg> = (Users.run { listOf(faculty) }.map(::GraphDbConditionArg) + limit).toMap()
}

object GroupListWiring : FieldTableWiring("groups", Groups, true) {
    override val argMap: Map<String, GraphDbArg> = listOf(limit).toMap()
}
