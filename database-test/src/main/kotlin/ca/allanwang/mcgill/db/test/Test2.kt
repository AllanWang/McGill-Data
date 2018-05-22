package ca.allanwang.mcgill.db.test

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.newOrUpdate(id: ID, update: T.() -> Unit): T =
        findById(id)?.apply(update) ?: new(id, update)

object TestTable : IdTable<Int>() {
    override val id = integer("id").clientDefault { (System.currentTimeMillis() % 10000).toInt() }.entityId()
    val name = varchar("name", 64)
}

class TestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestEntity>(TestTable) {

        fun fromName(name: String) = transaction {
            TestEntity.new {
                this.name = name
            }
        }

    }

    var name by TestTable.name

    fun withChild(name: String) = transaction {
        //        println("New child for ${id._value}")
        TestSubEntity.new {
            this.name = name
            this.parent = this@TestEntity
        }
    }

}

object TestSubTable : IdTable<Int>() {
    override val id = integer("id").clientDefault { (System.currentTimeMillis() % 10000).toInt() }.entityId()
    val name = varchar("name", 64)
    val parent = reference("parent", TestTable)
}

class TestSubEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestSubEntity>(TestSubTable)

    var name by TestSubTable.name
    var parent by TestEntity referencedOn TestSubTable.parent

    fun print() = transaction {
        println("TestSub $id")
    }

}

fun main(vararg args: String) {
    Database.connect("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1", "org.h2.Driver", "test", "test")
    val tables = arrayOf(TestTable, TestSubTable)
    transaction {
        SchemaUtils.create(*tables)

        val parent = TestEntity.fromName("test entity")
        println("Test Entity Count " + TestEntity.count())
        val child = parent.withChild("test child")
//        println("Test Sub Entity Count " + TestSubEntity.count())
        child.print()
        commit()
        transaction { SchemaUtils.drop(*tables) }
    }
}