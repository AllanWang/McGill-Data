package ca.allanwang.mcgill.graphql

import ca.allanwang.mcgill.db.tables.Users
import ca.allanwang.mcgill.graphql.db.TableWiring

object UserWiring : TableWiring(Users,
        singleQueryArgs = argDefinitions(Users.shortUser,
                Users.longUser,
                Users.id),
        listQueryArgs = argDefinitions(Users.shortUser,
                Users.longUser,
                Users.id,
                Users.email,
                Users.faculty))