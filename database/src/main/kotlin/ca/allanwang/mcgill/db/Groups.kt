package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.DataReceiver
import ca.allanwang.mcgill.db.bindings.OneToManyReceiver
import ca.allanwang.mcgill.db.bindings.save
import ca.allanwang.mcgill.db.statements.batchInsertOrIgnore
import ca.allanwang.mcgill.models.data.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */

fun Table.groupName() = varchar("group_name", 30)
fun Table.groupNameRef() = groupName() references Groups.groupName

object Groups : Table(), DataReceiver<String> {
    val groupName = groupName().primaryKey()

    override fun toTable(u: UpdateBuilder<*>, d: String) {
        u[groupName] = d
    }

    override val uniqueUpdateColumns: List<Column<*>> = listOf(groupName)

    override fun SqlExpressionBuilder.mapper(data: String): Op<Boolean> =
            (groupName eq data)
}

object UserGroups : Table(), OneToManyReceiver<User, String> {
    val shortUser = shortUserRef().primaryKey(0)
    val groupName = groupNameRef().primaryKey(1)

    override fun toTable(u: UpdateBuilder<*>, one: User, many: String) {
        u[shortUser] = one.shortUser
        u[groupName] = many
    }

    override fun getMany(one: User): List<String> = one.groups

    fun delete(user: User) {
        deleteWhere { UserGroups.shortUser eq user.shortUser }
    }
}