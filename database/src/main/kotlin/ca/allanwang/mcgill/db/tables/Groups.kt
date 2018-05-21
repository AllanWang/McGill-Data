package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.utils.referenceCol
import org.jetbrains.exposed.sql.Table

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
object Groups : Table() {
    val groupName = varchar("group_name", 128).primaryKey()
}

object UserGroups : Table() {
    val shortUser = referenceCol(Users.shortUser, 0)
    val groupName = referenceCol(Groups.groupName, 1)
}