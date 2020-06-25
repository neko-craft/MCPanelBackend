package net.nekocraft.mcpanelbackend

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

@ObsoleteCoroutinesApi
@ImplicitReflectionSerializer
class Commands constructor(instance: Main) : CommandExecutor, TabCompleter {
    private val db = instance.instance.db
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
                val user = db.getPlayer(s.uniqueId.toString())
                if (user.devices.isEmpty()) {
                    s.sendMessage("§e[用户中心] §c你还没有在网页上注册.")
                    return true
                }
                s.sendMessage("§b§m                    §r §e[用户中心] §b§m                    ")
                s.sendMessage("  §d设备列表:")
                user.devices.forEachIndexed { i, it ->
                    s.sendMessage(
                            TextComponent("  ${i + 1}. ").apply { color = ChatColor.GRAY },
                            TextComponent(it.name + "  "),
                            TextComponent("[删除设备]").apply {
                                color = ChatColor.RED
                                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/panel deldevice $i")
                            }
                    )
                }
                s.sendMessage("§b§m                                                       ")
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
                        val id = a[1].toInt()
                        val uuid = s.uniqueId.toString()
                        val user = db.getPlayer(uuid)
                        if (id >= user.devices.size) {
                            s.sendMessage("§e[用户中心] §c删除失败.")
                            return true
                        }
                        val device = user.devices.removeAt(id)
                        db.savePlayer(uuid, user)
                        val map = db.getDeviceMap()
                        if (map.remove(device.id) != null) db.saveDeviceMap(map)
                        s.sendMessage("§e[用户中心] §a删除成功.")
                    } catch (e: Exception) {
                        s.sendMessage("§e[用户中心] §c删除失败.")
                    }
                } else s.sendMessage("§e[用户中心] §c参数错误.")
            }
        }
        return true
    }

}
