package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.utils.DataReceiver
import ca.allanwang.mcgill.db.utils.OneToManyReceiver
import ca.allanwang.mcgill.db.utils.referenceCol
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

data class TestGroup(val name: String)

object TestGroups : Table(), DataReceiver<TestGroup> {
    val name = varchar("name", 20).primaryKey()

    override fun toTable(u: UpdateBuilder<*>, d: TestGroup) {
        u[name] = d.name
    }

    override val uniqueUpdateColumns: List<Column<*>> = listOf(name)

    override fun SqlExpressionBuilder.mapper(data: TestGroup): Op<Boolean> =
            name eq data.name
}

object TestUserGroups : Table(), OneToManyReceiver<TestUser, TestGroup> {
    val userId = referenceCol(TestUsers.id, 0)
    val groupName = referenceCol(TestGroups.name, 1)

    override fun toTable(u: UpdateBuilder<*>, one: TestUser, many: TestGroup) {
        u[userId] = one.id
        u[groupName] = many.name
    }

    override fun getMany(one: TestUser): List<TestGroup> = one.groups
}