package net.nekocraft.mcpanelbackend

import cn.apisium.nekoessentials.utils.DatabaseSingleton
import cn.apisium.nekoessentials.utils.Serializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
internal data class Device(
        val id: String,
        val name: String
)
@Serializable
internal data class User(
        val devices: ArrayList<Device> = ArrayList()
)

@ImplicitReflectionSerializer
@OptIn(UnstableDefault::class)
internal fun DatabaseSingleton.getPlayer(id: String): User {
    val bytes = this["$id.mcPanel"]
    return if (bytes == null) User() else Json.parse(bytes.toString())
}

@ImplicitReflectionSerializer
@OptIn(UnstableDefault::class)
internal fun DatabaseSingleton.savePlayer(id: String, user: User) {
    this.set("$id.mcPanel", Json.stringify(user).toByteArray())
}

private const val DEVICE_MAP = "mcPanelDevices"
@Suppress("UNCHECKED_CAST")
internal fun DatabaseSingleton.getDeviceMap(): HashMap<String, String> {
    val bytes = this[DEVICE_MAP]
    return if (bytes == null) HashMap() else Serializer.deserializeObject(bytes) as HashMap<String, String>
}

internal fun DatabaseSingleton.saveDeviceMap(map: HashMap<String, String>) {
    this.set(DEVICE_MAP, Serializer.serializeObject(map))
}
