package net.mythoclast.weatherbotkt

import net.mythoclast.weatherbotkt.ConfigFile.IRC.adminUser

import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.PrivateMessageEvent
import kotlin.system.exitProcess

internal object AdminCommandListener : ListenerAdapter() {
  override fun onPrivateMessage(event: PrivateMessageEvent) {
    val user = event.user ?: return
    val splitMessage = event.message?.split(" ") ?: return
    if (!user.isVerified || user.nick != adminUser || splitMessage.size <= 1) return

    when (splitMessage[0]) {
      ".quit" -> {
        if (splitMessage.size >= 2) {
          WeatherBotIRC.partMessage = splitMessage.subList(1, splitMessage.size).joinToString(" ")
        }
        exitProcess(0)
      }
      ".say" -> {
        if (splitMessage.size < 3) return
        event.getBot<PircBotX>().send().message(
            splitMessage[1], splitMessage.subList(2, splitMessage.size)
            .joinToString(" ")
        )
      }
      ".join" -> {
        if (splitMessage.size != 2 || !splitMessage[1].startsWith("#")) return
        event.getBot<PircBotX>().send().joinChannel(splitMessage[1])
      }
    }
  }
}
