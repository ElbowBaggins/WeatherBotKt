package net.mythoclast.WeatherBotKt

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.HashMap
import kotlin.concurrent.thread

internal object WeatherBotCore {

    private val regex: Regex
    private val userLocations: MutableMap<String, String>

    init {
        regex = "((\\.(setlocation|(w(f|c(a?)|uk)?)(long)?)))(( ).*)?".toRegex()

        // Attempt to read locations file. Just use a blank map if we can't do that for some reason.
        try {
            userLocations = Gson()
                    .fromJson<MutableMap<String, String>>(File("locations.json").readText(Charsets.UTF_8))
        } catch (e: IOException) {
            e.printStackTrace()
            userLocations = HashMap<String, String>()
        }
        Runtime.getRuntime().addShutdownHook(thread(false) {
            // Attempt to serialize location map
            OutputStreamWriter(FileOutputStream("locations.json")).use {
                it.write(GsonBuilder().setPrettyPrinting().create().toJson(WeatherBotCore.userLocations))
            }
        })
    }

    internal fun getResponseForCommand(receivedCommand: String?, requestUser: String) : String? {
        val response: String?
        if (isValidCommand(receivedCommand)) {

            val command: String = receivedCommand!!

            // When someone says .wbhelp
            if (command.startsWith(".wbhelp")) {
                response = "Hello! I support several commands. .w (Automatic, Region-Based Units), .wf ('Murrkuh), " +
                                           ".wc (All SI Units), .wca ('Canadian' Units), and .wuk ('British') " +
                                           "units each deliver a weather overview containing only the most " +
                                           "commonly wanted data. Their siblings .wlong, .wflong, .wclong, " +
                                           ".wcalong, and .wuklong provide all available fields. " +
                                           "\r\nAll of these commands take one argument: " +
                                           "the location you want to see weather information for. " +
                                           "You can also supply a location to the .setlocation command to use any " +
                                           "of the previous commands with no arguments."
            } else if (command.matches(regex)) {

                val location: String

                // Split incoming message on space, we'll be ignoring the lead word when constructing location
                val splitMessage: List<String> = command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }

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
                    response = "Please specify a location."
                } else when(splitMessage[0]) {
                    ".w"           -> response = WeatherBotData.autoSummary(location)
                    ".wf"          -> response = WeatherBotData.fahrenheitSummary(location)
                    ".wc"          -> response = WeatherBotData.intlSummary(location)
                    ".wca"         -> response = WeatherBotData.canadaSummary(location)
                    ".wuk"         -> response = WeatherBotData.ukSummary(location)
                    ".wlong"       -> response = WeatherBotData.longAutoSummary(location)
                    ".wflong"      -> response = WeatherBotData.longFahrenheitSummary(location)
                    ".wclong"      -> response = WeatherBotData.longIntlSummary(location)
                    ".wcalong"     -> response = WeatherBotData.longCanadaSummary(location)
                    ".wuklong"     -> response = WeatherBotData.longUKSummary(location)
                    ".setlocation" -> {
                        userLocations[requestUser] = location
                        response = "$requestUser's location is set to: '$location'."
                    }
                    else -> response = null
                }
            } else {
                response = null
            }
        } else {
            response = null
        }
        return response
    }

    internal fun isValidCommand(command: String?) : Boolean {
        return command?.startsWith(".wbhelp") ?: false || command?.matches(regex) ?: false
    }
}
