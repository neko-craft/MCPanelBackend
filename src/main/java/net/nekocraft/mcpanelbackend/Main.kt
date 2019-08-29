package net.nekocraft.mcpanelbackend

import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

internal val members = CopyOnWriteArrayList<WebSocketSession>()
internal val loginedMembers = ConcurrentHashMap<WebSocketSession, OfflinePlayer>()
internal var listData = ""

@ImplicitReflectionSerializer
private val errorMsg = Frame.Text(Json.stringify(Dialog("发生错误!")))

@Suppress("UNUSED")
@kotlinx.coroutines.ObsoleteCoroutinesApi
@ImplicitReflectionSerializer
class Main: JavaPlugin() {
    private lateinit var app: ApplicationEngine
    override fun onEnable() {
        Database.connect("jdbc:h2:${File(dataFolder, "db").absolutePath}", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(Users, Devices) }
        app = embeddedServer(Netty, 8124) {
            install(WebSockets)
            routing {
                webSocket("/ws") {
                    members.add(this)
                    try {
                        incoming.consumeEach {
                            if (it !is Frame.Text) return@consumeEach
                            try {
                                val (type, data) = it.readText().split('|', limit = 2)
                                outgoing.send(Frame.Text(ctrl(type, data, this) ?: return@consumeEach))
                            } catch (ignored: Exception) {
                                outgoing.send(errorMsg)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        members.remove(this)
                        loginedMembers.remove(this)
                    }
                }
            }
        }
        app.start()
        val cmd = server.getPluginCommand("panel")!!
        val cmdExec = Commands()
        cmd.setExecutor(cmdExec)
        cmd.tabCompleter = cmdExec
        server.pluginManager.registerEvents(Events(), this)
        server.scheduler.runTaskTimer(this, ::task, 0, 5 * 20)
        server.scheduler.runTaskTimer(this, fun () {
            listData = Json.stringify(
                    ListRet(
                            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).banEntries.map {
                                BanList(it.target, it.reason, it.created.time,
                                        it.expiration?.time, it.source)
                            },
                            Bukkit.getOfflinePlayers().mapNotNull {
                                val name = it.name
                                if (name == null) null else PlayerData(
                                        name,
                                        it.firstPlayed,
                                        it.lastSeen,
                                        it.isBanned,
                                        it.isWhitelisted,
                                        it.isOp
                                )
                            }
                    )
            )
        }, 0, 5 * 60 * 20)
    }

    override fun onDisable() {
        app.stop(0, 0, TimeUnit.SECONDS)
        members.clear()
        loginedMembers.clear()
        loginRequests.clear()
    }
}