package net.nekocraft.mcpanelbackend

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.select
import java.util.*

object Users: UUIDTable()

object Devices: UUIDTable() {
    val user = reference("user", Users)
    val name = varchar("name", 16)
}

class UsersDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UsersDao>(Users)

    val devices
    get() = Devices.select { Devices.user.eq(id) }
}
