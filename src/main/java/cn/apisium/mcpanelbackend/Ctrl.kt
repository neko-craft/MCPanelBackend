package cn.apisium.mcpanelbackend

import cn.apisium.nekoessentials.utils.DatabaseSingleton
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

internal val loginRequests = WeakHashMap<Player, (Boolean) -> Unit>()

@OptIn(UnstableDefault::class)
@ObsoleteCoroutinesApi
@Suppress("BlockingMethodInNonBlockingContext")
@ImplicitReflectionSerializer
suspend fun ctrl(type: String, d: String, client: WebSocketSession): String? {
    val db = DatabaseSingleton.INSTANCE
    when (type) {
        "login" -> {
            val data = Json.parse<LoginData>(d)
            val player = Bukkit.getPlayer(data.userName) ?: return Json.stringify(LoginRet("你没有进入游戏中!"))
            val uuid = player.uniqueId.toString()
            if (db.getPlayer(uuid).devices.size > 2)
                return Json.stringify(LoginRet("设备数量超过3个, 请进入游戏中输入 /panel devices 来删除!"))
            player.sendMessage("§b§m                    §r §e[用户中心] §b§m                    ")
            player.sendMessage("  §d收到新的登陆设备请求 §7(${data.name}):")
            player.sendMessage(
                    TextComponent("        "),
                    TextComponent("[拒绝登陆]").apply {
                        color = ChatColor.RED
                        clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel deny")
                    },
                    TextComponent("    "),
                    TextComponent("[确认登陆]").apply {
                        color = ChatColor.GREEN
                        clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel confirm")
                    }
            )
            player.sendMessage("  §7请确认是本人操作后再点击上方的确认按钮以登陆.")
            player.sendMessage("§b§m                                                       ")
            loginRequests[player] = {
                if (it) {
                    runBlocking {
                        @Suppress("UNINITIALIZED_VARIABLE") launch {
                            if (data.name.length > 16) {
                                client.outgoing.send(Frame.Text(Json.stringify(LoginRet("设备名过长!"))))
                                return@launch
                            }
                            val user = db.getPlayer(uuid)
                            val token = UUID.randomUUID().toString()
                            user.devices.add(Device(token, data.name))
                            db.savePlayer(uuid, user)
                            val map = db.getDeviceMap()
                            map[token] = uuid
                            db.saveDeviceMap(map)
                            client.outgoing.send(Frame.Text(Json.stringify(LoginRet(token = token))))
                        }
                    }
                    player.sendMessage("§e[用户中心] §a成功登陆.")
                } else {
                    runBlocking {
                        launch { client.outgoing.send(Frame.Text(Json.stringify(LoginRet("拒绝授权!")))) }
                    }
                    player.sendMessage("§e[用户中心] §c你拒绝了授权.")
                }
                loginRequests.remove(player)
            }
        }
        "token" -> {
            val data = Json.parse<TokenData>(d)
            val token = try {
                UUID.fromString(data.token).toString()
            } catch (e: Exception) {
                return Json.stringify(TokenRet("UUID 错误!"))
            }
            val id = db.getDeviceMap()[token] ?: return Json.stringify(TokenRet("UUID 已过期!"))
            db.getPlayer(id).devices.find { it.id == token } ?: return Json.stringify(TokenRet("UUID 已过期!"))
            val player = Bukkit.getOfflinePlayer(UUID.fromString(id))
            loggedMembers[client] = player
            return Json.stringify(TokenRet(null, player.name, player.isBanned,
                    player.isWhitelisted, Bukkit.hasWhitelist()))
        }
        "chat" -> {
            val user = loggedMembers[client] ?: return Json.stringify(Dialog("你还没有登录!"))
            val name = user.name ?: return null
            if (user.isBanned || (Bukkit.hasWhitelist() && !user.isWhitelisted))
                return Json.stringify(Dialog("你没有发送聊天信息的权限!"))
            val data = Json.parse<ChatData>(d)
            val data2 = Frame.Text(Json.stringify(ChatRet(name, data.message)))
            members.forEach { it.outgoing.send(data2) }
            Bukkit.getPluginManager().callEvent(AsyncWebChatEvent(user, data.message))
        }
        "list" -> return listData
        "quit" -> {
            val player = loggedMembers[client] ?: return Json.stringify(QuitRet("你还没有登录!"))
            val token = Json.parse<QuitData>(d).token
            val uuid = player.uniqueId.toString()
            val user = db.getPlayer(uuid)
            if (!user.devices.removeIf { it.id == token }) return Json.stringify(QuitRet("UUID 已过期!"))
            db.savePlayer(uuid, user)
            val map = db.getDeviceMap()
            if (map.remove(token) != null) db.saveDeviceMap(map)
            return Json.stringify(QuitRet(null, token))
        }
        "heartBeat" -> {
            return """{"type":"heartBeat"}"""
        }
    }
    return null
}
