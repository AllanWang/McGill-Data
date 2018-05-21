package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.models.data.Group
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

/*
 * -----------------------------------------
 * Shared Column definitions
 * -----------------------------------------
 */
object Groups : IdTable<String>() {
    override val id get() = groupName
    val groupName = varchar("group_name", 128).primaryKey().entityId()
}

class GroupDb(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, GroupDb>(Groups)

    val groupName: String get() = id.value

    fun toJson(): Group = transaction {
        Group(groupName)
    }

    fun associate(user: UserDb): GroupDb = apply {
        UserGroups.insertIgnore {
            it[this.shortUser] = user.id
            it[this.groupName] = id
        }
    }
}

object UserGroups : Table() {
    val shortUser = reference("short_user", Users, ReferenceOption.CASCADE).primaryKey(0)
    val groupName = reference("group_name", Groups, ReferenceOption.CASCADE).primaryKey(1)
}