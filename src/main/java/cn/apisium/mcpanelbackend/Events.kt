package cn.apisium.mcpanelbackend

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.event.*
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@ImplicitReflectionSerializer
@OptIn(UnstableDefault::class)
class Events: Listener {
    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        val data = Frame.Text(Json.stringify(ChatRet(e.player.name, e.message)))
        runBlocking { launch { members.forEach { it.outgoing.send(data) } } }
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val data = Frame.Text(Json.stringify(PlayerJoinData(e.player.name)))
        runBlocking { launch { members.forEach { it.outgoing.send(data) } } }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        val data = Frame.Text(Json.stringify(PlayerQuitData(e.player.name)))
        runBlocking { launch { members.forEach { it.outgoing.send(data) } } }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onWebChat(e: AsyncWebChatEvent) {
        val msg = "§7[网页] §f${e.player.name}§7: ${e.message}"
        Bukkit.broadcastMessage(msg)
    }
}

open class AsyncWebChatEvent(val player: OfflinePlayer, var message: String): Event(true), Cancellable {
    private var cancelled = false

    override fun setCancelled(c: Boolean) { cancelled = c }
    override fun isCancelled() = cancelled
    @Suppress("RecursivePropertyAccessor")
    override fun getHandlers(): HandlerList = handler

    companion object {
        private val handler = HandlerList()
        @Suppress("Unused")
        @JvmStatic fun getHandlerList() = handler
    }
}
