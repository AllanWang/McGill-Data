package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.db.bindings.shortUserRef
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */

fun Table.groupName() = varchar("group_name", 30)
fun Table.groupNameRef() = groupName() references Groups.groupName

object Groups : Table() {
    val groupName = groupName().primaryKey()
}

object UserGroups : IntIdTable() {
    val groupName = groupNameRef()
    val shortUser = shortUserRef()
}