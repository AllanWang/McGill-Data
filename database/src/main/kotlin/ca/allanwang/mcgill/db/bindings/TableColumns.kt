package ca.allanwang.mcgill.db.bindings

import ca.allanwang.mcgill.db.Users
import org.jetbrains.exposed.sql.Table

/*
 * -----------------------------------------
 * Collection of shared column definitions
 * -----------------------------------------
 */

fun Table.shortUser() = varchar("short_user", 20)
fun Table.shortUserRef() = shortUser() references Users.shortUser