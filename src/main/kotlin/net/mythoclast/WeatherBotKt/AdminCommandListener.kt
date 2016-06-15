package net.mythoclast.WeatherBotKt

import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.PrivateMessageEvent

class AdminCommandListener : ListenerAdapter() {
    override fun onPrivateMessage(event: PrivateMessageEvent) {
        val user = event.user ?: return
        val splitMessage = event.message?.split(" ") ?: return
        if(!user.isVerified || user.nick != ConfigFile.adminUser || splitMessage.size <= 1) return

        when(splitMessage[0]) {
            ".quit" -> {
                if(splitMessage.size >= 2) {
                    WeatherBot.partMessage = splitMessage.subList(1, splitMessage.size).joinToString(" ")
                }
                System.exit(0)
            }
            ".say" -> {
                if(splitMessage.size < 3) return
                event.getBot<PircBotX>().send().message(splitMessage[1], splitMessage.subList(2, splitMessage.size)
                        .joinToString(" " ))
            }
            ".join" -> {
                if(splitMessage.size != 2 || !splitMessage[1].startsWith("#")) return
                event.getBot<PircBotX>().send().joinChannel(splitMessage[1])
            }
        }
    }
}