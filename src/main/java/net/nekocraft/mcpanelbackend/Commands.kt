package net.nekocraft.mcpanelbackend

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import net.nekocraft.mcpanelbackend.Devices.id
import net.nekocraft.mcpanelbackend.Devices.name
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Commands: CommandExecutor, TabCompleter {
    override fun onTabComplete(s: CommandSender, c: Command, l: String, a: Array<out String>): MutableList<String> {
        if (a.size == 1) return mutableListOf("devices")
        return mutableListOf()
    }

    override fun onCommand(s: CommandSender, c: Command, l: String, a: Array<out String>): Boolean {
        if (a.isEmpty()) {
            s.sendMessage("§e[用户中心] §c指令错误!")
            return true
        }
        if (s !is Player) {
            s.sendMessage("§e[用户中心] §c你不是玩家!")
            return true
        }
        when (a[0]) {
            "devices" -> {
                transaction {
                    val user = UsersDao.findById(s.uniqueId)
                    if (user == null) {
                        s.sendMessage("§e[用户中心] §c你还没有在网页上注册.")
                        return@transaction
                    }
                    s.sendMessage("§b§m                    §r §e[用户中心] §b§m                    ")
                    s.sendMessage("  §d设备列表:")
                    user.devices.forEachIndexed { i, it ->
                        s.sendMessage(
                                TextComponent("  ${i + 1}. ").apply { color = ChatColor.GRAY },
                                TextComponent(it[name] + "  "),
                                TextComponent("[删除设备]").apply {
                                    color = ChatColor.RED
                                    clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel deldevice ${it[id]}")
                                }
                        )
                    }
                    s.sendMessage("§b§m                                                       ")
                }
            }
            "confirm" -> {
                val req = loginRequests[s]
                if (req == null) s.sendMessage("§e[用户中心] §c你没有任何验证请求.")
                else req(true)
            }
            "deny" -> {
                val req = loginRequests[s]
                if (req == null) s.sendMessage("§e[用户中心] §c你没有任何验证请求.")
                else req(false)
            }
            "deldevice" -> {
                if (a.size == 2) {
                    try {
                        val uuid = UUID.fromString(a[1])
                        s.sendMessage(if (transaction { Devices.deleteWhere { id eq uuid } } > 0)
                            "§e[用户中心] §a删除成功." else "§e[用户中心] §c删除失败.")
                    } catch (e: Exception) {
                        s.sendMessage("§e[用户中心] §c参数错误.")
                    }
                } else s.sendMessage("§e[用户中心] §c参数错误.")
            }
        }
        return true
    }

}
