package ca.allanwang.mcgill.graphql

import ca.allanwang.mcgill.db.tables.Courses
import ca.allanwang.mcgill.db.tables.Groups
import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.graphql.db.GraphDbArg
import ca.allanwang.mcgill.graphql.db.GraphDbField
import ca.allanwang.mcgill.graphql.db.GraphQLWiring
import ca.allanwang.mcgill.graphql.db.TableWiring
import org.jetbrains.exposed.sql.Column

object McGillGraphQL : GraphQLWiring(UserWiring)

object UserWiring : TableWiring<Users>(Users) {

    override fun Users.columnToField(column: Column<*>): GraphDbField = when (column) {
        userId -> GraphDbField("id", column)
        else -> GraphDbField(column)
    }

    override fun Users.singleQueryArgs(): List<GraphDbArg> =
            singleArgDefinitions(shortUser, longUser, userId)

    override fun Users.listQueryArgs(): List<GraphDbArg> =
            listArgDefinitions(shortUser, longUser, userId, email, faculty)
}

object GroupWiring : TableWiring<Groups>(Groups) {
    override fun Groups.singleQueryArgs(): List<GraphDbArg> =
            singleArgDefinitions(groupName)

    override fun Groups.listQueryArgs(): List<GraphDbArg> =
            listArgDefinitions(groupName)
}

object CourseWiring : TableWiring<Courses>(Courses) {
    override fun Courses.singleQueryArgs(): List<GraphDbArg> =
            singleArgDefinitions(courseName)

    override fun Courses.listQueryArgs(): List<GraphDbArg> =
            listArgDefinitions(courseName, season, teacher, year)
}