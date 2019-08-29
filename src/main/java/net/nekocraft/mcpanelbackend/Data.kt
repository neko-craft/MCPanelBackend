package net.nekocraft.mcpanelbackend

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(val userName: String, val name: String)

@Serializable
data class LoginRet(val error: String? = null, val token: String? = null, val type: String = "login")

@Serializable
data class TokenData(val token: String)

@Serializable
data class TokenRet(val error: String? = null, val name: String? = null,
                    val banned: Boolean? = null, val whiteList: Boolean? = null,
                    val needWhiteList: Boolean? = null, val type: String = "token")

@Serializable
data class ChatData(val message: String)

@Serializable
data class ChatRet(val name: String, val message: String, val type: String = "chat")

@Serializable
data class PlayerStatus(val name: String, val health: Double, val food: Int, val fishing: Boolean)

@Serializable
data class StatusData(val players: List<PlayerStatus>, val tps: Double, val type: String = "status")

@Serializable
data class Dialog(val message: String, val kind: String = "error", val type: String = "dialog")

@Serializable
data class BanList(val name: String, val reason: String?, val from: Long, val to: Long?,
                   val source: String)

@Serializable
data class PlayerData(val name: String, val registerTime: Long, val loginTime: Long, val banned: Boolean,
                      val whiteList: Boolean, val op: Boolean)

@Serializable
data class ListRet(val ban: List<BanList>, val players: List<PlayerData>, val type: String = "list")

@Serializable
data class PlayerJoinData(val name: String, val type: String = "playerJoin")

@Serializable
data class PlayerQuitData(val name: String, val type: String = "playerQuit")

@Serializable
data class QuitData(val token: String)

@Serializable
data class QuitRet(val error: String?, val token: String? = null, val type: String = "quit")
