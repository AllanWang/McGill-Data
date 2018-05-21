package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.tables.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

object McGillDb {

    val tables = arrayOf(Users, Groups, UserGroups, Courses, UserCourses)

    fun setup() {
        transaction {
            create(*tables)
        }
    }

}
