package ca.allanwang.mcgill.db.tables

import ca.allanwang.mcgill.db.bindings.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import kotlin.test.assertEquals
import kotlin.test.fail

data class TestUser(val id: String, val name: String, val groups: List<TestGroup>) : ColMapper {

    /**
     * Fetches self from db and checks for match
     */
    fun assertMatch() {
        val dbUser = Users.getMap {
            TestUsers.id eq id
        }.firstOrNull() ?: fail("No db user found for $this")

        assertEquals(id, dbUser["id"])
    }

    override fun colMap(): Map<Column<*>, Any?> = mapOf(
            TestUsers.id to id,
            TestUsers.name to name
    )
}

fun testUser(id: Int) = TestUser("utest$id", "Unit Test $id",
        listOf("Unit Test Group", "UT $id").map(::TestGroup))

object TestUsers : Table(), DataReceiver<TestUser> {
    val id = varchar("id", 20).primaryKey()
    val name = varchar("name", 20).uniqueIndex()

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