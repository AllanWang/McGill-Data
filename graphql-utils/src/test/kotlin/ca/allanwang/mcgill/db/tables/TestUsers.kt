package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.utils.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder

data class TestUser(var id: String, var name: String, var groups: List<TestGroup>) : ColMapper {

    /**
     * Fetches self from db and checks for match
     */
    fun assertMatch() = assertMatch(TestUsers) {
        TestUsers.id eq id
    }

    override fun colMap(): Map<Column<*>, Any?> = mapOf(
            TestUsers.id to id,
            TestUsers.name to name
    )
}

fun ColMapper.assertMatch(table: Table, where: SqlExpressionBuilder.() -> Op<Boolean>) {
    val map = table.getMap(limit = 1, where = where).firstOrNull() ?: fail("No items found")
    assertTrue(matches(map), "Data did not match sql map\n\n$this\n\n$map")
}

fun testUser(id: Int) = TestUser("utest$id", "Unit Test $id",
        listOf("Unit Test Group", "UT $id").map(::TestGroup))

object TestUsers : Table(), DataReceiver<TestUser> {
    val id = varchar("id", 20).primaryKey()
    val name = varchar("name", 20)

    override fun toTable(u: UpdateBuilder<*>, d: TestUser) {
        u[id] = d.id
        u[name] = d.name
    }

    override val uniqueUpdateColumns: List<Column<*>> = listOf(id)

    override fun SqlExpressionBuilder.mapper(data: TestUser): Op<Boolean> =
            id eq data.id
}

/*
 * -----------------------------------------------------
 * Model bindings
 * -----------------------------------------------------
 */
fun TestUser.save() {
    TestUsers.save(this)
    TestUserGroups.save(this, TestGroups)
}

fun TestUser.delete() {
    TestUsers.delete(this)
}