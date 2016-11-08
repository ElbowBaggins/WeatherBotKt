package net.mythoclast.WeatherBotKt

import net.mythoclast.WeatherBotKt.ConfigFile.IRC

import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.UtilSSLSocketFactory
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.KickEvent
import org.pircbotx.hooks.events.PartEvent
import org.pircbotx.hooks.types.GenericMessageEvent

import java.lang.ref.WeakReference
import java.net.Socket
import java.util.logging.Logger

internal object WeatherBotIRC : ListenerAdapter() {

    private val config: Configuration
    private val client: PircBotX

    internal var partMessage: String

    init {
        val confBuilder = Configuration.Builder()

        confBuilder.name = IRC.name
        confBuilder.login = IRC.login
        confBuilder.realName = IRC.realName
        confBuilder.nickservPassword = IRC.nickservPassword
        confBuilder.finger = IRC.finger
        confBuilder.addServer(IRC.server, IRC.port)

        if(IRC.autoReconnectAttempts != 0) {
            confBuilder.isAutoReconnect = true
            if(IRC.autoReconnectAttempts == -1) {
                confBuilder.autoReconnectAttempts = Int.MAX_VALUE
            } else {
                confBuilder.autoReconnectAttempts = IRC.autoReconnectAttempts
            }
        }

        if(IRC.ssl) {
            confBuilder.socketFactory = UtilSSLSocketFactory().trustAllCertificates()
        }

        for(channel in IRC.channels) {
            confBuilder.addAutoJoinChannel(channel.asString)
        }

        confBuilder.version = "WeatherBotKt 1.3"
        confBuilder.isAutoNickChange = true
        confBuilder.isShutdownHookEnabled = false
        confBuilder.addListener(this)
        confBuilder.addListener(AdminCommandListener)

        config = confBuilder.buildConfiguration()
        partMessage = IRC.partMessage
        client = PircBotX(config)
        Runtime.getRuntime().addShutdownHook(IRCShutdownHook)
    }

    override fun onKick(event: KickEvent) {
        if(event.recipient?.nick.equals(config.name, true)) {
            rejoin(event.channel.getBot<PircBotX>(), event.channel.name)
        }
    }

    override fun onPart(event: PartEvent) {
        if(event.user.nick.equals(config.name)) {
            rejoin(event.channel.getBot<PircBotX>(), event.channel.name)
        }
    }

    override fun onGenericMessage(event: GenericMessageEvent?) {
        // Strings for the nick of the requesting user, and the location used to fetch data
        val requestUser: String = event?.user?.nick ?: ""

        if(WeatherBotCore.isValidCommand(event?.message)) {
            event?.respondWith(WeatherBotCore.getResponseForCommand(event.message, requestUser))
        }
    }

    internal fun start() {
        client.startBot()
    }

    private fun rejoin(bot: PircBotX, channelName: String) {
        Thread.sleep(5000)
        bot.send().joinChannel(channelName)
    }

    private object IRCShutdownHook : Thread() {
        private val thisBotRef = WeakReference<PircBotX>(client)

        override fun run() {
            val thisBot = thisBotRef.get()
            if (PircBotX.State.DISCONNECTED != thisBot.state) {
                thisBot.stopBotReconnect()
                thisBot.sendIRC().quitServer(WeatherBotIRC.partMessage)
                try {
                    if (thisBot.isConnected) {
                        // Woo reflection!
                        val socketField = thisBot.javaClass.getDeclaredField("socket")
                        socketField.isAccessible = true
                        val botSocket = socketField.get(thisBot)

                        (botSocket as Socket).close()
                    }
                } catch (e: Exception) {
                    Logger.getLogger("WeatherBot").finest("Unable to forcibly close socket\n ${e.toString()}")
                }
            }
        }
    }
}
