package ca.allanwang.mcgill.db.internal

import ca.allanwang.kit.logger.WithLogging
import ca.allanwang.mcgill.db.bindings.DbConfigs
import ca.allanwang.mcgill.db.bindings.connect
import ca.allanwang.mcgill.test.Props

object DbSetup  {
    fun connect() {
        val configs: DbConfigs = object : DbConfigs {
            override val db: String = Props.testDb
            override val dbUser: String = Props.testDbUser
            override val dbPassword: String = Props.testDbPassword
            override val dbDriver: String = Props.testDriver
        }
        configs.connect()
    }
}