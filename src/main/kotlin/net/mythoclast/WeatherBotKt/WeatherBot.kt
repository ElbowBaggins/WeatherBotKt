package net.mythoclast.WeatherBotKt

import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.UtilSSLSocketFactory
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.KickEvent
import org.pircbotx.hooks.events.PartEvent
import org.pircbotx.hooks.types.GenericMessageEvent

import java.lang.ref.WeakReference
import java.net.Socket
import java.util.Arrays
import java.util.HashMap
import java.util.logging.Logger

class WeatherBot internal constructor() : ListenerAdapter() {

    private val lastLocation: MutableMap<String, String>
    private val regex = "((\\.w(f|c(a?)|uk)?)(long)?)(( ).*)?"

    val config: Configuration

    init {
        val confBuilder = Configuration.Builder()
        confBuilder.name = "WeatherBot"
        confBuilder.login = "radarkt"
        confBuilder.realName = "Sir Wethers \"Radar\" Bottingshire IV Esq."
        confBuilder.version = "WeatherBotKt 1.1.3"
        confBuilder.finger = "Where's your LeBaron, Freddy?"
        confBuilder.isAutoNickChange = true
        confBuilder.isAutoReconnect = true
        confBuilder.isShutdownHookEnabled = false
        confBuilder.autoReconnectAttempts = Int.MAX_VALUE
        confBuilder.nickservPassword = "YOUR NICKSERV PASSWORD"
        // Remove the line below if you're not using SSL. Or don't. Maybe it doesn't matter. Maybe it does.
        confBuilder.socketFactory = UtilSSLSocketFactory().trustAllCertificates()
        confBuilder.addServer("irc.somewhere.wat", 6697)
        confBuilder.addAutoJoinChannel("#somewhereland")
        confBuilder.addListener(this)
        this.config = confBuilder.buildConfiguration()

        lastLocation = HashMap<String, String>()
    }

    override fun onKick(event: KickEvent) {
        if(event.recipient?.nick.equals(config.name)) {
            rejoin(event.channel.getBot<PircBotX>(), event.channel!!.name)
        }
    }

    override fun onPart(event: PartEvent) {
        if(event.user.nick.equals(config.name)) {
            rejoin(event.channel.getBot<PircBotX>(), event.channel.name)
        }
    }

    override fun onGenericMessage(event: GenericMessageEvent?) {

        // Strings for the nick of the requesting user, and the location used to fetch data
        val requestUser: String
        val location: String

        // When someone says .whelp
        if (event?.message?.startsWith(".wbhelp") ?: false) {
            event?.respondWith("Hello! I support several commands. .w (Automatic, Region-Based Units), .wf ('Murrkuh), " +
                    ".wc (All SI Units), .wca ('Canadian' Units), and .wuk ('British') units each deliver a weather" +
                    " overview containing only the most commonly wanted data. Their siblings .wlong, .wflong, .wclong, " +
                    ".wcalong, and .wuklong provide all available fields. ")
            event?.respondWith("All of these commands take one argument: " +
                    "the location you want to see weather information for. If you have already requested weather data " +
                    "recently, then you may invoke any of the above with no argument and they will use your last-" +
                    "entered location.")
            return
        }
        if (event?.message?.matches(regex.toRegex()) ?: false) {

            // Get the requesting user's nick
            requestUser = event!!.user.nick

            // Split incoming message on space, we'll be ignoring the lead word when constructing location
            val splitMessage = Arrays.asList(
                    *event.message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

            // If there's more to the message than just the command
            if (1 < splitMessage.size) {

                // The location is the result of joining the rest of the incoming message string back together.
                location = splitMessage.subList(1, splitMessage.size).joinToString(" ")

                // If this wasn't blank, put it in the last location map so that users can
                // just run the command with no location.
                if(!"".equals(location)) {
                    lastLocation[requestUser] = location
                }
            } else {

                // If we're here, a location was not supplied with the command.
                // Lets see what happens if we try to get the user's last location used
                location = lastLocation[requestUser] ?: ""
            }


            // If this STILL comes back null, tell the user they have to supply a location.
            // Otherwise, run through the command list.
            if ("".equals(location)) {
                event.respondWith("Please specify a location.")
            } else if (".w".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.autoSummary(location))
            } else if (".wf".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.fahrenheitSummary(location))
            } else if (".wc".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.intlSummary(location))
            } else if (".wca".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.canadaSummary(location))
            } else if (".wuk".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.ukSummary(location))
            } else if (".wlong".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.longAutoSummary(location))
            } else if (".wflong".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.longFahrenheitSummary(location))
            } else if (".wclong".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.longIntlSummary(location))
            } else if (".wcalong".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.longCanadaSummary(location))
            } else if (".wuklong".equals(splitMessage[0])) {
                event.respondWith(WeatherBotData.longUKSummary(location))
            }
        }
    }

    private fun rejoin(bot: PircBotX, channelName: String) {
        Thread.sleep(5000)
        bot.send().joinChannel(channelName)
    }

    internal class BotShutdownHook(bot: PircBotX) : Thread() {
        private val thisBotRef: WeakReference<PircBotX>

        init {
            this.thisBotRef = WeakReference(bot)
            name = "WeatherBot-Shutdown Hook"
        }

        override fun run() {
            val thisBot = thisBotRef.get()
            if (PircBotX.State.DISCONNECTED != thisBot.state) {
                thisBot.stopBotReconnect()
                thisBot.sendIRC().quitServer("Back to you, cunt.")
                try {
                    if (thisBot.isConnected) {
                        // Woo reflection!
                        val botClass = thisBot.javaClass
                        val socketField = botClass.getDeclaredField("socket")
                        socketField.isAccessible = true
                        val botSocket = socketField.get(thisBot)

                        (botSocket as Socket).close()
                    }
                } catch (ex: Exception) {
                    Logger.getLogger("WeatherBot").finest("Unable to forcibly close socket\n" + ex.toString())
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    //Create our bot with the configuration
    val bot = PircBotX(WeatherBot().config)

    // Add customized version of built-in shutdown hook.
    Runtime.getRuntime().addShutdownHook(WeatherBot.BotShutdownHook(bot))
    //Connect to the server
    bot.startBot()
}