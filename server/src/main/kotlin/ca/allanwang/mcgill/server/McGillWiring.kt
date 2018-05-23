package ca.allanwang.mcgill.server

import ca.allanwang.mcgill.db.tables.Courses
import ca.allanwang.mcgill.db.tables.Groups
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

object GroupWiring : TableWiring(Groups,
        singleQueryArgs = argDefinitions(Groups.groupName),
        listQueryArgs = argDefinitions(Groups.groupName))

object CourseWiring : TableWiring(Courses,
        singleQueryArgs = argDefinitions(Courses.courseName),
        listQueryArgs = argDefinitions(Courses.courseName,
                Courses.season,
                Courses.teacher,
                Courses.year))