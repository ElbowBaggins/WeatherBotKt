package net.mythoclast.WeatherBotKt

import com.github.salomonbrys.kotson.*
import com.google.common.io.Files
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import kotlin.system.exitProcess

internal object ConfigFile {

    private val configFile: JsonObject
    private val ircConf: JsonObject

    init {
        configFile = readFile()
        ircConf = configFile.getAsJsonObject("IRC")
    }

    internal object Core {
        internal val GEOCODE_KEY: String by configFile.byString
        internal val FORECAST_KEY: String by configFile.byString
    }

    internal object IRC {

        internal val name: String by ircConf.byString
        internal val nickservPassword: String by ircConf.byString

        internal val login: String by ircConf.byString
        internal val realName: String by ircConf.byString
        internal val finger: String by ircConf.byString
        internal val partMessage: String by ircConf.byString

        internal val server: String by ircConf.byString
        internal val port: Int by ircConf.byInt

        internal val autoReconnectAttempts: Int by ircConf.byInt
        internal val ssl: Boolean by ircConf.byBool
        internal val adminUser: String by ircConf.byString
        internal val channels: JsonArray by ircConf.byArray
    }

    private fun readFile(): JsonObject {
        try {
            return JsonParser().parse(Files.toString(File("config.json"), Charsets.UTF_8)).asJsonObject
        } catch (e: IOException) {
            Logger.getLogger("WeatherBot").severe("Could not access configuration, or configuration is malformed.")
            exitProcess(-1)
        }
    }
}