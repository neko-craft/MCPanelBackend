package net.nekocraft.mcpanelbackend

import cn.apisium.nekoessentials.utils.Serializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.iq80.leveldb.DB
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
internal fun DB.getPlayer(id: String): User {
    val bytes = this[("$id.mcPanel").toByteArray()]
    return if (bytes == null) User() else Json.parse(bytes.toString())
}

@ImplicitReflectionSerializer
@OptIn(UnstableDefault::class)
internal fun DB.savePlayer(id: String, user: User) {
    this.put(("$id.mcPanel").toByteArray(), Json.stringify(user).toByteArray())
}

private val DEVICE_MAP = ("mcPanelDevices").toByteArray()
internal fun DB.getDeviceMap(): HashMap<String, String> {
    val bytes = this[DEVICE_MAP]
    return if (bytes == null) HashMap() else Serializer.deserializeObject(bytes) as HashMap<String, String>
}

internal fun DB.saveDeviceMap(map: HashMap<String, String>) {
    this.put(DEVICE_MAP, Serializer.serializeObject(map))
}
