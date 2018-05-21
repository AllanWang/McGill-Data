package ca.allanwang.mcgill.db

import ca.allanwang.mcgill.models.bindings.McGillModel
import ca.allanwang.mcgill.models.data.User
import org.junit.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

@Test
fun main(vararg args: String) {
    TableGenerator.generate<User> {
        primaryKey = User::shortUser
        unique = setOf(User::longUser, User::userId)
    }
}

/**
 * Given a mcgill data model,
 * generate the skeleton table and transformers
 */
object TableGenerator {

    private val snakeRegex = Regex("[A-Z\\d]")

    fun String.toSnakeCase(): String = snakeRegex.replace(this) {
        "_${it.groupValues[0].toLowerCase()}"
    }

    class Configs<M : McGillModel> {
        var primaryKey: KProperty1<M, *>? = null
        var unique: Set<KProperty1<M, *>> = emptySet()
    }

    inline fun <reified M : McGillModel> generate(config: Configs<M>.() -> Unit = {}) {
        if (!M::class.isData)
            throw RuntimeException("Models should use Kotlin's data class")
        val configs = Configs<M>()
        configs.config()
        val builder = StringBuilder()
        val name = M::class.simpleName
        val tableContent = generateTableContent(configs)
        val toData = generateToData(configs)
        val toTable = generateToTable(configs)

        builder.append("object ${name}s : Table(), DataMapper<$name> {\n")
                .append(tableContent.joinToString("") { "\t$it\n" })
                .append("\n\toverride fun Query.toData(): List<$name> =\n")
                .append("\t\tmap {\n")
                .append("\t\t\t$name(\n")
                .append(toData.joinToString(",\n", postfix = "\n") { "\t\t\t\t$it" })
                .append("\t\t\t)\n")
                .append("\t\t}\n")
                .append("\n\toverride fun toTable(u: UpdateBuilder<Int>, d: $name) {\n")
                .append(toTable.joinToString("") { "\t\t$it\n" })
                .append("\t}\n")
                .append("}")
        println(builder.toString())
    }

    inline fun <reified M : McGillModel> generateTableContent(configs: Configs<M>): List<String> {
        val primaryKey = configs.primaryKey?.name
        val unique = configs.unique.map(KProperty1<M, *>::name)
        return M::class.memberProperties.map {
            val n = it.name
            val key = "\"${n.toSnakeCase()}\""
            val type = it.returnType
            if (type.jvmErasure == List::class)
                return@map "//TODO add table for $n" // lists must be provided separately
            val builder = StringBuilder()
            builder.append("val $n = ")
            builder.append(when (type.jvmErasure) {
                String::class -> "varchar($key, 20)"
                Int::class -> "integer($key)"
                Long::class -> "long($key)"
                else -> "varchar($key, 20 /* TODO verify */)"
            })
            if (n == primaryKey)
                builder.append(".primaryKey()")
            else if (n in unique)
                builder.append(".uniqueIndex()")
            if (type.isMarkedNullable)
                builder.append(".nullable()")
            builder.toString()
        }.sortedBy(::TableAttr)
    }

    class TableAttr(val string: String) : Comparable<TableAttr> {

        val isPrimary = string.contains(".primaryKey()")
        val isUnique = string.contains(".uniqueIndex()")

        override fun compareTo(other: TableAttr): Int {
            arrayOf(TableAttr::isPrimary, TableAttr::isUnique).forEach {
                val b1 = it.get(this)
                val b2 = it.get(other)
                b1.compareTo(b2).apply {
                    if (this != 0)
                        return -this // apparently true > false
                }
            }
            return string.compareTo(other.string)
        }
    }

    /**
     * Generates boilerplate for [DataMapper]
     */
    inline fun <reified M : McGillModel> generateToData(configs: Configs<M>): List<String> =
            M::class.memberProperties.map {
                val result = when (it.returnType.jvmErasure) {
                    List::class -> "emptyList() /* TODO update */"
                    else -> "it[${it.name}]"
                }
                "${it.name} = $result"
            }

    inline fun <reified M : McGillModel> generateToTable(configs: Configs<M>): List<String> =
            M::class.memberProperties.mapNotNull {
                val result = when (it.returnType.jvmErasure) {
                    List::class -> return@mapNotNull null
                    else -> "d.${it.name}"
                }
                "u[${it.name}] = $result"
            }

}