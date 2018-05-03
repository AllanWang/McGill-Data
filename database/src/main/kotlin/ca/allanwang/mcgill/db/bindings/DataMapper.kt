package ca.allanwang.mcgill.db.bindings

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow

fun Query.readMap(mapper: SQLMapLevelGenerator.() -> Unit): Map<String, Any?> {
    val m = generateMapper(mapper)
    val data = mutableMapOf<String, Any?>()
    forEach { m.generate(data, it) }
    return data
}

fun generateMapper(mapper: SQLMapLevelGenerator.() -> Unit): SQLMapLevelGenerator {
    val generator = SQLMapLevelGenerator()
    generator.mapper()
    return generator
}

enum class SQLMapType {
    FIRST, LAST, LIST
}

class SQLAttributeException(msg: String) : RuntimeException(msg)

open class SQLMapLevelGenerator(var formatter: (key: String) -> String = String::toCamel) {
    private val keySet: MutableSet<String> = mutableSetOf()
    private val attrs: MutableSet<SQLAttribute> = mutableSetOf()

    private fun validate(name: String?, column: Column<*>? = null): String {
        val key = name ?: formatter(column?.name
                ?: throw SQLAttributeException("No name or column supplied"))
        if (!keySet.add(key))
            throw SQLAttributeException("Attr $key already contained in this level: $keySet")
        return key
    }

    private fun generate(generator: SQLMapLevelGenerator.() -> Unit): SQLMapLevelGenerator =
            SQLMapLevelGenerator(formatter).apply(generator)

    fun attr(column: Column<*>,
             name: String? = null,
             type: SQLMapType = SQLMapType.FIRST) {
        val key = validate(name, column)
        val attr = SQLColAttribute(column, key, type)
        attrs.add(attr)
    }

    fun attrs(columns: List<Column<*>>, type: SQLMapType = SQLMapType.FIRST) {
        columns.forEach {
            attr(it, type = type)
        }
    }

    fun first(column: Column<*>, name: String? = null) =
            attr(column, name, SQLMapType.FIRST)

    fun last(column: Column<*>, name: String? = null) =
            attr(column, name, SQLMapType.LAST)

    fun list(column: Column<*>, name: String? = null) =
            attr(column, name, SQLMapType.LIST)

    fun attr(name: String, type: SQLMapType, children: SQLMapLevelGenerator.() -> Unit) {
        val key = validate(name)
        val childGenerator = SQLMapLevelGenerator(formatter)
        childGenerator.children()
        val attr = SQLMapAttribute(key, type, childGenerator.attrs)
        attrs.add(attr)
    }

    fun first(name: String, children: SQLMapLevelGenerator.() -> Unit) =
            attr(name, SQLMapType.FIRST, children)

    fun last(name: String, children: SQLMapLevelGenerator.() -> Unit) =
            attr(name, SQLMapType.LAST, children)

    fun list(name: String, children: SQLMapLevelGenerator.() -> Unit) =
            attr(name, SQLMapType.LIST, children)

    fun generate(data: MutableMap<String, Any?>, row: ResultRow) {
        attrs.forEach {
            it.toMap(data, row)
        }
    }

}

interface SQLAttribute {
    val name: String
    val type: SQLMapType
    fun getData(row: ResultRow): Any?
    fun toMap(map: MutableMap<String, Any?>, row: ResultRow) {
        when (type) {
            SQLMapType.FIRST -> map.computeIfAbsent(name) { getData(row) }
            SQLMapType.LAST -> map[name] = getData(row)
            SQLMapType.LIST -> {
                val list = map[name] as? MutableList<Any?> ?: mutableListOf()
                list.add(getData(row))
                map[name] = list
            }

        }
    }
}

class SQLColAttribute(val column: Column<*>,
                      override val name: String,
                      override val type: SQLMapType) : SQLAttribute {
    override fun getData(row: ResultRow): Any? = row[column]
}

class SQLMapAttribute(override val name: String,
                      override val type: SQLMapType,
                      val children: Collection<SQLAttribute>) : SQLAttribute {
    override fun getData(row: ResultRow): Any? {
        val data = mutableMapOf<String, Any?>()
        children.forEach { it.toMap(data, row) }
        return data
    }
}