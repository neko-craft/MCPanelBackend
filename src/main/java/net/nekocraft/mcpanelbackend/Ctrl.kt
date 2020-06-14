package net.nekocraft.mcpanelbackend

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

internal val loginRequests = WeakHashMap<Player, (Boolean) -> Unit>()

@Suppress("BlockingMethodInNonBlockingContext")
@ImplicitReflectionSerializer
suspend fun ctrl(type: String, d: String, client: WebSocketSession): String? {
    when (type) {
        "login" -> {
            val data = Json.parse<LoginData>(d)
            val player = Bukkit.getPlayer(data.userName) ?: return Json.stringify(LoginRet("你没有进入游戏中!"))
            var user: UsersDao
            if (transaction {
                user = UsersDao.findById(player.uniqueId) ?: UsersDao.new(player.uniqueId) { }
                user.devices.count() > 2
            }) return Json.stringify(LoginRet("设备数量超过3个, 请进入游戏中输入 /panel devices 来删除!"))
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
                            var token: String
                            transaction {
                                token = (Devices.insert { t ->
                                    t[this.user] = user.id
                                    t[name] = data.name
                                } get Devices.id).toString()
                            }
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
                UUID.fromString(data.token)
            } catch (e: Exception) {
                return Json.stringify(TokenRet("UUID 错误!"))
            }
            val device = transaction { Devices.select { Devices.id.eq(token) }.singleOrNull() }
                    ?: return Json.stringify(TokenRet("UUID 已过期!"))
            val player = Bukkit.getOfflinePlayer(device[Devices.user].value)
            loginedMembers[client] = player
            return Json.stringify(TokenRet(null, player.name, player.isBanned,
                    player.isWhitelisted, Bukkit.hasWhitelist()))
        }
        "chat" -> {
            val user = loginedMembers[client] ?: return Json.stringify(Dialog("你还没有登录!"))
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
            val user = loginedMembers[client] ?: return Json.stringify(QuitRet("你还没有登录!"))
            val token = Json.parse<QuitData>(d).token
            val id = UUID.fromString(token)
            return transaction {
                val result = Devices.select {
                    Devices.id.eq(id) and Devices.user.eq(user.uniqueId)
                }.singleOrNull()
                Json.stringify(if (result == null) QuitRet("设备ID错误!") else {
                    if (Devices.deleteWhere { Devices.id.eq(id) } == 1) QuitRet(null, token)
                    else QuitRet("Token错误")
                })
            }

        }
        "heartBeat" -> {
            return """{"type":"heartBeat"}"""
        }
    }
    return null
}
