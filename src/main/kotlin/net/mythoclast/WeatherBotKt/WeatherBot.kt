package net.mythoclast.WeatherBotKt

import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.UtilSSLSocketFactory
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.KickEvent
import org.pircbotx.hooks.events.PartEvent
import org.pircbotx.hooks.types.GenericMessageEvent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

import java.lang.ref.WeakReference
import java.net.Socket
import java.util.*
import java.util.logging.Logger

class WeatherBot internal constructor() : ListenerAdapter() {

    private val regex = "((\\.(setlocation|(w(f|c(a?)|uk)?)(long)?)))(( ).*)?"
    private val userLocations: MutableMap<String, String>

    val config: Configuration

    init {
        val confBuilder = Configuration.Builder()
        confBuilder.name = ConfigFile.name
        confBuilder.login = ConfigFile.login
        confBuilder.realName = ConfigFile.realName
        confBuilder.nickservPassword = ConfigFile.nickservPassword
        confBuilder.finger = ConfigFile.finger
        confBuilder.addServer(ConfigFile.server, ConfigFile.port)

        if(ConfigFile.autoReconnectAttempts != 0) {
            confBuilder.isAutoReconnect = true
            if(ConfigFile.autoReconnectAttempts == -1) {
                confBuilder.autoReconnectAttempts = Int.MAX_VALUE
            } else {
                confBuilder.autoReconnectAttempts = ConfigFile.autoReconnectAttempts
            }
        }

        if(ConfigFile.ssl) {
            confBuilder.socketFactory = UtilSSLSocketFactory().trustAllCertificates()
        }

        for(channel in ConfigFile.channels) {
            confBuilder.addAutoJoinChannel(channel.asString)
        }

        confBuilder.version = "WeatherBotKt 1.2"
        confBuilder.isAutoNickChange = true
        confBuilder.isShutdownHookEnabled = false
        confBuilder.addListener(this)
        this.config = confBuilder.buildConfiguration()

        // Attempt to read locations file. Just use a blank map if we can't do that for some reason.
        try {
            userLocations = Gson().fromJson<MutableMap<String, String>>(File("locations.json")
                    .readText(Charsets.UTF_8))
        } catch (e: IOException) {
            userLocations = HashMap<String, String>()
        }
    }

    override fun onKick(event: KickEvent) {
        if(event.recipient?.nick.equals(config.name)) {
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
        val requestUser: String
        val location: String

        // When someone says .wbhelp
        if (event?.message?.startsWith(".wbhelp") ?: false) {
            event?.respondWith("Hello! I support several commands. .w (Automatic, Region-Based Units), .wf ('Murrkuh), " +
                    ".wc (All SI Units), .wca ('Canadian' Units), and .wuk ('British') units each deliver a weather" +
                    " overview containing only the most commonly wanted data. Their siblings .wlong, .wflong, .wclong, " +
                    ".wcalong, and .wuklong provide all available fields. ")
            event?.respondWith("All of these commands take one argument: " +
                    "the location you want to see weather information for. You can also supply a location to the " +
                    ".setlocation command to use any of the previous commands with no arguments.")
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

            } else {
                // If we're here, a location was not supplied with the command.
                // Lets see what happens if we try to get the user's set location (if any)
                location = userLocations[requestUser] ?: ""
            }


            // If this STILL comes back empty, tell the user they have to supply a location.
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
            } else if (".setlocation".equals(splitMessage[0])) {
                userLocations[requestUser] = location
                event.respondWith("$requestUser's location is set to: '$location'.")
            }
        }
    }

    private fun rejoin(bot: PircBotX, channelName: String) {
        Thread.sleep(5000)
        bot.send().joinChannel(channelName)
    }

    internal class BotShutdownHook(bot: PircBotX, weatherBot: WeatherBot) : Thread() {
        private val thisBotRef: WeakReference<PircBotX>
        private val weatherBotRef: WeakReference<WeatherBot>

        init {
            this.thisBotRef = WeakReference(bot)
            this.weatherBotRef = WeakReference(weatherBot)
            name = "WeatherBot-Shutdown Hook"
        }

        override fun run() {
            val thisBot = thisBotRef.get()
            val weatherBot = weatherBotRef.get()
            if (PircBotX.State.DISCONNECTED != thisBot.state) {
                thisBot.stopBotReconnect()
                thisBot.sendIRC().quitServer(ConfigFile.partMessage)
                Logger.getLogger("WeatherBot").severe("Part Message: " + ConfigFile.partMessage)
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
                // Attempt to serialize location map
                OutputStreamWriter(FileOutputStream("locations.json")).use {
                    it.write(GsonBuilder().setPrettyPrinting().create().toJson(weatherBot.userLocations))
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    //Create our bot with the configuration
    val weatherBot = WeatherBot()
    val ircBot = PircBotX(weatherBot.config)

    // Add customized version of built-in shutdown hook.
    Runtime.getRuntime().addShutdownHook(WeatherBot.BotShutdownHook(ircBot, weatherBot))
    //Connect to the server
    ircBot.startBot()
}