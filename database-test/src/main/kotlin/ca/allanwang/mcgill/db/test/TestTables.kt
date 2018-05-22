package ca.allanwang.mcgill.db.test

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.transactions.transaction

object TestItems : IntIdTable() {

    val name = varchar("name", 64)

}

class TestItemDb(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestItemDb>(TestItems)

    var name by TestItems.name
    val children by TestSubItemDb referrersOn TestSubItems.parent

    override fun toString(): String = transaction {
        val sub = children.toList()
        "TestItem $name \n\tSubItems (${sub.size}) ${sub.joinToString("\n\t", prefix = "\n\t")}"
    }
}

object TestSubItems : IntIdTable() {

    val parent = reference("parent", TestItems, ReferenceOption.CASCADE)
    val name = varchar("name", 64)

}

class TestSubItemDb(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestSubItemDb>(TestSubItems)

    var name by TestSubItems.name
    val parent by TestItemDb referencedOn TestSubItems.parent

    override fun toString(): String = transaction {
        "TestSubItem $name - parent $parent"
    }
}