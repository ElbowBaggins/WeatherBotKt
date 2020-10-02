package net.mythoclast.weatherbotkt

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.TS3Config
import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent
import net.mythoclast.weatherbotkt.ConfigFile.TS3
import kotlin.concurrent.thread

internal object WeatherBotTS3 : TS3EventAdapter() {

  private val config: TS3Config = TS3Config()
  private val query: TS3Query
  private val api: TS3Api
  private val channelID: Int
  private val messageInChannel: Boolean

  init {
    config.setHost(TS3.host)
        .setQueryPort(TS3.port)

    query = TS3Query(config)

    query.connect()

    api = query.api
    api.login(TS3.username, TS3.password)
    api.selectVirtualServerById(TS3.vserverID.coerceAtLeast(1))
    api.setNickname(TS3.nickname)

    channelID = TS3.channelID.coerceAtLeast(1)
    messageInChannel = TS3.messageInChannel

    val self = api.getClientByNameExact(TS3.nickname, false)

    if (TS3.channelID > 0) {
      if (TS3.channelPassword != "") {
        api.moveClient(self.id, channelID, TS3.channelPassword)
      } else {
        api.moveClient(self.id, channelID)
      }
    }

    api.registerEvents(TS3EventType.TEXT_CHANNEL, TS3EventType.TEXT_SERVER)
    api.addTS3Listeners(this)


    Runtime.getRuntime().addShutdownHook(thread(false) {
      query.exit()
    })
  }

  private fun sendMessage(message: String) {
    if (messageInChannel) {
      api.sendChannelMessage(channelID, message)
    } else {
      api.sendServerMessage(message)
    }
  }

  override fun onTextMessage(event: TextMessageEvent) {
    val requestUser = event.invokerUniqueId

    if (WeatherBotCore.isValidCommand(event.message)) {
      sendMessage("\n" + WeatherBotCore.getResponseForCommand(event.message, requestUser, true))
    } else if (event.invokerName != TS3.nickname) {
      URLTitler.getListOfTitlesForMessage(event.message)?.let {
        sendMessage(it)
      }
    }
  }
}
