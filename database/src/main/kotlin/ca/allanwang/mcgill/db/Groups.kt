package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.DataMapper
import ca.allanwang.mcgill.db.bindings.mapWith
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

object Groups : Table(), DataMapper<String> {
    val groupName = groupName().primaryKey()

    override fun toData(row: ResultRow): String =
            row[groupName]

    override fun toTable(u: UpdateBuilder<*>, d: String) {
        u[groupName] = d
    }

    override fun SqlExpressionBuilder.mapper(data: String): Op<Boolean> =
            (groupName eq data)
}

object UserGroups : Table() {
    val shortUser = shortUserRef().primaryKey(0)
    val groupName = groupNameRef().primaryKey(1)

    operator fun get(sam: String): List<String> {
        val shortUser = if (User.isShortUser(sam))
            sam else Users[sam]?.shortUser ?: return emptyList()
        return (Groups innerJoin UserGroups).select { UserGroups.shortUser eq shortUser }
                .mapWith(Groups::toData)
    }

    fun save(user: User) {
        Groups.save(Groups.groupName, user.groups)
        batchInsertOrIgnore(user.groups) {
            this[UserGroups.shortUser] = user.shortUser
            this[UserGroups.groupName] = it
        }
    }

    fun delete(user: User) {
        deleteWhere { UserGroups.shortUser eq user.shortUser }
    }
}