package cn.apisium.mcpanelbackend

import io.ktor.http.cio.websocket.Frame
import io.ktor.util.InternalAPI
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import org.bukkit.Bukkit
import org.bukkit.Material

@InternalAPI
@OptIn(UnstableDefault::class)
@ImplicitReflectionSerializer
fun task() {
    if (members.isEmpty()) return
    val data = Frame.Text(Json.stringify(StatusData(
            Bukkit.getOnlinePlayers().map {
                PlayerStatus(it.name, it.health, it.foodLevel,
                        it.inventory.itemInMainHand.type == Material.FISHING_ROD)
            },
            Bukkit.getTPS()[0]
    )))
    runBlocking {
        launch {
            members.forEach {
                it.outgoing.send(data)
            }
        }
    }
}
