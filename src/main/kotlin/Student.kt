import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement

/**
 * McGill student model
 */
data class Student(
        /**
         * Unique student id
         * Note that this is only accessible by the owner through LDAP,
         * and not through general queries
         */
        val studentId: Int,
        /**
         * Short username
         * eg utest01
         */
        val shortUser: String,
        /**
         * Long username
         * eg unit.test
         */
        val longUser: String,
        /**
         * First name
         */
        val firstName: String,
        /**
         * Last name
         */
        val lastName: String) : DataDsl {

    val email: String = "$longUser@mail.mcgill.ca"

    override fun save() {
        Students.save(this)
    }

    override fun delete() {
        Students.delete(this)
    }
}

object Students : Table(), DataMapper<Student> {
    val id = integer("id").primaryKey().uniqueIndex()
    val shortUser = varchar("short_user", 10)
    val longUser = varchar("long_user", 30)
    val firstName = varchar("first_name", 20)
    val lastName = varchar("last_name", 20)

    override fun Query.toData(): List<Student> =
            map {
                Student(
                        studentId = it[id],
                        shortUser = it[shortUser],
                        longUser = it[longUser],
                        firstName = it[firstName],
                        lastName = it[lastName]
                )
            }

    override fun toTable(u: UpdateStatement, d: Student) {
        u[id] = d.studentId
        u[shortUser] = d.shortUser
        u[longUser] = d.longUser
        u[firstName] = d.firstName
        u[lastName] = d.lastName
    }

    /**
     * Retrieve student by [Student.studentId]
     */
    operator fun get(id: Int): Student? =
            selectData { this@Students.id eq id }

    /**
     * Retrieve student by [Student.studentId], [Student.shortUser], or [Student.longUser]
     */
    operator fun get(sam: String): Student? {
        val id = sam.toIntOrNull()
        if (id != null)
            return get(id)
        return selectData { (longUser eq sam) or (shortUser eq sam) }
    }

    override fun SqlExpressionBuilder.mapper(data: Student): Op<Boolean> = id eq data.studentId
}



