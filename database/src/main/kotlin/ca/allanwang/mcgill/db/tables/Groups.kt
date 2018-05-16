package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.bindings.DataReceiver
import ca.allanwang.mcgill.db.bindings.OneToManyReceiver
import ca.allanwang.mcgill.db.bindings.referenceCol
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
object Groups : Table(), DataReceiver<String> {
    val groupName = varchar("group_name", 128).primaryKey()

    override fun toTable(u: UpdateBuilder<*>, d: String) {
        u[groupName] = d
    }

    override val uniqueUpdateColumns: List<Column<*>> = listOf(groupName)

    override fun SqlExpressionBuilder.mapper(data: String): Op<Boolean> =
            (groupName eq data)
}

object UserGroups : Table(), OneToManyReceiver<User, String> {
    val shortUser = referenceCol(Users.shortUser, 0)
    val groupName = referenceCol(Groups.groupName, 1)

    override fun toTable(u: UpdateBuilder<*>, one: User, many: String) {
        u[shortUser] = one.shortUser
        u[groupName] = many
    }

    override fun getMany(one: User): List<String> = one.groups
}